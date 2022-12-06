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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

public class ConfigurationApiTest {

    @Test public void testNullFeature() {
        assertNull(ConfigurationApi.getConfigurationApi((Feature)null));
    }

    @Test public void testNullExtension() {
        assertNull(ConfigurationApi.getConfigurationApi((Extension)null));
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        assertNull(ConfigurationApi.getConfigurationApi(f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongExtensionType() {
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        final Extension e = new Extension(ExtensionType.TEXT, ConfigurationApi.EXTENSION_NAME, ExtensionState.OPTIONAL);
        f.getExtensions().add(e);
        ConfigurationApi.getConfigurationApi(f);
    }

    @Test public void testSetConfigurationApi() {
        final ConfigurationApi api = new ConfigurationApi();
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));

        assertNull(f.getExtensions().getByName(ConfigurationApi.EXTENSION_NAME));

        ConfigurationApi.setConfigurationApi(f, api);
        assertNotNull(f.getExtensions().getByName(ConfigurationApi.EXTENSION_NAME));
        assertNotNull(ConfigurationApi.getConfigurationApi(f));

        ConfigurationApi.setConfigurationApi(f, null);
        assertNull(f.getExtensions().getByName(ConfigurationApi.EXTENSION_NAME));
    }

    @Test public void testClear() {
        final ConfigurationApi entity = new ConfigurationApi();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.getConfigurationDescriptions().put("pid", new ConfigurationDescription());
        entity.getFactoryConfigurationDescriptions().put("factory", new FactoryConfigurationDescription());
        entity.getFrameworkPropertyDescriptions().put("prop", new FrameworkPropertyDescription());
        entity.getInternalFrameworkProperties().add("iprop");
        entity.setRegion(Region.GLOBAL);
        entity.getFeatureToRegionCache().put(ArtifactId.parse("g:a:1"), Region.GLOBAL);
        entity.setMode(Mode.SILENT);
        entity.getConfigurationDescriptionAdditions().put("pid", new ConfigurationDescriptionAddition());
        entity.getFactoryConfigurationDescriptionAdditions().put("factory", new FactoryConfigurationDescriptionAddition());
        entity.clear();
        assertTrue(entity.getAttributes().isEmpty());
        assertTrue(entity.getConfigurationDescriptions().isEmpty());
        assertTrue(entity.getFactoryConfigurationDescriptions().isEmpty());
        assertTrue(entity.getFrameworkPropertyDescriptions().isEmpty());
        assertTrue(entity.getInternalFrameworkProperties().isEmpty());
        assertNull(entity.getRegion());
        assertTrue(entity.getFeatureToRegionCache().isEmpty());
        assertTrue(entity.getConfigurationDescriptionAdditions().isEmpty());
        assertTrue(entity.getFactoryConfigurationDescriptionAdditions().isEmpty());
        assertEquals(Mode.STRICT, entity.getMode());
    }

    @Test public void testFromJSONObject() throws IOException {
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 5, \"configurations\" : { \"pid\": {}}, " +
            "\"factory-configurations\" : { \"factory\" : {}}," +
            "\"framework-properties\" : { \"prop\" : { \"type\" : \"STRING\"}}," +
            "\"internal-framework-properties\" : [\"iprop\"],"+
            "\"region\" : \"INTERNAL\","+
            "\"region-cache\" : {\"g:a1:feature:1.0.0\" : \"INTERNAL\", \"g:a2:feature:1.7.3\" : \"GLOBAL\"},"+
            "\"configuration-additions\" : { \"pida\": {}},"+
            "\"factory-configuration-additions\" : { \"factorya\": {}}"+
            "}");

        final ConfigurationApi entity = new ConfigurationApi();
        entity.fromJSONObject(ext.getJSONStructure().asJsonObject());
        assertEquals(1, entity.getConfigurationDescriptions().size());
        assertEquals(1, entity.getFactoryConfigurationDescriptions().size());
        assertEquals(1, entity.getConfigurationDescriptionAdditions().size());
        assertEquals(1, entity.getFactoryConfigurationDescriptionAdditions().size());
        assertEquals(1, entity.getFrameworkPropertyDescriptions().size());
        assertEquals(1, entity.getInternalFrameworkProperties().size());
        assertEquals(2, entity.getFeatureToRegionCache().size());
        assertTrue(entity.getConfigurationDescriptions().containsKey("pid"));
        assertTrue(entity.getFactoryConfigurationDescriptions().containsKey("factory"));
        assertTrue(entity.getConfigurationDescriptionAdditions().containsKey("pida"));
        assertTrue(entity.getFactoryConfigurationDescriptionAdditions().containsKey("factorya"));
        assertTrue(entity.getFrameworkPropertyDescriptions().containsKey("prop"));
        assertTrue(entity.getInternalFrameworkProperties().contains("iprop"));
        assertEquals(Region.INTERNAL, entity.getRegion());
        assertEquals(Region.INTERNAL, entity.getFeatureToRegionCache().get(ArtifactId.parse("g:a1:feature:1.0.0")));
        assertEquals(Region.GLOBAL, entity.getFeatureToRegionCache().get(ArtifactId.parse("g:a2:feature:1.7.3")));
        assertEquals(Mode.STRICT, entity.getMode());
    }

