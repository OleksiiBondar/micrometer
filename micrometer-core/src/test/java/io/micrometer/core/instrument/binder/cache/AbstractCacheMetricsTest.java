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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Oleksii Bondar
 */
abstract class AbstractCacheMetricsTest {

    protected abstract Tags getTags();

    /**
     * Verifies base metrics presense
     */
    protected void verifyCommonCacheMetrics(MeterRegistry meterRegistry) {
        meterRegistry.get("cache.size").tags(getTags()).gauge();
        meterRegistry.get("cache.gets").tags(getTags()).tag("result", "hit").functionCounter();
        meterRegistry.get("cache.gets").tags(getTags()).tag("result", "miss").functionCounter();
        meterRegistry.get("cache.puts").tags(getTags()).functionCounter();
        meterRegistry.get("cache.evictions").tags(getTags()).functionCounter();
    }

    protected RequiredSearch fetch(SimpleMeterRegistry meterRegistry, String name) {
        return meterRegistry.get(name).tags(getTags());
    }

    protected RequiredSearch fetch(SimpleMeterRegistry meterRegistry, String name, Iterable<Tag> tags) {
        return fetch(meterRegistry, name).tags(tags);
    }
}
