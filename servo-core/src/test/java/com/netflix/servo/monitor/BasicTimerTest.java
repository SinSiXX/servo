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

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class BasicTimerTest extends AbstractMonitorTest<BasicTimer> {

    public BasicTimer newInstance(String name) {
        return new BasicTimer(MonitorConfig.builder(name).build());
    }

    @Test
    public void testGetValue() throws Exception {
        BasicTimer c = newInstance("foo");
        assertEquals(c.getValue().longValue(), 0L);
        assertEquals(c.getTotalTime().longValue(), 0L);
        assertEquals(c.getCount().longValue(), 0L);
        assertEquals(c.getMin().longValue(), 0L);
        assertEquals(c.getMax().longValue(), 0L);

        c.record(42, TimeUnit.MILLISECONDS);
        assertEquals(c.getValue().longValue(), 42L);
        assertEquals(c.getTotalTime().longValue(), 42L);
        assertEquals(c.getCount().longValue(), 1L);
        assertEquals(c.getMin().longValue(), 42L);
        assertEquals(c.getMax().longValue(), 42L);

        c.record(21, TimeUnit.MILLISECONDS);
        assertEquals(c.getValue().longValue(), 31L);
        assertEquals(c.getTotalTime().longValue(), 63L);
        assertEquals(c.getCount().longValue(), 2L);
        assertEquals(c.getMin().longValue(), 21L);
        assertEquals(c.getMax().longValue(), 42L);

        c.record(84, TimeUnit.MILLISECONDS);
        assertEquals(c.getValue().longValue(), 49L);
        assertEquals(c.getTotalTime().longValue(), 147L);
        assertEquals(c.getCount().longValue(), 3L);
        assertEquals(c.getMin().longValue(), 21L);
        assertEquals(c.getMax().longValue(), 84L);

        for (Monitor<?> m : c.getMonitors()) {
            if (m instanceof ResettableMonitor<?>) {
                double v = (Double) ((ResettableMonitor<?>) m).getAndResetValue();
                assertNotEquals(v, 0.0);
            }
        }

        assertEquals(c.getValue().longValue(), 49L);
        assertEquals(c.getTotalTime().longValue(), 147L);
        assertEquals(c.getCount().longValue(), 3L);
        assertEquals(c.getMin().longValue(), 0L);
        assertEquals(c.getMax().longValue(), 0L);
    }

    @Test
    public void testEqualsCount() throws Exception {
        BasicTimer c1 = newInstance("foo");
        BasicTimer c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.record(42, TimeUnit.MILLISECONDS);
        assertNotEquals(c1, c2);
        c2.record(42, TimeUnit.MILLISECONDS);
        assertEquals(c1, c2);
    }

    @Test
    public void testFractional() throws Exception {
        BasicTimer timer = newInstance("foo");

        timer.record(1000, TimeUnit.NANOSECONDS);
        assertEquals(timer.getTotalTime(), 0.001);
        assertEquals(timer.getMax(), 0.001);
        assertEquals(timer.getMin(), 0.001);
    }
}
