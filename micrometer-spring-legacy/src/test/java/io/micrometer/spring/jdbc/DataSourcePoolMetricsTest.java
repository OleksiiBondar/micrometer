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
package io.micrometer.spring.jdbc;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;

import static java.util.Collections.emptyList;

/**
 * @author Jon Schneider
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.generate-unique-name=true",
    "management.security.enabled=false",
    "spring.datasource.type=org.apache.tomcat.jdbc.pool.DataSource"
})
class DataSourcePoolMetricsTest {
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeterRegistry registry;

    @Test
    void dataSourceIsInstrumented() throws SQLException {
        dataSource.getConnection().getMetaData();
        registry.find("data.source.connections.max").meter();
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    @Import(DataSourceConfig.class)
    static class MetricsApp {
        @Bean
        MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    static class DataSourceConfig {
        public DataSourceConfig(DataSource dataSource,
                                Collection<DataSourcePoolMetadataProvider> metadataProviders,
                                MeterRegistry registry) {
            new DataSourcePoolMetrics(
                dataSource,
                metadataProviders,
                "data.source",
                emptyList()).bindTo(registry);
        }
    }
}
