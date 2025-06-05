/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions.api.artifacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import jakarta.json.Json;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class VersionRuleTest {

    @Test public void testClear() {
        final VersionRule entity = new VersionRule();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setAllowedVersionRanges(new VersionRange[] {new VersionRange("1.0")});
        entity.setDeniedVersionRanges(new VersionRange[] {new VersionRange("3.0")});
        entity.setMode(Mode.LENIENT);
        entity.setMessage("msg");
        entity.setArtifactId(ArtifactId.parse("g:a:1"));
        entity.setEnforceOn("2024-01-01");
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertNull(entity.getAllowedVersionRanges());
        assertNull(entity.getDeniedVersionRanges());
        assertNull(entity.getMessage());
        assertNull(entity.getArtifactId());
        assertNull(entity.getMode());
        assertNull(entity.getEnforceOn());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"message\" : \"msg\", \"artifact-id\":\"g:a:1\","
            + "\"allowed-version-ranges\":[\"1.0\"],\"denied-version-ranges\":[\"2.0\"]}");

        final VersionRule entity = new VersionRule();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(Mode.LENIENT, entity.getMode());
        assertEquals("msg", entity.getMessage());
        assertEquals(ArtifactId.parse("g:a:1"), entity.getArtifactId());
        assertEquals(1, entity.getAllowedVersionRanges().length);
        assertEquals(new VersionRange("1.0"), entity.getAllowedVersionRanges()[0]);
        assertEquals(1, entity.getDeniedVersionRanges().length);
        assertEquals(new VersionRange("2.0"), entity.getDeniedVersionRanges()[0]);
        assertNull(entity.getEnforceOn());
    }

    @Test public void testToJSONObject() throws IOException {
        final VersionRule entity = new VersionRule();
        entity.setMode(Mode.LENIENT);
        entity.setMessage("msg");
        entity.setArtifactId(ArtifactId.parse("g:a:1"));
        entity.setAllowedVersionRanges(new VersionRange[] {new VersionRange("1.0.0")});
        entity.setDeniedVersionRanges(new VersionRange[] {new VersionRange("2.0.0")});

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"artifact-id\":\"g:a:1\", \"message\" : \"msg\","
            + "\"allowed-version-ranges\":[\"1.0.0\"],\"denied-version-ranges\":[\"2.0.0\"]}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }

    @Test public void testFromJSONObjectWithEnforceOn() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"message\" : \"msg\", \"artifact-id\":\"g:a:1\","
            + "\"allowed-version-ranges\":[\"1.0\"],\"denied-version-ranges\":[\"2.0\"],\"enforce-on\":\"2024-02-02\"}");

        final VersionRule entity = new VersionRule();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(Mode.LENIENT, entity.getMode());
        assertEquals("msg", entity.getMessage());
        assertEquals(ArtifactId.parse("g:a:1"), entity.getArtifactId());
        assertEquals(1, entity.getAllowedVersionRanges().length);
        assertEquals(new VersionRange("1.0"), entity.getAllowedVersionRanges()[0]);
        assertEquals(1, entity.getDeniedVersionRanges().length);
        assertEquals(new VersionRange("2.0"), entity.getDeniedVersionRanges()[0]);
        assertEquals("2024-02-02", entity.getEnforceOn());
    }

    @Test public void testToJSONObjectWithEnforceOn() throws IOException {
        final VersionRule entity = new VersionRule();
        entity.setMode(Mode.LENIENT);
        entity.setMessage("msg");
        entity.setArtifactId(ArtifactId.parse("g:a:1"));
        entity.setAllowedVersionRanges(new VersionRange[] {new VersionRange("1.0.0")});
        entity.setDeniedVersionRanges(new VersionRange[] {new VersionRange("2.0.0")});
        entity.setEnforceOn("2024-02-02");

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"artifact-id\":\"g:a:1\", \"message\" : \"msg\","
            + "\"allowed-version-ranges\":[\"1.0.0\"],\"denied-version-ranges\":[\"2.0.0\"],\"enforce-on\":\"2024-02-02\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }

    @Test public void testIsAllowedNoRanges() {
        final VersionRule entity = new VersionRule();
        assertFalse(entity.isAllowed(new Version("1.0")));
        assertFalse(entity.isAllowed(new Version("1.3")));
        assertFalse(entity.isAllowed(new Version("2.1")));
    }

    @Test public void testIsAllowedAllowedRange() {
        final VersionRule entity = new VersionRule();
        entity.setAllowedVersionRanges(new VersionRange[] {new VersionRange("[1.2, 2)")});
        assertFalse(entity.isAllowed(new Version("1.0")));
        assertTrue(entity.isAllowed(new Version("1.3")));
        assertFalse(entity.isAllowed(new Version("2.1")));
    }

    @Test public void testIsAllowedAllowedDenied() {
        final VersionRule entity = new VersionRule();
        entity.setAllowedVersionRanges(new VersionRange[] {new VersionRange("[1.2, 2)")});
        entity.setDeniedVersionRanges(new VersionRange[] {new VersionRange("[1.3.1,1.3.1]")});
        assertFalse(entity.isAllowed(new Version("1.0")));
        assertTrue(entity.isAllowed(new Version("1.3")));
        assertFalse(entity.isAllowed(new Version("1.3.1")));
        assertTrue(entity.isAllowed(new Version("1.3.2")));
        assertFalse(entity.isAllowed(new Version("2.1")));
    }

    @Test public void testSetEnforceOn() {
        final VersionRule entity = new VersionRule();
        entity.setEnforceOn("2024-01-01");
        assertEquals("2024-01-01", entity.getEnforceOn());
        final Calendar c = entity.getEnforceOnDate();
        assertEquals(2024, c.get(Calendar.YEAR));
        assertEquals(0, c.get(Calendar.MONTH));
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = IllegalArgumentException.class) public void testSetEnforceOnInvalid() {
        final VersionRule entity = new VersionRule();
        entity.setEnforceOn("invalid-date");
    }
}
