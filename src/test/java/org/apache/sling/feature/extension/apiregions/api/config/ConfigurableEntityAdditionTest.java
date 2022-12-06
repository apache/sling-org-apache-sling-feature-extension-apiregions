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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.json.Json;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

public class ConfigurableEntityAdditionTest {

    public static class CE extends ConfigurableEntityAddition {
        // ConfigurableEntityAddition is abstract, therefore subclassing for testing

        public CE() {
            setDefaults();
        }
    }

    @Test public void testClear() {
        final CE entity = new CE();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.getPropertyDescriptionAdditions().put("a", new PropertyDescriptionAddition());
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertTrue(entity.getPropertyDescriptionAdditions().isEmpty());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"properties\" : { \"a\" : {}, \"b\" : {}}}");

        final CE entity = new CE();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getPropertyDescriptionAdditions().size());
        assertNotNull(entity.getPropertyDescriptionAdditions().get("a"));
        assertNotNull(entity.getPropertyDescriptionAdditions().get("b"));
    }

    @Test public void testToJSONObject() throws IOException {
        final CE entity = new CE();
        entity.getPropertyDescriptionAdditions().put("a", new PropertyDescriptionAddition());
        entity.getPropertyDescriptionAdditions().put("b", new PropertyDescriptionAddition());

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
}