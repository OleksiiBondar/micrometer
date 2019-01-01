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
package io.micrometer.spring;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.atomic.AtomicReference;

import static io.micrometer.core.aop.TimedAspect.EXCEPTION_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TimedAspectTest.TestAspectConfig.class)
class TimedAspectTest {
    
    @Autowired
    private TimedService service;

    @Autowired
    private MeterRegistry registry;

    @Test
    void serviceIsTimed() {
        service.timeMe();
        assertThat(registry.get("something").timer().count()).isEqualTo(1);
    }

    @Test
    void serviceIsTimedWhenNoValue() {
        service.timeWithoutValue();
        assertThat(registry.get(TimedAspect.DEFAULT_METRIC_NAME).timer().count()).isEqualTo(1);
    }

    @Test
    void serviceIsTimedWhenThereIsAnException() {
        assertThrows(RuntimeException.class, () -> service.timeWithException());
        assertThat(registry.get("somethingElse").tags(EXCEPTION_TAG, "RuntimeException").timer().count()).isEqualTo(1);
    }

    @Test
    void serviceIsTimedWhenThereIsNoException() {
        service.timeWithoutException();
        assertThat(registry.get("somethingElse").tags(EXCEPTION_TAG, "none").timer().count()).isEqualTo(1);
    }

    @Test
    void serviceIsTimedWithHistogram() {
        // given...
        // ... we are waiting for a metric to be created with a histogram
        AtomicReference<DistributionStatisticConfig> myConfig = new AtomicReference<>();
        registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("something")) {
                    myConfig.set(config);
                }
                return config;
            }
        });

        // when...
        // ... the service is being called
        service.timeWithHistogram();

        // then...
        assertThat(myConfig.get()).as("the metric has been created").isNotNull();
        assertThat(myConfig.get().isPublishingHistogram()).as("the metric has a histogram").isTrue();
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import(TimedService.class)
    static class TestAspectConfig {
        @Bean
        public SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public TimedAspect micrometerAspect(MeterRegistry meterRegistry) {
            return new TimedAspect(meterRegistry);
        }
    }

    @Service
    static class TimedService {
        @Timed("something")
        public String timeMe() {
            return "hello world";
        }

        @Timed
        public String timeWithoutValue() {
            return "hello universe";
        }

        @Timed("somethingElse")
        public String timeWithException() {
            throw new RuntimeException("universe destroyed.");
        }

        @Timed("somethingElse")
        public String timeWithoutException() {
            return "hello world";
        }

        @Timed(value = "something", histogram = true)
        public String timeWithHistogram() {
            return "hello histogram";
        }
    }
}
