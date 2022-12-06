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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.json.Json;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;
import org.osgi.framework.VersionRange;

public class ArtifactRulesTest {

    @Test public void testNullFeature() {
        assertNull(ArtifactRules.getArtifactRules((Feature)null));
    }

    @Test public void testNullExtension() {
        assertNull(ArtifactRules.getArtifactRules((Extension)null));
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        assertNull(ArtifactRules.getArtifactRules(f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongExtensionType() {
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        final Extension e = new Extension(ExtensionType.TEXT, ArtifactRules.EXTENSION_NAME, ExtensionState.OPTIONAL);
        f.getExtensions().add(e);
        ArtifactRules.getArtifactRules(f);
    }

    @Test public void testSetArtifactRules() {
        final ArtifactRules rules = new ArtifactRules();
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));

        assertNull(f.getExtensions().getByName(ArtifactRules.EXTENSION_NAME));

        ArtifactRules.setArtifactRules(f, rules);
        assertNotNull(f.getExtensions().getByName(ArtifactRules.EXTENSION_NAME));
        assertNotNull(ArtifactRules.getArtifactRules(f));

        ArtifactRules.setArtifactRules(f, null);
        assertNull(f.getExtensions().getByName(ArtifactRules.EXTENSION_NAME));
    }

    @Test public void testClear() {
        final ArtifactRules entity = new ArtifactRules();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.setMode(Mode.LENIENT);
        entity.getBundleVersionRules().add(new VersionRule());
        entity.getArtifactVersionRules().add(new VersionRule());
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertTrue(entity.getBundleVersionRules().isEmpty());
        assertTrue(entity.getArtifactVersionRules().isEmpty());
        assertEquals(Mode.STRICT, entity.getMode());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, ArtifactRules.EXTENSION_NAME, ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"bundle-version-rules\":[{"+
                "\"artifact-id\":\"g:a:1\",\"allowed-version-ranges\":[\"1.0.0\"]}]"+
                ", \"artifact-version-rules\":[{"+
                "\"artifact-id\":\"g:c:1\",\"allowed-version-ranges\":[\"2.0.0\"]}]}");

        final ArtifactRules entity = new ArtifactRules();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(Mode.LENIENT, entity.getMode());
        assertEquals(1, entity.getBundleVersionRules().size());
        assertEquals(ArtifactId.parse("g:a:1"), entity.getBundleVersionRules().get(0).getArtifactId());
        assertEquals(1, entity.getBundleVersionRules().get(0).getAllowedVersionRanges().length);
        assertEquals(new VersionRange("1.0.0"), entity.getBundleVersionRules().get(0).getAllowedVersionRanges()[0]);
        assertEquals(1, entity.getArtifactVersionRules().size());
        assertEquals(ArtifactId.parse("g:c:1"), entity.getArtifactVersionRules().get(0).getArtifactId());
        assertEquals(1, entity.getArtifactVersionRules().get(0).getAllowedVersionRanges().length);
        assertEquals(new VersionRange("2.0.0"), entity.getArtifactVersionRules().get(0).getAllowedVersionRanges()[0]);
    }

    @Test public void testToJSONObject() throws IOException {
        final ArtifactRules entity = new ArtifactRules();
        entity.setMode(Mode.LENIENT);
        final VersionRule rule = new VersionRule();
        rule.setArtifactId(ArtifactId.parse("g:a:1"));
        rule.setAllowedVersionRanges(new VersionRange[] {new VersionRange("1.0.0")});
        entity.getBundleVersionRules().add(rule);

        final VersionRule artifactRule = new VersionRule();
        artifactRule.setArtifactId(ArtifactId.parse("g:c:1"));
        artifactRule.setAllowedVersionRanges(new VersionRange[] {new VersionRange("2.0.0")});
        entity.getArtifactVersionRules().add(artifactRule);

        final Extension ext = new Extension(ExtensionType.JSON, ArtifactRules.EXTENSION_NAME, ExtensionState.OPTIONAL);
        ext.setJSON("{ \"mode\" : \"LENIENT\", \"bundle-version-rules\":[{"+
                "\"artifact-id\":\"g:a:1\",\"allowed-version-ranges\":[\"1.0.0\"]}]"+
                ", \"artifact-version-rules\":[{"+
                "\"artifact-id\":\"g:c:1\",\"allowed-version-ranges\":[\"2.0.0\"]}]}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());
    }
}
