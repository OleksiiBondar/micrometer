/**
 * Copyright 2018 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import io.micrometer.core.lang.Nullable;

import javax.management.MBeanServer;

import static java.util.Collections.emptyList;

/**
 * Kafka producer metrics collected from metrics exposed by Kafka producers via the MBeanServer.
 * <p>
 * Metric names here are based on the naming scheme as it was last changed in Kafka version 0.11.0. Metrics for earlier
 * versions of Kafka will not report correctly.
 *
 * @author Oleksii Bondar
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html#producer-metrics">Kakfa monitoring
 *      documentation</a>
 */
@NonNullApi
@NonNullFields
public class KafkaProducerMetrics extends AbstractKafkaMetrics {

    private static final String JMX_DOMAIN = "kafka.producer";
    private static final String METRIC_NAME_PREFIX = "kafka.producer.";

    @Nullable
    private Integer kafkaMajorVersion;

    public KafkaProducerMetrics() {
        this(emptyList());
    }

    public KafkaProducerMetrics(Iterable<Tag> tags) {
        super(tags);
    }

    public KafkaProducerMetrics(MBeanServer mBeanServer, Iterable<Tag> tags) {
        super(mBeanServer, tags);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerMetricsEventually("producer-metrics", (o, tags) -> {
            registerTimeGaugeForObject(registry, o, "request-latency-avg", tags, "The average request latency in ms.");
            registerTimeGaugeForObject(registry, o, "request-latency-max", tags, "The maximum request latency in ms.");
            
            registerGaugeForObject(registry, o, "request-rate", tags, "The average number of requests sent per second.", "records");
            registerGaugeForObject(registry, o, "response-rate", tags, "The average number of records in each request.", "records");
            registerGaugeForObject(registry, o, "incoming-byte-rate", tags, "The average number of incoming bytes received per second from all servers.", "bytes");
            registerGaugeForObject(registry, o, "outgoing-byte-rate", tags, "The average number of outgoing bytes sent per second to all servers.", "bytes");
            
            registerGaugeForObject(registry, o, "connection-count", tags, "The current number of active connections.", "connections");
            registerGaugeForObject(registry, o, "connection-creation-rate", tags, "New connections established per second in the window.", "connections");
            registerGaugeForObject(registry, o, "connection-close-rate", tags, "Connections closed per second in the window.", "connections");
            registerGaugeForObject(registry, o, "io-ratio", tags, "The fraction of time the I/O thread spent doing I/O.", null);
            registerGaugeForObject(registry, o, "io-time-ns-avg", tags, "The average length of time for I/O per select call in nanoseconds.", "ns");
            registerGaugeForObject(registry, o, "io-wait-ratio", tags, "The fraction of time the I/O thread spent waiting.", null);
            registerGaugeForObject(registry, o, "select-rate", tags, "Number of times the I/O layer checked for new I/O to perform per second.", null);
            registerGaugeForObject(registry, o, "io-wait-time-ns-avg", tags, "The average length of time the I/O thread spent waiting for a socket ready for reads or writes in nanoseconds.", "ns");
        });
        
        registerMetricsEventually("producer-topic-metrics", (o, tags) -> {
            registerGaugeForObject(registry, o, "byte-rate", tags, "The average request latency in ms.", "bytes");
            registerGaugeForObject(registry, o, "record-send-rate", tags, "The average number of records sent per second for a topic.", "records");
            registerGaugeForObject(registry, o, "compression-rate", tags, "The average compression rate of record batches for a topic.", null);
            registerGaugeForObject(registry, o, "record-retry-rate", tags, "The average per-second number of retried record sends for a topic.", null);
            registerGaugeForObject(registry, o, "record-error-rate", tags, "The average per-second number of record sends that resulted in errors for a topic.", "records");
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

}
