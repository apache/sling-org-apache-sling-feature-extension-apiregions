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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.json.Json;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

public class FactoryConfigurationDescriptionAdditionTest {

    @Test public void testClear() {
        final FactoryConfigurationDescriptionAddition entity = new FactoryConfigurationDescriptionAddition();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.getPropertyDescriptionAdditions().put("a", new PropertyDescriptionAddition());
        entity.getInternalNames().add("internal");
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertTrue(entity.getPropertyDescriptionAdditions().isEmpty());
        assertTrue(entity.getInternalNames().isEmpty());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"internal-names\" : [ \"a\", \"b\"]}");

        final FactoryConfigurationDescriptionAddition entity = new FactoryConfigurationDescriptionAddition();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(2, entity.getInternalNames().size());
        assertTrue(entity.getInternalNames().contains("a"));
        assertTrue(entity.getInternalNames().contains("b"));
    }

    @Test public void testToJSONObject() throws IOException {
        final FactoryConfigurationDescription entity = new FactoryConfigurationDescription();
        entity.getInternalNames().add("a");
        entity.getInternalNames().add("b");

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"internal-names\" : [ \"a\", \"b\"]}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }
}