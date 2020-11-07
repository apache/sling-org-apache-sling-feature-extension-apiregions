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
package org.apache.sling.feature.extension.apiregions.api.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.json.Json;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

public class ConfigurableEntityTest {

    public static class CE extends ConfigurableEntity {
        // ConfigurableEntity is abstract, therefore subclassing for testing
    }

    @Test public void testClear() {
        final CE entity = new CE();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setDeprecated("d");
        entity.setTitle("t");
        entity.setDescription("x");
        entity.getProperties().put("a", new Property());
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertNull(entity.getDeprecated());
        assertNull(entity.getTitle());
        assertNull(entity.getDescription());
        assertTrue(entity.getProperties().isEmpty());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {}, \"b\" : {}}}");

        final CE entity = new CE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getProperties().size());
        assertNotNull(entity.getProperties().get("a"));
        assertNotNull(entity.getProperties().get("b"));
    }

    @Test public void testToJSONObject() throws IOException {
        final CE entity = new CE();
        entity.getProperties().put("a", new Property());
        entity.getProperties().put("b", new Property());

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {\"type\":\"STRING\"}, \"b\" : {\"type\":\"STRING\"}}}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }
}