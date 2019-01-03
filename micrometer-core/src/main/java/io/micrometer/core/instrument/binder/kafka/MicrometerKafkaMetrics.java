/**
 * Copyright 2019 Pivotal Software, Inc.
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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oleksii Bondar
 */
@NonNullApi
@NonNullFields
public class MicrometerKafkaMetrics extends JmxReporter  implements MeterBinder {
    
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private Iterable<Tag> commonTags;
    private MeterRegistry registry;
    
    public MicrometerKafkaMetrics(Iterable<Tag> tags) {
        this.commonTags = tags;
    }
    
    public MicrometerKafkaMetrics(MeterRegistry registry, Iterable<Tag> tags) {
        this.registry = registry;
        this.commonTags = tags;
    }
    
    public void configure(Map<String, ?> configs) {
        super.configure(configs);
        logger.info("configure executed");
    }
    
    public void metricRemoval(KafkaMetric metric) {
        super.metricRemoval(metric);
    }

    public void close() {
        super.close();
        logger.info("close executed");
    }
    
    public void init(List<KafkaMetric> metrics) {
        super.init(metrics);
        logger.info("init executed");
    }
    
    public void bindTo(MeterRegistry registry) {
        logger.info("bindTo method invoked with {}", registry);
        if (registry == null) {
            this.registry = registry;
        }
    }

    public void metricChange(KafkaMetric metric) {
        super.metricChange(metric);
        logger.info("metricChange method invoked");
        String metricName = metric.metricName().name();
        Map<String, String> metricTags = metric.metricName().tags();
        logger.debug("Registering metric: [{}], [{}], [{}]", metricName, metricTags, metric.metricName().description());
        if (!(metric.metricValue() instanceof Double)) {
            logger.debug("Non-double metric: [{}] -> [{}]", metricName, metric.metricValue().getClass());
            return;
        }
        if (registry == null) {
            logger.error("MeterRegistry is not initialized");
            return;
        }

        Collection<Tag> tags = metricTags.entrySet().stream().map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
//        registry.gauge(metricName, tags, metric, m -> (Double) m.metricValue());
        
        Gauge.builder(metricName, metric, m -> (Double) m.metricValue())
        .tags(tags)
        .baseUnit("na")
        .register(registry);
    }

}
