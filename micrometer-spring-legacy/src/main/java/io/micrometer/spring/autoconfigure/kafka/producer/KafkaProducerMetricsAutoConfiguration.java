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
package io.micrometer.spring.autoconfigure.kafka.producer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaProducerMetrics;
import io.micrometer.spring.autoconfigure.MetricsAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.management.MBeanServer;

import java.util.Collections;

/**
 * Configuration for {@link KafkaProducerMetrics}.
 *
 * @author Oleksii Bondar
 */
@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, JmxAutoConfiguration.class})
@ConditionalOnClass(KafkaProducerMetrics.class)
@ConditionalOnBean(MeterRegistry.class)
public class KafkaProducerMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MBeanServer.class)
    @ConditionalOnProperty(value = "management.metrics.kafka.producer.enabled", matchIfMissing = true)
    public KafkaProducerMetrics kafkaProducerMetrics(MBeanServer mbeanServer) {
        return new KafkaProducerMetrics(mbeanServer, Collections.emptyList());
    }

}
