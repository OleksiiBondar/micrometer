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
package io.micrometer.core.instrument.binder.cache;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import java.net.URI;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CaffeineCacheMetrics}.
 *
 * @author Oleksii Bondar
 */
class JCacheMetricsTest extends AbstractCacheMetricsTest {

    @Mock
    private Cache<String, String> cache;

    @Mock
    private CacheManager cacheManager;

    private Tags expectedTag = Tags.of("app", "test");
    private JCacheMetrics metrics;
    private MBeanServer mbeanServer;
    private Long expectedAttributeValue = new Random().nextLong();

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(cache.getCacheManager()).thenReturn(cacheManager);
        when(cache.getName()).thenReturn("testCache");
        when(cacheManager.getURI()).thenReturn(new URI("http://localhost"));
        metrics = new JCacheMetrics(cache, expectedTag);

        // emulate MBean server with MBean used for statistic lookup
        mbeanServer = MBeanServerFactory.createMBeanServer();
        ObjectName objectName = new ObjectName("javax.cache:type=CacheStatistics");
        ReflectionTestUtils.setField(metrics, "objectName", objectName);
        CacheMBeanStub mBean = new CacheMBeanStub(expectedAttributeValue);
        mbeanServer.registerMBean(mBean, objectName);
    }

    @AfterEach
    void tearDown() {
        if (mbeanServer != null) {
            MBeanServerFactory.releaseMBeanServer(mbeanServer);
        }
    }

    @Test
    void reportExpectedMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        metrics.bindTo(meterRegistry);

        Gauge cacheRemovals = fetch(meterRegistry, "cache.removals").gauge();
        assertThat(cacheRemovals.value()).isEqualTo(expectedAttributeValue.doubleValue());
    }
    
    @Test
    void constructInstanceViaStaticMethodMonitor() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        JCacheMetrics.monitor(meterRegistry, cache, expectedTag);

        meterRegistry.get("cache.removals").tags(expectedTag).gauge();
    }

    @Test
    void returnCacheSize() {
        assertThat(metrics.size()).isNull();
    }

    @Test
    void returnNullForMissCount() {
        assertThat(metrics.missCount()).isEqualTo(expectedAttributeValue);
    }

    @Test
    void returnNullForEvictionCount() {
        assertThat(metrics.evictionCount()).isEqualTo(expectedAttributeValue);
    }

    @Test
    void returnHitCount() {
        assertThat(metrics.hitCount()).isEqualTo(expectedAttributeValue);
    }

    @Test
    void returnPutCount() {
        assertThat(metrics.putCount()).isEqualTo(expectedAttributeValue);
    }
    
    @Test
    void defaultValueWhenNoMBeanAttributeFound() throws MalformedObjectNameException {
        // change source MBean to emulate AttributeNotFoundException
        ObjectName objectName = new ObjectName("javax.cache:type=CacheInformation");
        ReflectionTestUtils.setField(metrics, "objectName", objectName);

        assertThat(metrics.hitCount()).isEqualTo(0L);
    }
    
    private static class CacheMBeanStub implements DynamicMBean {

        private Long expectedAttributeValue;

        public CacheMBeanStub(Long attributeValue) {
            this.expectedAttributeValue = attributeValue;
        }

        @Override
        public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
            return expectedAttributeValue;
        }

        @Override
        public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            return null;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return null;
        }

        @Override
        public Object invoke(String actionName,
                             Object[] params,
                             String[] signature) throws MBeanException, ReflectionException {
            return null;
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            return new MBeanInfo(CacheMBeanStub.class.getName(), "description", null, null, null, null);
        }

    }
    
    @Override
    protected Tags getTags() {
        return expectedTag;
    }

}
