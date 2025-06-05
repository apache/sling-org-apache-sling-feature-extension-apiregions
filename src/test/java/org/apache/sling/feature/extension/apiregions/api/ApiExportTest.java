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

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiExportTest {

    private static final String MSG = "deprecated!";

    private static final String SINCE = "now";

    private static final String PCK = "org.apache.sling";

    private static final String FOR_REMOVAL = "2021-01-01";

    @Test(expected = IllegalArgumentException.class)
    public void testNameRequired() throws Exception {
        new ApiExport(PCK);
        new ApiExport(null);
    }

    private JsonObject getJson(final String text) {
        try (final StringReader reader = new StringReader(text)) {
            return Json.createReader(reader).readObject();
        }
    }

    @Test
    public void testPackageDeprecationSimpleMessage() throws Exception {
        final JsonValue jv = Json.createValue(MSG);

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertEquals(MSG, exp.getDeprecation().getPackageInfo().getMessage());
        assertNull(exp.getDeprecation().getPackageInfo().getSince());
        assertTrue(exp.getDeprecation().getMemberInfos().isEmpty());

        assertEquals(jv, exp.deprecationToJSON());
    }

    @Test
    public void testPackageDeprecationMessageAndSince() throws Exception {
        final JsonValue jv = getJson("{\"msg\":\"" + MSG + "\",\"since\":\"" + SINCE + "\"}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertEquals(MSG, exp.getDeprecation().getPackageInfo().getMessage());
        assertEquals(SINCE, exp.getDeprecation().getPackageInfo().getSince());
        assertNull(exp.getDeprecation().getPackageInfo().getForRemoval());
        assertTrue(exp.getDeprecation().getMemberInfos().isEmpty());

        assertEquals(jv, exp.deprecationToJSON());
    }

    @Test
    public void testPackageDeprecationMessageAndForRemoval() throws Exception {
        final JsonValue jv = getJson("{\"msg\":\"" + MSG + "\",\"for-removal\":\"" + FOR_REMOVAL + "\"}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertEquals(MSG, exp.getDeprecation().getPackageInfo().getMessage());
        assertEquals(FOR_REMOVAL, exp.getDeprecation().getPackageInfo().getForRemoval());
        assertNull(exp.getDeprecation().getPackageInfo().getSince());
        assertTrue(exp.getDeprecation().getMemberInfos().isEmpty());

        assertEquals(jv, exp.deprecationToJSON());
    }

    @Test
    public void testSimpleMembers() throws Exception {
        final JsonValue jv = getJson("{\"members\":{\"foo\":\"" + MSG + "\",\"bar\":\"" + MSG + MSG + "\"}}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertNull(exp.getDeprecation().getPackageInfo());
        assertEquals(2, exp.getDeprecation().getMemberInfos().size());

        final DeprecationInfo foo = exp.getDeprecation().getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = exp.getDeprecation().getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertNull(bar.getSince());

        assertEquals(jv, exp.deprecationToJSON());
    }

    @Test
    public void testComplexMembers() throws Exception {
        final JsonValue jv = getJson("{\"members\":{\"foo\":{\"msg\":\"" + MSG + "\"},\"bar\":{\"msg\":\"" + MSG + MSG
                + "\",\"since\":\"" + SINCE + "\"}}}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertNull(exp.getDeprecation().getPackageInfo());
        assertEquals(2, exp.getDeprecation().getMemberInfos().size());

        final DeprecationInfo foo = exp.getDeprecation().getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = exp.getDeprecation().getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertEquals(SINCE, bar.getSince());

        // the expected JSON is actually a mixed JSON (not the input!)
        final JsonValue expJV = getJson("{\"members\":{\"foo\":\"" + MSG + "\",\"bar\":{\"msg\":\"" + MSG + MSG
                + "\",\"since\":\"" + SINCE + "\"}}}");
        assertEquals(expJV, exp.deprecationToJSON());
    }

    @Test
    public void testMixedMembers() throws Exception {
        final JsonValue jv = getJson("{\"members\":{\"foo\":\"" + MSG + "\",\"bar\":{\"msg\":\"" + MSG + MSG
                + "\",\"since\":\"" + SINCE + "\"}}}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertNull(exp.getDeprecation().getPackageInfo());
        assertEquals(2, exp.getDeprecation().getMemberInfos().size());

        final DeprecationInfo foo = exp.getDeprecation().getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = exp.getDeprecation().getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertEquals(SINCE, bar.getSince());

        assertEquals(jv, exp.deprecationToJSON());
    }

    @Test
    public void testMode() throws Exception {
        final JsonValue jv =
                getJson("{\"msg\":\"" + MSG + "\",\"mode\":\"" + DeprecationValidationMode.STRICT.name() + "\"}");

        final ApiExport exp = new ApiExport(PCK);
        exp.parseDeprecation(jv);

        assertEquals(
                DeprecationValidationMode.STRICT,
                exp.getDeprecation().getPackageInfo().getMode());

        assertEquals(jv, exp.deprecationToJSON());
    }
}
