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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.json.Json;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

public class ConfigurableEntityTest {

    public static class CE extends ConfigurableEntity {
        // ConfigurableEntity is abstract, therefore subclassing for testing

        public CE() {
            setDefaults();
        }
    }

    @Test public void testClear() {
        final CE entity = new CE();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setDeprecated("d");
        entity.setTitle("t");
        entity.setDescription("x");
        entity.getPropertyDescriptions().put("a", new PropertyDescription());
        entity.setMode(Mode.SILENT);
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertNull(entity.getDeprecated());
        assertNull(entity.getTitle());
        assertNull(entity.getDescription());
        assertTrue(entity.getPropertyDescriptions().isEmpty());
        assertNull(entity.getMode());
        assertTrue(entity.getInternalPropertyNames().isEmpty());
        assertFalse(entity.isAllowAdditionalProperties());
        assertEquals(Region.GLOBAL, entity.getRegion());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {}, \"b\" : {}}}");

        final CE entity = new CE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getPropertyDescriptions().size());
        assertNotNull(entity.getPropertyDescriptions().get("a"));
        assertNotNull(entity.getPropertyDescriptions().get("b"));
    }

    @Test public void testToJSONObject() throws IOException {
        final CE entity = new CE();
        entity.getPropertyDescriptions().put("a", new PropertyDescription());
        entity.getPropertyDescriptions().put("b", new PropertyDescription());

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {}, \"b\" : {}}}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }

    @Test(expected = IOException.class) 
    public void testDuplicateCaseInsensitiveKeys() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {}, \"A\" : {}}}");

        final CE entity = new CE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
    }

    @Test public void testSerialisingMode() throws IOException {
        final CE entity = new CE();
        entity.setMode(Mode.SILENT);

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"SILENT\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
        entity.clear();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(Mode.SILENT, entity.getMode());
    }

    @Test public void testSerialisingRegion() throws IOException {
        final CE entity = new CE();
        entity.setRegion(Region.INTERNAL);

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"region\" : \"INTERNAL\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
        entity.clear();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(Region.INTERNAL, entity.getRegion());
    }

    @Test public void testSerialisingAllowAdditionalProperties() throws IOException {
        final CE entity = new CE();
        entity.setAllowAdditionalProperties(true);

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"allow-additional-properties\" : true}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
        entity.clear();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertTrue(entity.isAllowAdditionalProperties());
    }

    @Test public void testSerialisingInternalProperties() throws IOException {
        final CE entity = new CE();
        entity.getInternalPropertyNames().add("a");
        entity.getInternalPropertyNames().add("b");

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"internal-property-names\" : [\"a\",\"b\"]}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
        entity.clear();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getInternalPropertyNames().size());
        assertTrue(entity.getInternalPropertyNames().contains("a"));
        assertTrue(entity.getInternalPropertyNames().contains("b"));
    }
}