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
package org.apache.sling.feature.extension.apiregions.api.config.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.extension.apiregions.ConfigurationApiMergeHandler;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationApi;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.FactoryConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.FrameworkPropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.Operation;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;
import org.apache.sling.feature.extension.apiregions.api.config.Range;
import org.apache.sling.feature.extension.apiregions.api.config.Region;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FeatureValidatorTest {

    private static final String PID = "org.apache.sling";

    private static final String FACTORY_PID = "org.apache.sling.factory";

    private final FeatureValidator validator = new FeatureValidator();

    @Before
    public void setup() {
        this.validator.setFeatureProvider(null);
    }

    private Feature createFeature(final String id) {
        final Feature f = new Feature(ArtifactId.parse(id));
        final Configuration c = new Configuration(PID);
        c.getProperties().put("prop", "a");
        f.getConfigurations().add(c);

        final Configuration fc = new Configuration(FACTORY_PID.concat("~print"));
        fc.getProperties().put("key", "value");
        f.getConfigurations().add(fc);

        f.getFrameworkProperties().put("prop", "1");

        return f;
    }

    private ConfigurationApi createApi() {
        final ConfigurationApi api = new ConfigurationApi();

        final ConfigurationDescription cd = new ConfigurationDescription();
        cd.getPropertyDescriptions().put("prop", new PropertyDescription());

        api.getConfigurationDescriptions().put(PID, cd);

        final FactoryConfigurationDescription fd = new FactoryConfigurationDescription();
        fd.getPropertyDescriptions().put("key", new PropertyDescription());

        api.getFactoryConfigurationDescriptions().put(FACTORY_PID, fd);

        final FrameworkPropertyDescription fpd = new FrameworkPropertyDescription();
        fpd.setType(PropertyType.INTEGER);
        api.getFrameworkPropertyDescriptions().put("prop", fpd);

        return api;
    }

    private FeatureValidator.RegionInfo getRegionInfo(final Feature f, final Configuration c) {
        final Map<ArtifactId, Region> cache = new HashMap<>();
        final ConfigurationApi api = ConfigurationApi.getConfigurationApi(f);
        if (api != null) {
            cache.putAll(api.getFeatureToRegionCache());
        }
        if (api == null || api.getRegion() == null) {
            cache.put(f.getId(), Region.GLOBAL);
        } else {
            cache.put(f.getId(), api.getRegion());
        }
        return validator.getRegionInfo(f, c, cache);
    }

    private FeatureValidator.RegionInfo getRegionInfo(final Feature f, final String name) {
        final Map<ArtifactId, Region> cache = new HashMap<>();
        final ConfigurationApi api = ConfigurationApi.getConfigurationApi(f);
        if (api != null) {
            cache.putAll(api.getFeatureToRegionCache());
        }
        if (api == null || api.getRegion() == null) {
            cache.put(f.getId(), Region.GLOBAL);
        } else {
            cache.put(f.getId(), api.getRegion());
        }
        return validator.getRegionInfo(f, name, cache);
    }

    @Test
    public void testGetRegionInfoConfigurationNoOrigin() {
        final Feature f1 = createFeature("g:a:1");
        final Configuration cfg = f1.getConfigurations().getConfiguration(PID);

        // no api set
        FeatureValidator.RegionInfo info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // empty region in api
        final ConfigurationApi api = createApi();
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // global region in api
        api.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // internal region in api
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.INTERNAL, info.region);
        assertFalse(info.isUpdate);
    }

    @Test
    public void testGetRegionInfoConfigurationSingleOrigin() {
        final Feature f1 = createFeature("g:a:1");
        final Configuration cfg = f1.getConfigurations().getConfiguration(PID);

        final Feature f2 = createFeature("g:b:1");
        cfg.setFeatureOrigins(Collections.singletonList(f2.getId()));

        // set feature provider to always provide f2
        this.validator.setFeatureProvider(id -> f2);
        // no api in origin
        FeatureValidator.RegionInfo info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // no region in api
        final ConfigurationApi api2 = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(f2, api2);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // global in api
        api2.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // internal in api
        api2.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.INTERNAL, info.region);
        assertFalse(info.isUpdate);

        // unknown id
        this.validator.setFeatureProvider(id -> null);
        cfg.setFeatureOrigins(Collections.singletonList(ArtifactId.parse("g:xy:1")));
        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
    }

    @Test
    public void testGetRegionInfoConfigurationMultipleOrigins() {
        final Feature f1 = createFeature("g:a:1");
        final Configuration cfg = f1.getConfigurations().getConfiguration(PID);

        final Feature f2 = createFeature("g:b:1");
        final Feature f3 = createFeature("g:c:1");
        cfg.setFeatureOrigins(Arrays.asList(f2.getId(), f3.getId()));

        final FeatureProvider provider = new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {
                if (f1.getId().equals(id)) {
                    return f1;
                } else if (f2.getId().equals(id)) {
                    return f2;
                } else if (f3.getId().equals(id)) {
                    return f3;
                }
                return null;
            }
        };

        this.validator.setFeatureProvider(provider);

        // no api in origins
        FeatureValidator.RegionInfo info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // global-internal
        final ConfigurationApi api2 = new ConfigurationApi();
        final ConfigurationApi api3 = new ConfigurationApi();
        api2.setRegion(Region.GLOBAL);
        api3.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);

        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // global-global
        api2.setRegion(Region.GLOBAL);
        api3.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);

        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // internal-internal
        api2.setRegion(Region.INTERNAL);
        api3.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);
        ConfigurationApi f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        f1Api.getFeatureToRegionCache().put(f3.getId(), Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);

        info = getRegionInfo(f1, cfg);
        assertEquals(Region.INTERNAL, info.region);
        assertTrue(info.isUpdate);

        // internal-global
        api2.setRegion(Region.INTERNAL);
        api3.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);
        f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        f1Api.getFeatureToRegionCache().put(f3.getId(), Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);

        info = getRegionInfo(f1, cfg);
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);
    }

    @Test
    public void testGetRegionInfoFrameworkPropertyNoOrigin() {
        final Feature f1 = createFeature("g:a:1");

        // no api set
        FeatureValidator.RegionInfo info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // empty region in api
        final ConfigurationApi api = createApi();
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // global region in api
        api.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // internal region in api
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.INTERNAL, info.region);
        assertFalse(info.isUpdate);
    }

    @Test
    public void testGetRegionInfoFrameworkPropertySingleOrigin() {
        final Feature f1 = createFeature("g:a:1");

        final Feature f2 = createFeature("g:b:1");
        f1.setFeatureOrigins(f1.getFrameworkPropertyMetadata("prop"), Collections.singletonList(f2.getId()));

        // set feature provider to always provide f2
        this.validator.setFeatureProvider(id -> f2);
        // no api in origin
        FeatureValidator.RegionInfo info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // no region in api
        final ConfigurationApi api2 = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(f2, api2);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // global in api
        api2.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertFalse(info.isUpdate);

        // internal in api
        api2.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.INTERNAL, info.region);
        assertFalse(info.isUpdate);

        // unknown id
        this.validator.setFeatureProvider(id -> null);
        f1.setFeatureOrigins(
                f1.getFrameworkPropertyMetadata("prop"), Collections.singletonList(ArtifactId.parse("g:xy:1")));
        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
    }

    @Test
    public void testGetRegionInfoFrameworkPropertyMultipleOrigins() {
        final Feature f1 = createFeature("g:a:1");

        final Feature f2 = createFeature("g:b:1");
        final Feature f3 = createFeature("g:c:1");
        f1.setFeatureOrigins(f1.getFrameworkPropertyMetadata("prop"), Arrays.asList(f2.getId(), f3.getId()));

        final FeatureProvider provider = new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {
                if (f1.getId().equals(id)) {
                    return f1;
                } else if (f2.getId().equals(id)) {
                    return f2;
                } else if (f3.getId().equals(id)) {
                    return f3;
                }
                return null;
            }
        };

        this.validator.setFeatureProvider(provider);

        // no api in origins
        FeatureValidator.RegionInfo info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // global-internal
        final ConfigurationApi api2 = new ConfigurationApi();
        final ConfigurationApi api3 = new ConfigurationApi();
        api2.setRegion(Region.GLOBAL);
        api3.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);

        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // global-global
        api2.setRegion(Region.GLOBAL);
        api3.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);

        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);

        // internal-internal
        api2.setRegion(Region.INTERNAL);
        api3.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);
        ConfigurationApi f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        f1Api.getFeatureToRegionCache().put(f3.getId(), Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);

        info = getRegionInfo(f1, "prop");
        assertEquals(Region.INTERNAL, info.region);
        assertTrue(info.isUpdate);

        // internal-global
        api2.setRegion(Region.INTERNAL);
        api3.setRegion(Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f2, api2);
        ConfigurationApi.setConfigurationApi(f3, api3);
        f1Api = new ConfigurationApi();
        f1Api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        f1Api.getFeatureToRegionCache().put(f3.getId(), Region.GLOBAL);
        ConfigurationApi.setConfigurationApi(f1, f1Api);

        info = getRegionInfo(f1, "prop");
        assertEquals(Region.GLOBAL, info.region);
        assertTrue(info.isUpdate);
    }

    @Test
    public void testSingleConfigurationValidation() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = createApi();
        ConfigurationApi.setConfigurationApi(f1, api);

        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // add property
        f1.getConfigurations().getConfiguration(PID).getProperties().put("b", "x");
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
    }

    @Test
    public void testInternalConfiguration() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(f1, api);

        // global region
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // mark configurations as internal
        api.getConfigurationDescriptions().put(PID, new ConfigurationDescription());
        api.getFactoryConfigurationDescriptions().put(FACTORY_PID, new FactoryConfigurationDescription());
        ConfigurationApi.setConfigurationApi(f1, api);

        // global region
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getConfigurationResults().get(PID).isValid());
        assertFalse(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // internal region
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testInternalFactoryNames() {
        final Feature f1 = createFeature("g:a:1");

        final Configuration fa = new Configuration(FACTORY_PID.concat("~a"));
        fa.getProperties().put("key", "value");
        f1.getConfigurations().add(fa);

        final Configuration fb = new Configuration(FACTORY_PID.concat("~b"));
        fb.getProperties().put("key", "value");
        f1.getConfigurations().add(fb);

        final ConfigurationApi api = createApi();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getInternalNames()
                .add("a");
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getInternalNames()
                .add("b");
        ConfigurationApi.setConfigurationApi(f1, api);

        // global region -> fail
        FeatureValidationResult result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(
                result.getConfigurationResults().get(FACTORY_PID.concat("~a")).isValid());
        assertFalse(
                result.getConfigurationResults().get(FACTORY_PID.concat("~b")).isValid());
        assertTrue(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // global region, lenient -> warn
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(Mode.LENIENT);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~a"))
                        .getWarnings()
                        .size());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~b"))
                        .getWarnings()
                        .size());
        assertEquals(
                0,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~print"))
                        .getWarnings()
                        .size());

        // internal region
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        ConfigurationApi.setConfigurationApi(f1, api);
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testFactoryConfigurationOperationsWithCreate() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = createApi();

        // no operation -> fail
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        FeatureValidationResult result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // no operation, lenient -> warn
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(Mode.LENIENT);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~print"))
                        .getWarnings()
                        .size());

        // only update -> fail
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // only update, lenient -> warn
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(Mode.LENIENT);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~print"))
                        .getWarnings()
                        .size());

        // only create -> success
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // update, create -> success
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // internal region -> always success
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testFactoryConfigurationOperationsWithUpdate() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = createApi();

        final Configuration cfg = f1.getConfigurations().getConfiguration(FACTORY_PID.concat("~print"));

        final Feature f2 = createFeature("g:b:1");
        final Feature f3 = createFeature("g:c:1");
        cfg.setFeatureOrigins(Arrays.asList(f2.getId(), f3.getId()));

        final FeatureProvider provider = new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {
                if (f1.getId().equals(id)) {
                    return f1;
                } else if (f2.getId().equals(id)) {
                    return f2;
                } else if (f3.getId().equals(id)) {
                    return f3;
                }
                return null;
            }
        };

        this.validator.setFeatureProvider(provider);

        // no operation -> fail
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        FeatureValidationResult result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // no operation, lenient -> warn
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(Mode.LENIENT);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~print"))
                        .getWarnings()
                        .size());

        // only update -> success
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // only create -> fail
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getConfigurationResults()
                .get(FACTORY_PID.concat("~print"))
                .isValid());

        // only create, lenient -> warn
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(Mode.LENIENT);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertEquals(
                1,
                result.getConfigurationResults()
                        .get(FACTORY_PID.concat("~print"))
                        .getWarnings()
                        .size());

        // update, create -> success
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setMode(null);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // internal region -> always success
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f2, api);
        ConfigurationApi.setConfigurationApi(f3, api);
        // need to fill cache
        api.getFeatureToRegionCache().put(f1.getId(), Region.INTERNAL);
        api.getFeatureToRegionCache().put(f2.getId(), Region.INTERNAL);
        api.getFeatureToRegionCache().put(f3.getId(), Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.UPDATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .add(Operation.CREATE);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getOperations()
                .clear();
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testInternalFrameworkProperty() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = new ConfigurationApi();
        ConfigurationApi.setConfigurationApi(f1, api);

        // global region
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // mark framework property as internal
        api.getInternalFrameworkProperties().add("prop");
        ConfigurationApi.setConfigurationApi(f1, api);

        // global region
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getFrameworkPropertyResults().get("prop").isValid());

        // internal region
        api.setRegion(Region.INTERNAL);
        ConfigurationApi.setConfigurationApi(f1, api);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testFrameworkProperty() {
        final Feature f1 = createFeature("g:a:1");
        final ConfigurationApi api = createApi();
        ConfigurationApi.setConfigurationApi(f1, api);

        // value is valid
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // no value -> valid
        f1.getFrameworkProperties().remove("prop");
        result = validator.validate(f1, api);
        assertTrue(result.isValid());

        // invalid value
        f1.getFrameworkProperties().put("prop", "foo");
        result = validator.validate(f1, api);
        assertFalse(result.isValid());
        assertFalse(result.getFrameworkPropertyResults().get("prop").isValid());
    }

    @Test
    public void testRegionCache() {
        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ConfigurationApiMergeHandler());

        final Feature f1 = createFeature("g:a:1");
        f1.getConfigurations().clear();
        final Feature f2 = createFeature("g:b:1");

        final Feature aggregate = FeatureBuilder.assemble(ArtifactId.parse("g:agg:1"), context, f1, f2);
        ConfigurationApi api = ConfigurationApi.getConfigurationApi(aggregate);
        assertNull(api);

        final Feature f3 = createFeature("g:c:1");
        f3.getConfigurations().clear();
        api = this.createApi();
        ConfigurationApi.setConfigurationApi(f3, api);
        final Feature feature = FeatureBuilder.assemble(ArtifactId.parse("g:f:1"), context, f3, aggregate);

        FeatureValidationResult result = validator.validate(feature);
        assertTrue(result.isValid());
    }

    @Test
    public void testDefinitiveModeForConfigurationProperties() {
        for (int i = 0; i < 2; i++) {
            final Feature f = new Feature(ArtifactId.parse("g:a:1"));
            final Configuration cfg = new Configuration("org.apache.sling");
            cfg.getProperties().put("a", 1);
            cfg.getProperties().put("b", 1);
            cfg.getProperties().put("c", 1);
            if (i == 0) {
                cfg.getProperties().put("d", new String[] {"a", "b", "c"});
                cfg.getProperties().put("e", new Integer[] {1, 2, 3});
                cfg.getProperties().put("f", new int[] {1, 2, 3});
            } else {
                cfg.getProperties().put("d", new ArrayList<>(Arrays.asList("a", "b", "c")));
                cfg.getProperties().put("e", new ArrayList<>(Arrays.asList(1, 2, 3)));
                cfg.getProperties().put("f", new String[] {"1", "2", "3"});
            }
            f.getConfigurations().add(cfg);

            final ConfigurationApi api = new ConfigurationApi();
            api.setMode(i == 0 ? Mode.DEFINITIVE : Mode.SILENT_DEFINITIVE);
            final ConfigurationDescription desc = new ConfigurationDescription();
            final PropertyDescription pda = new PropertyDescription();
            pda.setType(PropertyType.INTEGER);
            final PropertyDescription pdb = new PropertyDescription();
            pdb.setType(PropertyType.INTEGER);
            pdb.setRange(new Range());
            pdb.getRange().setMin(2);
            final PropertyDescription pdc = new PropertyDescription();
            pdc.setType(PropertyType.INTEGER);
            pdc.setRange(new Range());
            pdc.getRange().setMin(2);
            pdc.setDefaultValue(4);
            final PropertyDescription pdd = new PropertyDescription();
            pdd.setIncludes(new String[] {"a", "d"});
            pdd.setExcludes(new String[] {"b", "e"});
            final PropertyDescription pde = new PropertyDescription();
            pde.setType(PropertyType.INTEGER);
            pde.setIncludes(new String[] {"1", "4"});
            pde.setExcludes(new String[] {"2", "5"});
            final PropertyDescription pdf = new PropertyDescription();
            pdf.setIncludes(new String[] {"1", "4"});
            pdf.setExcludes(new String[] {"2", "5"});
            if (i == 0) {
                pdf.setType(PropertyType.INTEGER);
            } else {
                pdf.setDefaultValue(new String[] {"1", "4"});
            }
            desc.getPropertyDescriptions().put("a", pda);
            desc.getPropertyDescriptions().put("b", pdb);
            desc.getPropertyDescriptions().put("c", pdc);
            desc.getPropertyDescriptions().put("d", pdd);
            desc.getPropertyDescriptions().put("e", pde);
            desc.getPropertyDescriptions().put("f", pdf);
            api.getConfigurationDescriptions().put("org.apache.sling", desc);

            final FeatureValidationResult result = this.validator.validate(f, api);
            assertTrue(result.isValid());

            // values have not changed
            assertEquals(1, cfg.getConfigurationProperties().get("a"));
            assertEquals(1, cfg.getConfigurationProperties().get("b"));
            assertEquals(1, cfg.getConfigurationProperties().get("c"));
            if (i == 0) {
                assertArrayEquals(new String[] {"a", "b", "c"}, (String[])
                        cfg.getConfigurationProperties().get("d"));
                assertArrayEquals(new Integer[] {1, 2, 3}, (Integer[])
                        cfg.getConfigurationProperties().get("e"));
                assertArrayEquals(new int[] {1, 2, 3}, (int[])
                        cfg.getConfigurationProperties().get("f"));
            } else {
                assertEquals(
                        Arrays.asList("a", "b", "c"),
                        cfg.getConfigurationProperties().get("d"));
                assertEquals(
                        Arrays.asList(1, 2, 3), cfg.getConfigurationProperties().get("e"));
                assertArrayEquals(new String[] {"1", "2", "3"}, (String[])
                        cfg.getConfigurationProperties().get("f"));
            }

            // apply changes
            this.validator.applyDefaultValues(f, result);
            assertEquals(1, cfg.getConfigurationProperties().get("a"));
            assertNull(cfg.getConfigurationProperties().get("b"));
            assertEquals(4, cfg.getConfigurationProperties().get("c"));
            if (i == 0) {
                assertArrayEquals(new String[] {"d", "a", "c"}, (String[])
                        cfg.getConfigurationProperties().get("d"));
                assertArrayEquals(new Integer[] {4, 1, 3}, (Integer[])
                        cfg.getConfigurationProperties().get("e"));
                assertArrayEquals(new int[] {4, 1, 3}, (int[])
                        cfg.getConfigurationProperties().get("f"));
            } else {
                assertEquals(
                        Arrays.asList("d", "a", "c"),
                        cfg.getConfigurationProperties().get("d"));
                assertEquals(
                        Arrays.asList(4, 1, 3), cfg.getConfigurationProperties().get("e"));
                assertArrayEquals(new String[] {"1", "4"}, (String[])
                        cfg.getConfigurationProperties().get("f"));
            }
        }
    }

    @Test
    public void testDefinitiveModeForFrameworkProperties() {
        for (int i = 0; i < 2; i++) {
            final Feature f = new Feature(ArtifactId.parse("g:a:1"));
            f.getFrameworkProperties().put("a", "hello");
            f.getFrameworkProperties().put("b", "world");
            f.getFrameworkProperties().put("c", "world");

            final ConfigurationApi api = new ConfigurationApi();
            api.setMode(i == 0 ? Mode.DEFINITIVE : Mode.SILENT_DEFINITIVE);
            final FrameworkPropertyDescription pda = new FrameworkPropertyDescription();
            final FrameworkPropertyDescription pdb = new FrameworkPropertyDescription();
            pdb.setRegex("h(.*)");
            final FrameworkPropertyDescription pdc = new FrameworkPropertyDescription();
            pdc.setRegex("h(.*)");
            pdc.setDefaultValue("hi");
            api.getFrameworkPropertyDescriptions().put("a", pda);
            api.getFrameworkPropertyDescriptions().put("b", pdb);
            api.getFrameworkPropertyDescriptions().put("c", pdc);

            final FeatureValidationResult result = this.validator.validate(f, api);
            assertTrue(result.isValid());

            // values have not changed
            assertEquals("hello", f.getFrameworkProperties().get("a"));
            assertEquals("world", f.getFrameworkProperties().get("b"));
            assertEquals("world", f.getFrameworkProperties().get("c"));

            // apply changes
            this.validator.applyDefaultValues(f, result);
            assertEquals("hello", f.getFrameworkProperties().get("a"));
            assertNull(f.getFrameworkProperties().get("b"));
            assertEquals("hi", f.getFrameworkProperties().get("c"));
        }
    }

    @Test
    public void testInternalConfigurationNoPropertyDescriptions() {
        Feature f1 = createFeature("g:a:1");
        ConfigurationApi api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.INTERNAL);

        // internal -> valid
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> invalid
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.GLOBAL);
        result = validator.validate(f1, api);
        assertFalse(result.isValid());

        // global -> invalid, but mode DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertNull(f1.getConfigurations().getConfiguration(PID));

        // global -> invalid, but mode LENIENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.LENIENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> invalid, but mode SILENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> invalid, but mode SILENT_DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        assertTrue(validator.applyDefaultValues(f1, result));
        assertNull(f1.getConfigurations().getConfiguration(PID));
    }

    @Test
    public void testInternalFactoryConfigurationNoPropertyDescriptions() {
        Feature f1 = createFeature("g:a:1");
        ConfigurationApi api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.INTERNAL);

        // internal -> valid
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> invalid
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.GLOBAL);
        result = validator.validate(f1, api);
        assertFalse(result.isValid());

        // global -> invalid, but mode DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertNull(f1.getConfigurations().getConfiguration(FACTORY_PID.concat("~print")));

        // global -> invalid, but mode LENIENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.LENIENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> invalid, but mode SILENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> invalid, but mode SILENT_DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertNull(f1.getConfigurations().getConfiguration(FACTORY_PID.concat("~print")));
    }

    @Test
    public void testInternalConfigurationNoPropertyDescriptionsButAllowAdditional() {
        Feature f1 = createFeature("g:a:1");
        ConfigurationApi api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.INTERNAL);

        // internal -> valid
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> valid
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode LENIENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.LENIENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode SILENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode SILENT_DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getConfigurationDescriptions().get(PID).getPropertyDescriptions().clear();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(PID)
                        .getConfigurationProperties()
                        .size());
    }

    @Test
    public void testInternalFactoryConfigurationNoPropertyDescriptionsButAllowAdditional() {
        Feature f1 = createFeature("g:a:1");
        ConfigurationApi api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.INTERNAL);

        // internal -> valid
        FeatureValidationResult result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> invalid
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> invalid, but mode LENIENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.LENIENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode SILENT
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());

        // global -> valid, but mode SILENT_DEFINITIVE
        f1 = createFeature("g:a:1");
        api = createApi();
        // no property descriptions -> internal
        api.getFactoryConfigurationDescriptions()
                .get(FACTORY_PID)
                .getPropertyDescriptions()
                .clear();
        api.getFactoryConfigurationDescriptions().get(FACTORY_PID).setAllowAdditionalProperties(true);
        api.setRegion(Region.GLOBAL);
        api.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(f1, api);
        assertTrue(result.isValid());
        validator.applyDefaultValues(f1, result);
        assertEquals(
                1,
                f1.getConfigurations()
                        .getConfiguration(FACTORY_PID.concat("~print"))
                        .getConfigurationProperties()
                        .size());
    }

    @Test
    public void testLiveValidation() {
        assertFalse(validator.isLiveValues());

        final Feature feature = createFeature("g:a:1");
        final ConfigurationApi api = createApi();
        // make property a password requiring a placeholder
        api.getConfigurationDescriptions()
                .get(PID)
                .getPropertyDescriptions()
                .get("prop")
                .setType(PropertyType.PASSWORD);

        // validate non live values - this should value as no secret is used for the password
        FeatureValidationResult result = validator.validate(feature, api);
        assertFalse(result.isValid());

        try {
            validator.setLiveValues(true);

            result = validator.validate(feature, api);
            assertTrue(result.isValid());

        } finally {
            validator.setLiveValues(false);
        }
    }

    @Test
    public void testInternalPropertyNames() {
        final Feature feature = createFeature("g:a:1");
        feature.getConfigurations().getConfiguration(PID).getProperties().put("xyz", true);

        final ConfigurationApi api = createApi();
        api.getConfigurationDescriptions().get(PID).getInternalPropertyNames().add("xyz");

        // validate global region
        FeatureValidationResult result = validator.validate(feature, api);
        assertFalse(result.isValid());

        // validate internal region
        api.setRegion(Region.INTERNAL);
        result = validator.validate(feature, api);
        assertTrue(result.isValid());
    }

    @Test
    public void testAllowAdditional() {
        final Feature feature = createFeature("g:a:1");
        feature.getConfigurations().getConfiguration(PID).getProperties().put("xyz", true);

        final ConfigurationApi api = createApi();
        api.getConfigurationDescriptions().get(PID).setAllowAdditionalProperties(true);

        // validate global region
        FeatureValidationResult result = validator.validate(feature, api);
        assertTrue(result.isValid());

        // validate internal region
        api.setRegion(Region.INTERNAL);
        result = validator.validate(feature, api);
        assertTrue(result.isValid());
    }
}
