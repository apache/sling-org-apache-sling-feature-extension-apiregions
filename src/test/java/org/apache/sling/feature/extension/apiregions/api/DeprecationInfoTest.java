/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.extension.apiregions.api;

import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeprecationInfoTest {

    @Test(expected = IllegalArgumentException.class)
    public void testMessageRequired() throws Exception {
        new DeprecationInfo("Message");
        new DeprecationInfo(null);
    }

    @Test
    public void testForRemovalNull() throws Exception {
        final DeprecationInfo info = new DeprecationInfo("Message");
        assertNull(info.getForRemoval());
        assertFalse(info.isForRemoval());
        assertNull(info.getForRemovalBy());
    }

    @Test
    public void testForRemovalTrue() throws Exception {
        final DeprecationInfo info = new DeprecationInfo("Message");
        info.setForRemoval("true");
        assertEquals("true", info.getForRemoval());
        assertTrue(info.isForRemoval());
        assertNull(info.getForRemovalBy());
    }

    @Test
    public void testForRemovalFalse() throws Exception {
        final DeprecationInfo info = new DeprecationInfo("Message");
        info.setForRemoval("false");
        assertEquals("false", info.getForRemoval());
        assertFalse(info.isForRemoval());
        assertNull(info.getForRemovalBy());
    }

    @Test
    public void testForRemovalDate() throws Exception {
        final DeprecationInfo info = new DeprecationInfo("Message");
        info.setForRemoval("2021-02-05");
        assertEquals("2021-02-05", info.getForRemoval());
        assertTrue(info.isForRemoval());
        final Calendar c = info.getForRemovalBy();
        assertNotNull(c);
        assertEquals(2021, c.get(Calendar.YEAR));
        assertEquals(1, c.get(Calendar.MONTH));
        assertEquals(5, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testForRemovalString() throws Exception {
        final DeprecationInfo info = new DeprecationInfo("Message");
        info.setForRemoval("hello");
        assertEquals("hello", info.getForRemoval());
        assertTrue(info.isForRemoval());
        assertNull(info.getForRemovalBy());
    }
}
