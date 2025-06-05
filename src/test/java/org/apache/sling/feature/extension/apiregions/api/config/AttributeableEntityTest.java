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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AttributeableEntityTest {

    public static class AE extends AttributeableEntity {
        // AttributeableEntity is abstract, therefore subclassing for testing
    }

    @Test
    public void testClear() {
        final AE entity = new AE();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
    }

    @Test
    public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 1, \"b\" : \"2\"}");

        final AE entity = new AE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getAttributes().size());
        assertEquals(Json.createValue(1), entity.getAttributes().get("a"));
        assertEquals(Json.createValue("2"), entity.getAttributes().get("b"));
    }

    @Test
    public void testToJSONObject() throws IOException {
        final AE entity = new AE();
        entity.getAttributes().put("a", Json.createValue(1));
        entity.getAttributes().put("b", Json.createValue("2"));

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 1, \"b\" : \"2\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }

    @Test
    public void testGetString() {
        final AE entity = new AE();
        assertNull(entity.getString("foo"));
        entity.getAttributes().put("foo", Json.createValue("bar"));
        assertEquals("bar", entity.getString("foo"));
        assertTrue(entity.getAttributes().isEmpty());
    }

    @Test
    public void testGetStringArray() throws IOException {
        final AE entity = new AE();
        assertNull(entity.getStringArray("foo"));
        entity.getAttributes().put("foo", Json.createValue("bar"));
        assertArrayEquals(new String[] {"bar"}, entity.getStringArray("foo"));
        assertTrue(entity.getAttributes().isEmpty());
        final JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add("a");
        jab.add("b");
        entity.getAttributes().put("foo", jab.build());
        assertArrayEquals(new String[] {"a", "b"}, entity.getStringArray("foo"));
        assertTrue(entity.getAttributes().isEmpty());
    }

    @Test
    public void testGetBoolean() throws IOException {
        final AE entity = new AE();
        assertTrue(entity.getBoolean("foo", true));

        entity.getAttributes().put("foo", JsonValue.FALSE);
        assertEquals(false, entity.getBoolean("foo", true));
        assertTrue(entity.getAttributes().isEmpty());

        try {
            entity.getAttributes().put("foo", Json.createValue(1.0));
            entity.getBoolean("foo", false);
            fail();
        } catch (final IOException expected) {
            // this is expected
        }
    }

    @Test
    public void testGetInteger() throws IOException {
        final AE entity = new AE();
        assertEquals(7, entity.getInteger("foo", 7));

        entity.getAttributes().put("foo", Json.createValue(9));
        assertEquals(9, entity.getInteger("foo", 7));
        assertTrue(entity.getAttributes().isEmpty());

        entity.getAttributes().put("foo", Json.createValue("9"));
        assertEquals(9, entity.getInteger("foo", 7));
        assertTrue(entity.getAttributes().isEmpty());
    }

    @Test
    public void testGetNumber() throws IOException {
        final AE entity = new AE();
        assertNull(entity.getNumber("foo"));

        entity.getAttributes().put("foo", Json.createValue(5));
        assertEquals(5L, entity.getNumber("foo"));
        assertTrue(entity.getAttributes().isEmpty());

        try {
            entity.getAttributes().put("foo", Json.createValue("a"));
            entity.getNumber("foo");
            fail();
        } catch (final IOException expected) {
            // this is expected
        }
    }

    @Test
    public void testSetStringArray() {
        final AE entity = new AE();

        JsonObjectBuilder builder = Json.createObjectBuilder();
        entity.setStringArray(builder, "foo", null);
        assertEquals("{}", builder.build().toString());

        builder = Json.createObjectBuilder();
        entity.setStringArray(builder, "foo", new String[0]);
        assertEquals("{}", builder.build().toString());

        builder = Json.createObjectBuilder();
        entity.setStringArray(builder, "foo", new String[] {"a", "b"});
        assertEquals("{\"foo\":[\"a\",\"b\"]}", builder.build().toString());
    }
}
