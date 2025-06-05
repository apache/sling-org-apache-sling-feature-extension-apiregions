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
package org.apache.sling.feature.extension.apiregions.api.config;

import java.io.IOException;

import jakarta.json.Json;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DescribableEntityTest {

    public static class DE extends DescribableEntity {
        // DescribableEntity is abstract, therefore subclassing for testing
    }

    @Test
    public void testClear() {
        final DE entity = new DE();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setDeprecated("d");
        entity.setTitle("t");
        entity.setDescription("x");
        entity.setEnforceOn("e");
        entity.setSince("s");
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertNull(entity.getDeprecated());
        assertNull(entity.getTitle());
        assertNull(entity.getDescription());
        assertNull(entity.getEnforceOn());
        assertNull(entity.getSince());
    }

    @Test
    public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 1, \"b\" : \"2\", \"title\" : \"t\", \"description\" : \"desc\", \"deprecated\" : "
                + "\"depr\", \"enforce-on\" : \"1970-04-01\", \"since\" : \"1970-01-01\"}");

        final DE entity = new DE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getAttributes().size());
        assertEquals(Json.createValue(1), entity.getAttributes().get("a"));
        assertEquals(Json.createValue("2"), entity.getAttributes().get("b"));
        assertEquals("t", entity.getTitle());
        assertEquals("desc", entity.getDescription());
        assertEquals("1970-04-01", entity.getEnforceOn());
        assertEquals("1970-01-01", entity.getSince());
    }

    @Test
    public void testToJSONObject() throws IOException {
        final DE entity = new DE();
        entity.getAttributes().put("a", Json.createValue(1));
        entity.getAttributes().put("b", Json.createValue("2"));
        entity.setTitle("t");
        entity.setDescription("desc");
        entity.setDeprecated("depr");
        entity.setEnforceOn("1970-04-01");
        entity.setSince("1970-01-01");

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 1, \"b\" : \"2\", \"title\" : \"t\", \"description\" : \"desc\", \"deprecated\" : "
                + "\"depr\", \"enforce-on\" : \"1970-04-01\", \"since\" : \"1970-01-01\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }
}
