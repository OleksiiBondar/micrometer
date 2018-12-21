/**
 * Copyright 2018 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/**
 * @author Oleksii Bondar
 */
public abstract class AbstractKafkaMetrics implements MeterBinder {
    
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    @Nullable
    private Integer kafkaMajorVersion;
    
    private final MBeanServer mBeanServer;

    protected final Iterable<Tag> tags;
    
    public abstract String getJmxDomain();
    public abstract String getMetricNamePrefix();
    
    public AbstractKafkaMetrics(Iterable<Tag> tags) {
        this(getMBeanServer(), tags);
    }
    
    public AbstractKafkaMetrics(MBeanServer mBeanServer, Iterable<Tag> tags) {
        this.mBeanServer = mBeanServer;
        this.tags = tags;
    }
    
    protected int kafkaMajorVersion(Tags tags) {
        if (kafkaMajorVersion == null) {
            kafkaMajorVersion = tags.stream().filter(t -> "client.id".equals(t.getKey())).findAny()
                    .map(clientId -> {
                        try {
                            String version = (String) mBeanServer.getAttribute(new ObjectName(getJmxDomain() + ":type=app-info,client-id=" + clientId.getValue()), "version");
                            return Integer.parseInt(version.substring(0, version.indexOf('.')));
                        } catch (Throwable e) {
                            logger.error("failed to fetch kafka major version", e);
                            return -1; // should never happen
                        }
                    })
                    .orElse(-1);
        }
        return kafkaMajorVersion;
    } 
    
    protected void registerGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, String meterName, Tags allTags, String description, @Nullable String baseUnit) {
        final AtomicReference<Gauge> gauge = new AtomicReference<>();
        gauge.set(Gauge
                .builder(getMetricNamePrefix() + meterName, mBeanServer,
                        getJmxAttribute(registry, gauge, o, jmxMetricName))
                .description(description)
                .baseUnit(baseUnit)
                .tags(allTags)
                .register(registry));
    }

    protected void registerGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, Tags allTags, String description, @Nullable String baseUnit) {
        registerGaugeForObject(registry, o, jmxMetricName, sanitize(jmxMetricName), allTags, description, baseUnit);
    }

    protected void registerCounterForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, Tags allTags, String description, @Nullable String baseUnit) {
        final AtomicReference<FunctionCounter> counter = new AtomicReference<>();
        counter.set(FunctionCounter
                .builder(getMetricNamePrefix() + sanitize(jmxMetricName), mBeanServer,
                        getJmxAttribute(registry, counter, o, jmxMetricName))
                .description(description)
                .baseUnit(baseUnit)
                .tags(allTags)
                .register(registry));
    }

    protected void registerTimeGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, String meterName, Tags allTags, String description) {
        final AtomicReference<TimeGauge> timeGauge = new AtomicReference<>();
        timeGauge.set(TimeGauge.builder(getMetricNamePrefix() + meterName, mBeanServer, TimeUnit.MILLISECONDS,
                getJmxAttribute(registry, timeGauge, o, jmxMetricName))
                .description(description)
                .tags(allTags)
                .register(registry));
    }

    private ToDoubleFunction<MBeanServer> getJmxAttribute(MeterRegistry registry, AtomicReference<? extends Meter> meter,
                                                          ObjectName o, String jmxMetricName) {
        return s -> safeDouble(jmxMetricName, () -> {
            if (!s.isRegistered(o)) {
                registry.remove(meter.get());
            }
            return s.getAttribute(o, jmxMetricName);
        });
    }

    protected void registerTimeGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, Tags allTags, String description) {
        registerTimeGaugeForObject(registry, o, jmxMetricName, sanitize(jmxMetricName), allTags, description);
    }

    protected void registerMetricsEventually(String type, BiConsumer<ObjectName, Tags> perObject) {
        try {
            Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName(getJmxDomain() + ":type=" + type + ",*"), null);
            if (!objs.isEmpty()) {
                for (ObjectName o : objs) {
                    perObject.accept(o, Tags.concat(tags, nameTag(o)));
                }
                return;
            }
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Error registering Kafka JMX based metrics", e);
        }

        registerNotificationListener(type, perObject);
    }

    /**
     * This notification listener should remain indefinitely since new Kafka consumers can be added at any time.
     *
     * @param type      The Kafka JMX type to listen for.
     * @param perObject Metric registration handler when a new MBean is created.
     */
    protected void registerNotificationListener(String type, BiConsumer<ObjectName, Tags> perObject) {
        NotificationListener notificationListener = (notification, handback) -> {
            MBeanServerNotification mbs = (MBeanServerNotification) notification;
            ObjectName o = mbs.getMBeanName();
            perObject.accept(o, Tags.concat(tags, nameTag(o)));
        };

        NotificationFilter filter = (NotificationFilter) notification -> {
            if (!MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType()))
                return false;
            ObjectName obj = ((MBeanServerNotification) notification).getMBeanName();
            return obj.getDomain().equals(getJmxDomain()) && obj.getKeyProperty("type").equals(type) &&
                    obj.getKeyProperty("partition") == null && obj.getKeyProperty("topic") == null;
        };

        try {
            mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("Error registering Kafka MBean listener", e);
        }
    }
    
    private static MBeanServer getMBeanServer() {
        List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (!mBeanServers.isEmpty()) {
            return mBeanServers.get(0);
        }
        return ManagementFactory.getPlatformMBeanServer();
    }

    private double safeDouble(String jmxMetricName, Callable<Object> callable) {
        try {
            return Double.parseDouble(callable.call().toString());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private Iterable<Tag> nameTag(ObjectName name) {
        Tags tags = Tags.empty();

        String clientId = name.getKeyProperty("client-id");
        if (clientId != null) {
            tags = Tags.concat(tags, "client.id", clientId);
        }

        return tags;
    }

    private static String sanitize(String value) {
        return value.replaceAll("-", ".");
    }
}
