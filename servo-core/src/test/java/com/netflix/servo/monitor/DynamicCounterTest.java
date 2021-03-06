/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.google.common.collect.ImmutableList;
import com.netflix.servo.jsr166e.ConcurrentHashMapV8;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.ExpiringCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class DynamicCounterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounterTest.class);
    private DynamicCounter getInstance() throws Exception  {
        Field theInstance = DynamicCounter.class.getDeclaredField("INSTANCE");
        theInstance.setAccessible(true);
        return (DynamicCounter) theInstance.get(null);
    }

    private List<Monitor<?>> getCounters() throws Exception {
        return getInstance().getMonitors();
    }

    private final TagList tagList = new BasicTagList(ImmutableList.of(
        (Tag) new BasicTag("PLATFORM", "true")));

    private ResettableCounter getByName(String name) throws Exception {
        List<Monitor<?>> counters = getCounters();
        for (Monitor<?> m : counters) {
            String monitorName = m.getConfig().getName();
            if (name.equals(monitorName)) {
                return (ResettableCounter) m;
            }
        }
        return null;
    }

    @Test
    public void testHasCounterTag() throws Exception {
        DynamicCounter.increment("test1", tagList);
        ResettableCounter c = getByName("test1");
        Tag type = c.getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "RATE");
    }

    /**
     * Erase all previous counters by creating a new loading cache with a short expiration time
     */
    @BeforeMethod
    public void setupInstance() throws Exception {
        LOGGER.info("Setting up DynamicCounter instance with a new cache");
        DynamicCounter theInstance = getInstance();
        Field counters = DynamicCounter.class.getDeclaredField("counters");
        counters.setAccessible(true);
        ExpiringCache<MonitorConfig, Counter> newShortExpiringCache = new ExpiringCache<MonitorConfig, Counter>(1000L,
                new ConcurrentHashMapV8.Fun<MonitorConfig, Counter>() {
                    @Override
                    public Counter apply(final MonitorConfig config) {
                        return new ResettableCounter(config);
                    }
                }, 100L);

        counters.set(theInstance, newShortExpiringCache);
    }

    @Test
    public void testGetValue() throws Exception {
        DynamicCounter.increment("test1", tagList);
        ResettableCounter c = getByName("test1");
        assertEquals(c.getCount(), 1L);
        c.increment(13);
        assertEquals(c.getCount(), 14L);
    }

    @Test
    public void testExpiration() throws Exception {
        DynamicCounter.increment("test1", tagList);
        DynamicCounter.increment("test2", tagList);

        Thread.sleep(500L);
        DynamicCounter.increment("test1", tagList);

        Thread.sleep(500L);
        DynamicCounter.increment("test1", tagList);

        Thread.sleep(200L);
        ResettableCounter c1 = getByName("test1");
        assertEquals(c1.getCount(), 3L);

        ResettableCounter c2 = getByName("test2");
        assertNull(c2, "Counters not used in a while should expire");
    }

    @Test
    public void testByStrings() throws Exception {
        DynamicCounter.increment("byName");
        DynamicCounter.increment("byName");

        ResettableCounter c = getByName("byName");
        assertEquals(c.getCount(), 2L);

        DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
        DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
        ResettableCounter c2 = getByName("byName2");
        assertEquals(c2.getCount(), 2L);
    }

    @Test
    public void testShouldNotThrow() throws Exception {
        DynamicCounter.increment("name", "", "");
    }
}
