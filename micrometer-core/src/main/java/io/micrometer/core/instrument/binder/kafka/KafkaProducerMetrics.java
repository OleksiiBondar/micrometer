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
    private static final String OBJECT_NAME_PATTERN = JMX_DOMAIN + ":type=producer-metrics,client-id=*";

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