    @Test public void testToJSONObject() throws IOException {
        final ConfigurationApi entity = new ConfigurationApi();
        entity.getAttributes().put("a", Json.createValue(5));
        entity.getConfigurationDescriptions().put("pid", new ConfigurationDescription());
        entity.getFactoryConfigurationDescriptions().put("factory", new FactoryConfigurationDescription());
        entity.getFrameworkPropertyDescriptions().put("prop", new FrameworkPropertyDescription());
        entity.getConfigurationDescriptionAdditions().put("pida", new ConfigurationDescriptionAddition());
        entity.getFactoryConfigurationDescriptionAdditions().put("factorya", new FactoryConfigurationDescriptionAddition());
        entity.getInternalFrameworkProperties().add("iprop");
        entity.setRegion(Region.INTERNAL);
        entity.getFeatureToRegionCache().put(ArtifactId.parse("g:a1:feature:1.0.0"), Region.INTERNAL);
        entity.getFeatureToRegionCache().put(ArtifactId.parse("g:a2:feature:1.7.3"), Region.GLOBAL);
        entity.setMode(Mode.SILENT);
        final Extension ext = new Extension(ExtensionType.JSON, "a", ExtensionState.OPTIONAL);
        ext.setJSON("{ \"a\" : 5, \"configurations\" : { \"pid\": {}}, " +
            "\"factory-configurations\" : { \"factory\" : {}}," +
            "\"framework-properties\" : { \"prop\" : {}}," +
            "\"internal-framework-properties\" : [\"iprop\"],"+
            "\"region\" : \"INTERNAL\","+
            "\"region-cache\" : {\"g:a1:feature:1.0.0\" : \"INTERNAL\", \"g:a2:feature:1.7.3\" : \"GLOBAL\"}," +
            "\"configuration-additions\" : { \"pida\": {}},"+
            "\"factory-configuration-additions\" : { \"factorya\": {}},"+
            "\"mode\" : \"SILENT\"}");

        assertEquals(ext.getJSONStructure().asJsonObject(), entity.toJSONObject());        
    }

    @Test public void testDetectRegion() {
        final ConfigurationApi entity = new ConfigurationApi();
        assertEquals(Region.GLOBAL, entity.detectRegion());
        entity.setRegion(Region.GLOBAL);
        assertEquals(Region.GLOBAL, entity.detectRegion());
        entity.setRegion(Region.INTERNAL);
        assertEquals(Region.INTERNAL, entity.detectRegion());
    }

    @Test public void testIsInternalConfiguration() {
        final String PID = "org.apache.sling.configuration";
        final ConfigurationApi api = new ConfigurationApi();
        // ootb nothing is internal
        assertFalse(api.isInternalConfiguration(PID));

        // adding a normal description with a property does not change this
        final ConfigurationDescription desc = new ConfigurationDescription();
        final PropertyDescription propDesc = new PropertyDescription();
        desc.getPropertyDescriptions().put("foo", propDesc);
        api.getConfigurationDescriptions().put(PID, desc);
        assertFalse(api.isInternalConfiguration(PID));

        // a description without properties makes it internal
        desc.getPropertyDescriptions().clear();
        assertTrue(api.isInternalConfiguration(PID));
    }

    @Test public void testIsInternalFactoryConfigurationNoName() {
        final String FACTORYPID = "org.apache.sling.configuration";
        final ConfigurationApi api = new ConfigurationApi();
        // ootb nothing is internal
        assertFalse(api.isInternalFactoryConfiguration(FACTORYPID, null));

        // adding a normal description with a property does not change this
        final FactoryConfigurationDescription desc = new FactoryConfigurationDescription();
        final PropertyDescription propDesc = new PropertyDescription();
        desc.getPropertyDescriptions().put("foo", propDesc);
        api.getFactoryConfigurationDescriptions().put(FACTORYPID, desc);
        assertFalse(api.isInternalFactoryConfiguration(FACTORYPID, null));

        // a description without properties makes it internal
        desc.getPropertyDescriptions().clear();
        assertTrue(api.isInternalFactoryConfiguration(FACTORYPID, null));
    }

    @Test public void testIsInternalFactoryConfigurationWithName() {
        final String FACTORYPID = "org.apache.sling.configuration";
        final String NAME = "bar";
        final ConfigurationApi api = new ConfigurationApi();
        // ootb nothing is internal
        assertFalse(api.isInternalFactoryConfiguration(FACTORYPID, NAME));

        // adding a normal description with a property does not change this
        final FactoryConfigurationDescription desc = new FactoryConfigurationDescription();
        final PropertyDescription propDesc = new PropertyDescription();
        desc.getPropertyDescriptions().put("foo", propDesc);
        api.getFactoryConfigurationDescriptions().put(FACTORYPID, desc);
        assertFalse(api.isInternalFactoryConfiguration(FACTORYPID, NAME));

        // name can be added to internal names
        desc.getInternalNames().add(NAME);
        assertTrue(api.isInternalFactoryConfiguration(FACTORYPID, NAME));

        // a description without properties makes it internal
        desc.getInternalNames().clear();
        desc.getPropertyDescriptions().clear();
        assertTrue(api.isInternalFactoryConfiguration(FACTORYPID, NAME));
    }
}
