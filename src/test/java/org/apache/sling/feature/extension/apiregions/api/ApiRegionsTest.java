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
package org.apache.sling.feature.extension.apiregions.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

public class ApiRegionsTest {

    private String readJSON(final String name) throws IOException {
        try (final Reader reader = new InputStreamReader(
                ApiRegionsTest.class.getResourceAsStream("/json/" + name + ".json"),
                "UTF-8"); final Writer writer = new StringWriter()) {
            int l;
            char[] buf = new char[2048];
            while ((l = reader.read(buf)) > -1) {
                writer.write(buf, 0, l);
            }

            return writer.toString();
        }
    }

    @Test
    public void testParsing() throws Exception {
        final String json = readJSON("apis");

        final ApiRegions regions = ApiRegions.parse(json);
        assertNotNull(regions);

        assertEquals(2, regions.listRegions().size());

        final ApiRegion global = regions.listRegions().get(0);
        assertEquals("global", global.getName());

        assertEquals(2, global.listExports().size());

        final ApiRegion internal = regions.listRegions().get(1);
        assertEquals("internal", internal.getName());

        assertEquals(1, internal.listExports().size());
    }

    @Test
    public void testOrdering() throws Exception {
        final ApiRegions regions = new ApiRegions();
        final ApiRegion one = new ApiRegion("one");
        one.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        final ApiRegion two = new ApiRegion("two");
        two.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        final ApiRegion three = new ApiRegion("three");
        three.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));

        final ApiRegion duplicate = new ApiRegion("two");
        duplicate.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        final ApiRegion other = new ApiRegion("other");
        other.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));

        assertTrue(regions.add(one));
        assertTrue(regions.add(two));
        assertTrue(regions.add(three));

        assertFalse(regions.add(duplicate));

        assertEquals(3, regions.listRegions().size());

        assertNull(one.getParent());
        assertEquals(one, two.getParent());
        assertEquals(two, three.getParent());
    }

    @Test
    public void testExports() throws Exception {
        final ApiRegions regions = new ApiRegions();

        final ApiRegion one = new ApiRegion("one");
        one.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        one.add(new ApiExport("a"));

        final ApiRegion two = new ApiRegion("two");
        two.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        two.add(new ApiExport("b"));

        final ApiRegion three = new ApiRegion("three");
        three.setFeatureOrigins(ArtifactId.fromMvnId("f:f1:1"));
        three.add(new ApiExport("c"));

        assertTrue(regions.add(one));
        assertTrue(regions.add(two));
        assertTrue(regions.add(three));

        assertEquals(1, one.listAllExports().size());
        assertTrue(one.listAllExports().contains(new ApiExport("a")));
        assertEquals(1, one.listExports().size());
        assertTrue(one.listExports().contains(new ApiExport("a")));

        assertEquals(2, two.listAllExports().size());
        assertTrue(two.listAllExports().contains(new ApiExport("a")));
        assertTrue(two.listAllExports().contains(new ApiExport("b")));
        assertEquals(1, two.listExports().size());
        assertTrue(two.listExports().contains(new ApiExport("b")));

        assertEquals(3, three.listAllExports().size());
        assertTrue(three.listAllExports().contains(new ApiExport("a")));
        assertTrue(three.listAllExports().contains(new ApiExport("b")));
        assertTrue(three.listAllExports().contains(new ApiExport("c")));
        assertEquals(1, three.listExports().size());
        assertTrue(three.listExports().contains(new ApiExport("c")));
    }

    @Test public void testNullFeature() {
        assertNull(ApiRegions.getApiRegions((Feature)null));
    }

    @Test public void testNullExtension() {
        assertNull(ApiRegions.getApiRegions((Extension)null));
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        assertNull(ApiRegions.getApiRegions(f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongExtensionType() {
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        final Extension e = new Extension(ExtensionType.TEXT, ApiRegions.EXTENSION_NAME, ExtensionState.OPTIONAL);
        f.getExtensions().add(e);
        ApiRegions.getApiRegions(f);
    }

    @Test
    public void testDeprecationJSON() throws Exception {
        final String json = readJSON("apis-deprecation");

        final ApiRegions regions = ApiRegions.parse(json);
        assertNotNull(regions);

        assertEquals(1, regions.listRegions().size());

        final ApiRegion internal = regions.listRegions().get(0);
        assertEquals("internal", internal.getName());

        assertEquals(1, internal.listExports().size());

        final ApiExport exp = internal.listExports().iterator().next();

        assertEquals("org.apache.sling.internal", exp.getName());
        assertNotNull(exp.getDeprecation().getPackageInfo());
        assertEquals("deprecated", exp.getDeprecation().getPackageInfo().getMessage());

        final JsonArray array = regions.toJSONArray();
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            final JsonArray orig = reader.readArray();
            assertEquals(orig, array);
        }
    }

    @Test public void testToggles() throws Exception {
        final String json = readJSON("apis-toggles");

        final ApiRegions regions = ApiRegions.parse(json);
        assertNotNull(regions);

        assertEquals(1, regions.listRegions().size());

        final ApiRegion global = regions.listRegions().get(0);
        assertEquals("global", global.getName());

        assertEquals(2, global.listExports().size());

        final Iterator<ApiExport> iter = global.listExports().iterator();
        final ApiExport exp1 = iter.next();
        assertEquals("org.apache.sling.global", exp1.getName());
        assertEquals("sling_enabled", exp1.getToggle());
        assertNull(exp1.getPreviousArtifactId());

        final ApiExport exp2 = iter.next();
        assertEquals("org.apache.felix.global", exp2.getName());
        assertEquals("global_enabled", exp2.getToggle());
        assertEquals(ArtifactId.parse("org.apache.felix:api:1.1"), exp2.getPreviousArtifactId());

        // create json and parse
        final ApiRegions regions2 = ApiRegions.parse(regions.toJSONArray());
        assertEquals(regions, regions2);
    }
}
