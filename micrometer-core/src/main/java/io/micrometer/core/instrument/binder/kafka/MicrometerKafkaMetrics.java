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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oleksii Bondar
 */
@NonNullApi
@NonNullFields
public class MicrometerKafkaMetrics implements MetricsReporter {
    
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public void configure(Map<String, ?> configs) {
        logger.info("configure executed");
    }
    
    public void metricRemoval(KafkaMetric metric) {
    }

    public void close() {
        logger.info("close executed");
    }
    
    public void init(List<KafkaMetric> metrics) {
        logger.info("init executed");
    }
    
    public void metricChange(KafkaMetric metric) {
        String metricName = metric.metricName().name();
        Map<String, String> metricTags = metric.metricName().tags();
        logger.info("Registering metric: [{}], [{}], [{}]", metricName, metricTags, metric.metricName().description());
        if (!(metric.metricValue() instanceof Double)) {
            logger.info("Non-double metric: [{}] -> [{}]", metricName, metric.metricValue().getClass());
            return;
        }

        Collection<Tag> tags = metricTags.entrySet().stream().map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
        Metrics.gauge("kafka." + metric.metricName().name(), tags, metric, m -> (Double) m.metricValue());
    }

}
