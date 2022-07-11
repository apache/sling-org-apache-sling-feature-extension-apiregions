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
 package org.apache.sling.feature.extension.apiregions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Prototype;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationApi;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescriptionAddition;
import org.apache.sling.feature.extension.apiregions.api.config.FactoryConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.FactoryConfigurationDescriptionAddition;
import org.apache.sling.feature.extension.apiregions.api.config.FrameworkPropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescriptionAddition;
import org.apache.sling.feature.extension.apiregions.api.config.Region;
import org.junit.Test;

public class ConfigurationApiMergeHandlerTest {

    @Test public void testPrototypeRegionMerge() {
        final Feature prototype = new Feature(ArtifactId.parse("g:p:1"));
        final ConfigurationApi prototypeApi = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);

        // always return prototype
        final BuilderContext context = new BuilderContext(id -> prototype);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());
        
        final Feature feature = new Feature(ArtifactId.parse("g:f:1"));
        feature.setPrototype(new Prototype(prototype.getId()));
        final ConfigurationApi featureApi = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(feature, featureApi);

        // no region
        Feature result = FeatureBuilder.assemble(feature, context);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(result);
        assertNotNull(api);
        assertNull(api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));

        // prototype has region
        prototypeApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.INTERNAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(prototype.getId()));

        prototypeApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));

        // feature has region
        prototypeApi.setRegion(null);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        featureApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.INTERNAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));

        featureApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));

        // both have region
        prototypeApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        featureApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.INTERNAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(prototype.getId()));

        prototypeApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        featureApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.INTERNAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));

        prototypeApi.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        featureApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(prototype.getId()));

        prototypeApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(prototype, prototypeApi);
        featureApi.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(feature, featureApi);
        result = FeatureBuilder.assemble(feature, context);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(1, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(prototype.getId()));
    }
 
    @Test public void testRegionMerge() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        // no region
        final ArtifactId id = ArtifactId.parse("g:m:1");
        Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(result);
        assertNotNull(api);
        assertNull(api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));

        // only A has region
        apiA.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));

        apiA.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));

        // only B has region
        apiA.setRegion(null);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        apiB.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureB.getId()));

        apiB.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));

        // both have region
        apiA.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        apiB.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.INTERNAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureB.getId()));

        apiA.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        apiB.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureB.getId()));

        apiA.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        apiB.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));

        apiA.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        apiB.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);
        result = FeatureBuilder.assemble(id, context, featureA, featureB);
        api = ConfigurationApi.getConfigurationApi(result);
        assertEquals(Region.GLOBAL, api.getRegion());
        assertEquals(2, api.getFeatureToRegionCache().size());
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));
    }

    @Test public void testConfigurationApiMergeDifferentConfig() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getConfigurationDescriptions().put("a", new ConfigurationDescription());
        apiA.getFactoryConfigurationDescriptions().put("fa", new FactoryConfigurationDescription());
        apiA.getFrameworkPropertyDescriptions().put("pa", new FrameworkPropertyDescription());
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getConfigurationDescriptions().put("b", new ConfigurationDescription());
        apiB.getFactoryConfigurationDescriptions().put("fb", new FactoryConfigurationDescription());
        apiB.getFrameworkPropertyDescriptions().put("pb", new FrameworkPropertyDescription());
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(result);
        assertNotNull(api);

        assertEquals(2, api.getConfigurationDescriptions().size());
        assertNotNull(api.getConfigurationDescriptions().get("a"));
        assertNotNull(api.getConfigurationDescriptions().get("b"));

        assertEquals(2, api.getFactoryConfigurationDescriptions().size());
        assertNotNull(api.getFactoryConfigurationDescriptions().get("fa"));
        assertNotNull(api.getFactoryConfigurationDescriptions().get("fb"));

        assertEquals(2, api.getFrameworkPropertyDescriptions().size());
        assertNotNull(api.getFrameworkPropertyDescriptions().get("pa"));
        assertNotNull(api.getFrameworkPropertyDescriptions().get("pb"));
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigurationApiMergeSameConfigurations() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getConfigurationDescriptions().put("a", new ConfigurationDescription());
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getConfigurationDescriptions().put("a", new ConfigurationDescription());
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        FeatureBuilder.assemble(id, context, featureA, featureB);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigurationApiMergeSameFactoryConfigurations() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getFactoryConfigurationDescriptions().put("fa", new FactoryConfigurationDescription());
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getFactoryConfigurationDescriptions().put("fa", new FactoryConfigurationDescription());
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        FeatureBuilder.assemble(id, context, featureA, featureB);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigurationApiMergeSameFrameworkProperties() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getFrameworkPropertyDescriptions().put("pa", new FrameworkPropertyDescription());
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getFrameworkPropertyDescriptions().put("pa", new FrameworkPropertyDescription());
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        FeatureBuilder.assemble(id, context, featureA, featureB);
    }

    @Test public void testConfigurationApiMergeInternalNames() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getInternalConfigurations().add("a");
        apiA.getInternalFactoryConfigurations().add("fa");
        apiA.getInternalFrameworkProperties().add("pa");

        apiA.getInternalConfigurations().add("c");
        apiA.getInternalFactoryConfigurations().add("fc");
        apiA.getInternalFrameworkProperties().add("pc");
        ConfigurationApi.setConfigurationApi(featureA, apiA);
        
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getInternalConfigurations().add("b");
        apiB.getInternalFactoryConfigurations().add("fb");
        apiB.getInternalFrameworkProperties().add("pb");

        apiB.getInternalConfigurations().add("c");
        apiB.getInternalFactoryConfigurations().add("fc");
        apiB.getInternalFrameworkProperties().add("pc");
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(result);
        assertNotNull(api);

        assertEquals(3, api.getInternalConfigurations().size());
        assertTrue(api.getInternalConfigurations().contains("a"));
        assertTrue(api.getInternalConfigurations().contains("b"));
        assertTrue(api.getInternalConfigurations().contains("c"));

        assertEquals(3, api.getInternalFactoryConfigurations().size());
        assertTrue(api.getInternalFactoryConfigurations().contains("fa"));
        assertTrue(api.getInternalFactoryConfigurations().contains("fb"));
        assertTrue(api.getInternalFactoryConfigurations().contains("fc"));

        assertEquals(3, api.getInternalFrameworkProperties().size());
        assertTrue(api.getInternalFrameworkProperties().contains("pa"));
        assertTrue(api.getInternalFrameworkProperties().contains("pb"));
        assertTrue(api.getInternalFrameworkProperties().contains("pc"));
    }

    @Test public void testConfigurationApiMergeRegionCache() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final Feature featureC = new Feature(ArtifactId.parse("g:c:1"));
        final ConfigurationApi apiC = new ConfigurationApi();
        apiC.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureC, apiC);

        final Feature featureD = new Feature(ArtifactId.parse("g:d:1"));
        final ConfigurationApi apiD = new ConfigurationApi();
        apiD.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(featureD, apiD);

        final ArtifactId idIntermediate = ArtifactId.parse("g:i:1");
        Feature intermediate = FeatureBuilder.assemble(idIntermediate, context, featureA, featureB);
        final ArtifactId id = ArtifactId.parse("g:m:1");
        Feature result = FeatureBuilder.assemble(id, context, featureC, featureD, intermediate);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(result);
        assertNotNull(api);


        assertEquals(5, api.getFeatureToRegionCache().size());
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureA.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(featureB.getId()));
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureC.getId()));
        assertEquals(Region.INTERNAL, api.getFeatureToRegionCache().get(featureD.getId()));
        assertEquals(Region.GLOBAL, api.getFeatureToRegionCache().get(idIntermediate));
    }

    @Test public void testConfigurationAdditions() {
        final ConfigurationDescriptionAddition cda1 = new ConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda11 = new PropertyDescriptionAddition();
        pda11.setIncludes(new String[] {"c"});
        cda1.getPropertyDescriptionAdditions().put("p1", pda11);
        final PropertyDescriptionAddition pda12 = new PropertyDescriptionAddition();
        pda12.setIncludes(new String[] {"x"});
        cda1.getPropertyDescriptionAdditions().put("p2", pda12);
        final ConfigurationDescriptionAddition cda2 = new ConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda21 = new PropertyDescriptionAddition();
        pda21.setIncludes(new String[] {"d"});
        cda2.getPropertyDescriptionAdditions().put("p3", pda21);
        final PropertyDescriptionAddition pda22 = new PropertyDescriptionAddition();
        pda22.setIncludes(new String[] {"y"});
        cda2.getPropertyDescriptionAdditions().put("p4", pda22);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getConfigurationDescriptionAdditions().put("pid1", cda1);

        final ConfigurationDescription cd1 = new ConfigurationDescription();
        final PropertyDescription pd11 = new PropertyDescription();
        pd11.setCardinality(-1);
        pd11.setIncludes(new String[] {"a", "b"});
        cd1.getPropertyDescriptions().put("p1", pd11);

        final PropertyDescription pd12 = new PropertyDescription();
        pd12.setCardinality(-1);
        cd1.getPropertyDescriptions().put("p2", pd12);

        final ConfigurationDescription cd2 = new ConfigurationDescription();
        final PropertyDescription pd21 = new PropertyDescription();
        pd21.setCardinality(-1);
        pd21.setIncludes(new String[] {"a", "b"});
        cd2.getPropertyDescriptions().put("p3", pd21);

        final PropertyDescription pd22 = new PropertyDescription();
        pd22.setCardinality(-1);
        cd2.getPropertyDescriptions().put("p4", pd22);

        apiA.getConfigurationDescriptions().put("pid1", cd1);
        apiA.getConfigurationDescriptions().put("pid2", cd2);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getConfigurationDescriptionAdditions().put("pid2", cda2);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        final Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);

        final ConfigurationApi resultApi = ConfigurationApi.getConfigurationApi(result);
        assertTrue(resultApi.getConfigurationDescriptionAdditions().isEmpty());
        final ConfigurationDescription resultCD1 = resultApi.getConfigurationDescriptions().get("pid1");
        assertNotNull(resultCD1);
        final PropertyDescription resultPD11 = resultCD1.getPropertyDescriptions().get("p1");
        assertNotNull(resultPD11);
        assertArrayEquals(new String[] {"a", "b", "c"}, resultPD11.getIncludes());
        final PropertyDescription resultPD12 = resultCD1.getPropertyDescriptions().get("p2");
        assertNotNull(resultPD12);
        assertArrayEquals(new String[] {"x"}, resultPD12.getIncludes());
        final ConfigurationDescription resultCD2 = resultApi.getConfigurationDescriptions().get("pid2");
        assertNotNull(resultCD2);
        final PropertyDescription resultPD21 = resultCD2.getPropertyDescriptions().get("p3");
        assertNotNull(resultPD21);
        assertArrayEquals(new String[] {"a", "b", "d"}, resultPD21.getIncludes());
        final PropertyDescription resultPD22 = resultCD2.getPropertyDescriptions().get("p4");
        assertNotNull(resultPD22);
        assertArrayEquals(new String[] {"y"}, resultPD22.getIncludes());
    }

    @Test public void testFactoryConfigurationAdditions() {
        final FactoryConfigurationDescriptionAddition cda1 = new FactoryConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda11 = new PropertyDescriptionAddition();
        pda11.setIncludes(new String[] {"c"});
        cda1.getPropertyDescriptionAdditions().put("p1", pda11);
        cda1.getInternalNames().add("mrx");
        final PropertyDescriptionAddition pda12 = new PropertyDescriptionAddition();
        pda12.setIncludes(new String[] {"x"});
        cda1.getPropertyDescriptionAdditions().put("p2", pda12);
        final FactoryConfigurationDescriptionAddition cda2 = new FactoryConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda21 = new PropertyDescriptionAddition();
        pda21.setIncludes(new String[] {"d"});
        cda2.getPropertyDescriptionAdditions().put("p3", pda21);
        final PropertyDescriptionAddition pda22 = new PropertyDescriptionAddition();
        pda22.setIncludes(new String[] {"y"});
        cda2.getPropertyDescriptionAdditions().put("p4", pda22);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getFactoryConfigurationDescriptionAdditions().put("factory1", cda1);

        final FactoryConfigurationDescription cd1 = new FactoryConfigurationDescription();
        cd1.getInternalNames().add("i1");
        final PropertyDescription pd11 = new PropertyDescription();
        pd11.setCardinality(-1);
        pd11.setIncludes(new String[] {"a", "b"});
        cd1.getPropertyDescriptions().put("p1", pd11);

        final PropertyDescription pd12 = new PropertyDescription();
        pd12.setCardinality(-1);
        cd1.getPropertyDescriptions().put("p2", pd12);

        final FactoryConfigurationDescription cd2 = new FactoryConfigurationDescription();
        cd2.getInternalNames().add("i2");
        final PropertyDescription pd21 = new PropertyDescription();
        pd21.setCardinality(-1);
        pd21.setIncludes(new String[] {"a", "b"});
        cd2.getPropertyDescriptions().put("p3", pd21);

        final PropertyDescription pd22 = new PropertyDescription();
        pd22.setCardinality(-1);
        cd2.getPropertyDescriptions().put("p4", pd22);

        apiA.getFactoryConfigurationDescriptions().put("factory1", cd1);
        apiA.getFactoryConfigurationDescriptions().put("factory2", cd2);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getFactoryConfigurationDescriptionAdditions().put("factory2", cda2);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        final Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);

        final ConfigurationApi resultApi = ConfigurationApi.getConfigurationApi(result);
        assertTrue(resultApi.getFactoryConfigurationDescriptionAdditions().isEmpty());
        final FactoryConfigurationDescription resultCD1 = resultApi.getFactoryConfigurationDescriptions().get("factory1");
        assertNotNull(resultCD1);
        assertEquals(Arrays.asList("i1", "mrx"), resultCD1.getInternalNames());
        final PropertyDescription resultPD11 = resultCD1.getPropertyDescriptions().get("p1");
        assertNotNull(resultPD11);
        assertArrayEquals(new String[] {"a", "b", "c"}, resultPD11.getIncludes());
        final PropertyDescription resultPD12 = resultCD1.getPropertyDescriptions().get("p2");
        assertNotNull(resultPD12);
        assertArrayEquals(new String[] {"x"}, resultPD12.getIncludes());
        final FactoryConfigurationDescription resultCD2 = resultApi.getFactoryConfigurationDescriptions().get("factory2");
        assertNotNull(resultCD2);
        assertEquals(Arrays.asList("i2"), resultCD2.getInternalNames());
        final PropertyDescription resultPD21 = resultCD2.getPropertyDescriptions().get("p3");
        assertNotNull(resultPD21);
        assertArrayEquals(new String[] {"a", "b", "d"}, resultPD21.getIncludes());
        final PropertyDescription resultPD22 = resultCD2.getPropertyDescriptions().get("p4");
        assertNotNull(resultPD22);
        assertArrayEquals(new String[] {"y"}, resultPD22.getIncludes());
    }

    @Test public void testConfigurationAdditionsConfigDoesNotExist() {
        final ConfigurationDescriptionAddition cda = new ConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda = new PropertyDescriptionAddition();
        pda.setIncludes(new String[] {"a"});
        cda.getPropertyDescriptionAdditions().put("p1", pda);

        final FactoryConfigurationDescriptionAddition cdb = new FactoryConfigurationDescriptionAddition();
        cdb.getPropertyDescriptionAdditions().put("p1", pda);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        apiA.getConfigurationDescriptionAdditions().put("pid1", cda);
        apiA.getFactoryConfigurationDescriptionAdditions().put("factory1", cdb);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getConfigurationDescriptionAdditions().put("pid2", cda);
        apiB.getFactoryConfigurationDescriptionAdditions().put("factory2", cdb);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        final Feature result = FeatureBuilder.assemble(id, context, featureA, featureB);
        final ConfigurationApi resultApi = ConfigurationApi.getConfigurationApi(result);
        assertEquals(2, resultApi.getConfigurationDescriptionAdditions().size());
        assertNotNull(resultApi.getConfigurationDescriptionAdditions().get("pid1"));
        assertNotNull(resultApi.getConfigurationDescriptionAdditions().get("pid2"));
        assertEquals(2, resultApi.getFactoryConfigurationDescriptionAdditions().size());
        assertNotNull(resultApi.getFactoryConfigurationDescriptionAdditions().get("factory1"));
        assertNotNull(resultApi.getFactoryConfigurationDescriptionAdditions().get("factory2"));
    }

    @Test(expected = IllegalStateException.class) 
    public void testConfigurationAdditionsConfigPropDoesNotExist() {
        final ConfigurationDescriptionAddition cda = new ConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda = new PropertyDescriptionAddition();
        pda.setIncludes(new String[] {"c"});
        cda.getPropertyDescriptionAdditions().put("p1", pda);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        final ConfigurationDescription cd = new ConfigurationDescription();
        apiA.getConfigurationDescriptions().put("pid", cd);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getConfigurationDescriptionAdditions().put("pid", cda);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        FeatureBuilder.assemble(id, context, featureA, featureB);
    }

    @Test(expected = IllegalStateException.class) 
    public void testConfigurationAdditionsFactoryConfigPropDoesNotExist() {
        final FactoryConfigurationDescriptionAddition cda = new FactoryConfigurationDescriptionAddition();
        final PropertyDescriptionAddition pda = new PropertyDescriptionAddition();
        pda.setIncludes(new String[] {"c"});
        cda.getPropertyDescriptionAdditions().put("p1", pda);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ConfigurationApi apiA = new ConfigurationApi();
        final FactoryConfigurationDescription cd = new FactoryConfigurationDescription();
        apiA.getFactoryConfigurationDescriptions().put("pid", cd);
        ConfigurationApi.setConfigurationApi(featureA, apiA);

        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ConfigurationApi apiB = new ConfigurationApi();
        apiB.getFactoryConfigurationDescriptionAdditions().put("pid", cda);
        ConfigurationApi.setConfigurationApi(featureB, apiB);

        final ArtifactId id = ArtifactId.parse("g:m:1");
        FeatureBuilder.assemble(id, context, featureA, featureB);
    }
}