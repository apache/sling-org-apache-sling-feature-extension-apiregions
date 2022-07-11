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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.json.Json;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

public class PropertyDescriptionAdditionTest {

    @Test public void testClear() {
        final PropertyDescriptionAddition entity = new PropertyDescriptionAddition();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setIncludes(new String[] {"in"});
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertNull(entity.getIncludes());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"includes\" : [\"in\"]}");

        final PropertyDescriptionAddition entity = new PropertyDescriptionAddition();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());

        assertArrayEquals(new String[] {"in"}, entity.getIncludes());
   }

    @Test public void testToJSONObject() throws IOException {
        final PropertyDescriptionAddition entity = new PropertyDescriptionAddition();
        entity.setIncludes(new String[] {"in"});

        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"includes\" : [\"in\"]}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());

        // test defaults and empty values
        entity.setIncludes(null);

        ext.setJSON("{}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }
}
