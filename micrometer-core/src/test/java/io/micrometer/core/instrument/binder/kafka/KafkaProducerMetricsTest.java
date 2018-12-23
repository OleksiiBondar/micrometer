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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class KafkaProducerMetricsTest {

    private final static String TOPIC = "my-example-topic";
    private final static String BOOTSTRAP_SERVERS = "localhost:9092";

    private final KafkaProducerMetrics kafkaProducerMetrics = new KafkaProducerMetrics();

    @Test
    void reportExpectedMetrics() {
        try (Producer<Long, String> producer = createProducer()) {

            MeterRegistry registry = new SimpleMeterRegistry();
            kafkaProducerMetrics.bindTo(registry);

            producer.send(new ProducerRecord<Long, String>(TOPIC, "message"));

            // fetch metrics
            registry.get("kafka.producer.request.rate").gauge();
        }

        System.out.println("Completed");
    }

    private Producer<Long, String> createProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        return new KafkaProducer<Long, String>(properties);
    }
}
