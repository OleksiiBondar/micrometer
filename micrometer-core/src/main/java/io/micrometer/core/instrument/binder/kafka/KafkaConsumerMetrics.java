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

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static java.util.Collections.emptyList;

/**
 * Kafka consumer metrics collected from metrics exposed by Kafka consumers via the MBeanServer. Metrics are exposed
 * at each consumer thread.
 * <p>
 * Metric names here are based on the naming scheme as it was last changed in Kafka version 0.11.0. Metrics for earlier
 * versions of Kafka will not report correctly.
 *
 * @author Wardha Perinkadakattu
 * @author Jon Schneider
 * @author Johnny Lim
 * @author Oleksii Bondar
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html#new-consumer-metrics">Kakfa monitoring documentation</a>
 * @since 1.1.0
 */
@Incubating(since = "1.1.0")
@NonNullApi
@NonNullFields
public class KafkaConsumerMetrics extends AbstractKafkaMetrics {
    private static final String JMX_DOMAIN = "kafka.consumer";
    private static final String METRIC_NAME_PREFIX = "kafka.consumer.";

    public KafkaConsumerMetrics() {
        this(emptyList());
    }

    public KafkaConsumerMetrics(Iterable<Tag> tags) {
        super(tags);
    }

    public KafkaConsumerMetrics(MBeanServer mBeanServer, Iterable<Tag> tags) {
        super(mBeanServer, tags);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerMetricsEventually("consumer-fetch-manager-metrics", (o, tags) -> {
            registerGaugeForObject(registry, o, "records-lag-max", tags, "The maximum lag in terms of number of records for any partition in this window. An increasing value over time is your best indication that the consumer group is not keeping up with the producers.", "records");
            registerGaugeForObject(registry, o, "fetch-size-avg", tags, "The average number of bytes fetched per request.", "bytes");
            registerGaugeForObject(registry, o, "fetch-size-max", tags, "The maximum number of bytes fetched per request.", "bytes");
            registerGaugeForObject(registry, o, "records-per-request-avg", tags, "The average number of records in each request.", "records");

            registerCounterForObject(registry, o, "fetch-total", tags, "The number of fetch requests.", "requests");
            registerCounterForObject(registry, o, "bytes-consumed-total", tags, "The total number of bytes consumed.", "bytes");
            registerCounterForObject(registry, o, "records-consumed-total", tags, "The total number of records consumed.", "records");

            if (kafkaMajorVersion(tags) >= 2) {
                // KAFKA-6184
                registerTimeGaugeForObject(registry, o, "records-lead-min", tags, "The lag between the consumer offset and the start offset of the log. If this gets close to zero, it's an indication that the consumer may lose data soon.");
            }

            registerTimeGaugeForObject(registry, o, "fetch-latency-avg", tags, "The average time taken for a fetch request.");
            registerTimeGaugeForObject(registry, o, "fetch-latency-max", tags, "The max time taken for a fetch request.");
            registerTimeGaugeForObject(registry, o, "fetch-throttle-time-avg", tags, "The average throttle time. When quotas are enabled, the broker may delay fetch requests in order to throttle a consumer which has exceeded its limit. This metric indicates how throttling time has been added to fetch requests on average.");
            registerTimeGaugeForObject(registry, o, "fetch-throttle-time-max", tags, "The maximum throttle time.");
        });

        registerMetricsEventually("consumer-coordinator-metrics", (o, tags) -> {
            registerGaugeForObject(registry, o, "assigned-partitions", tags, "The number of partitions currently assigned to this consumer.", "partitions");
            registerGaugeForObject(registry, o, "commit-rate", tags, "The number of commit calls per second.", "commits");
            registerGaugeForObject(registry, o, "join-rate", tags, "The number of group joins per second. Group joining is the first phase of the rebalance protocol. A large value indicates that the consumer group is unstable and will likely be coupled with increased lag.", "joins");
            registerGaugeForObject(registry, o, "sync-rate", tags, "The number of group syncs per second. Group synchronization is the second and last phase of the rebalance protocol. A large value indicates group instability.", "syncs");
            registerGaugeForObject(registry, o, "heartbeat-rate", tags, "The average number of heartbeats per second. After a rebalance, the consumer sends heartbeats to the coordinator to keep itself active in the group. You may see a lower rate than configured if the processing loop is taking more time to handle message batches. Usually this is OK as long as you see no increase in the join rate.", "heartbeats");

            registerTimeGaugeForObject(registry, o, "commit-latency-avg", tags, "The average time taken for a commit request.");
            registerTimeGaugeForObject(registry, o, "commit-latency-max", tags, "The max time taken for a commit request.");
            registerTimeGaugeForObject(registry, o, "join-time-avg", tags, "The average time taken for a group rejoin. This value can get as high as the configured session timeout for the consumer, but should usually be lower.");
            registerTimeGaugeForObject(registry, o, "join-time-max", tags, "The max time taken for a group rejoin. This value should not get much higher than the configured session timeout for the consumer.");
            registerTimeGaugeForObject(registry, o, "sync-time-avg", tags, "The average time taken for a group sync.");
            registerTimeGaugeForObject(registry, o, "sync-time-max", tags, "The max time taken for a group sync.");
            registerTimeGaugeForObject(registry, o, "heartbeat-response-time-max", tags, "The max time taken to receive a response to a heartbeat request.");
            registerTimeGaugeForObject(registry, o, "last-heartbeat-seconds-ago", "last-heartbeat", tags, "The time since the last controller heartbeat.");
        });

        registerMetricsEventually("consumer-metrics", (o, tags) -> {
            registerGaugeForObject(registry, o, "connection-count", tags, "The current number of active connections.", "connections");
            registerGaugeForObject(registry, o, "connection-creation-total", tags, "New connections established.", "connections");
            registerGaugeForObject(registry, o, "connection-close-total", tags, "Connections closed.", "connections");
            registerGaugeForObject(registry, o, "io-ratio", tags, "The fraction of time the I/O thread spent doing I/O.", null);
            registerGaugeForObject(registry, o, "io-wait-ratio", tags, "The fraction of time the I/O thread spent waiting.", null);
            registerGaugeForObject(registry, o, "select-total", tags, "Number of times the I/O layer checked for new I/O to perform.", null);

            registerTimeGaugeForObject(registry, o, "io-time-ns-avg", "io-time-avg", tags, "The average length of time for I/O per select call.");
            registerTimeGaugeForObject(registry, o, "io-wait-time-ns-avg", "io-wait-time-avg", tags, "The average length of time the I/O thread spent waiting for a socket to be ready for reads or writes.");

            if (kafkaMajorVersion(tags) >= 2) {
                registerGaugeForObject(registry, o, "successful-authentication-total", "authentication-attempts",
                        Tags.concat(tags, "result", "successful"), "The number of successful authentication attempts.", null);
                registerGaugeForObject(registry, o, "failed-authentication-total", "authentication-attempts",
                        Tags.concat(tags, "result", "failed"), "The number of failed authentication attempts.", null);

                registerGaugeForObject(registry, o, "network-io-total", tags, "", "bytes");
                registerGaugeForObject(registry, o, "outgoing-byte-total", tags, "", "bytes");
                registerGaugeForObject(registry, o, "request-total", tags, "", "requests");
                registerGaugeForObject(registry, o, "response-total", tags, "", "responses");

                registerTimeGaugeForObject(registry, o, "io-waittime-total", "io-wait-time-total", tags, "Time spent on the I/O thread waiting for a socket to be ready for reads or writes.");
                registerTimeGaugeForObject(registry, o, "iotime-total", "io-time-total", tags, "Time spent in I/O during select calls.");
            }
        });
    }

    @Override
    public String getJmxDomain() {
        return JMX_DOMAIN;
    }

    @Override
    public String getMetricNamePrefix() {
        return METRIC_NAME_PREFIX;
    }
    
    @Override
    protected boolean isSupportedObjectName(ObjectName obj, String type) {
        return super.isSupportedObjectName(obj, type) && obj.getKeyProperty("partition") == null && obj
                .getKeyProperty("topic") == null;
    }

}
