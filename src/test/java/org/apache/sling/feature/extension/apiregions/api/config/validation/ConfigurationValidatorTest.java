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

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.FactoryConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;
import org.apache.sling.feature.extension.apiregions.api.config.Region;
import org.junit.Test;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationValidatorTest {

    private final ConfigurationValidator validator = new ConfigurationValidator();

    @Test
    public void testWrongDescriptionTypeForConfiguration() {
        final Configuration cfg = new Configuration("org.apache");
        final FactoryConfigurationDescription fcd = new FactoryConfigurationDescription();

        final ConfigurationValidationResult result = validator.validate(cfg, fcd, null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void testWrongDescriptionTypeForFactoryConfiguration() {
        final Configuration cfg = new Configuration("org.apache~foo");
        final ConfigurationDescription fcd = new ConfigurationDescription();

        final ConfigurationValidationResult result = validator.validate(cfg, fcd, null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    public void testDeprecated() {
        final Configuration cfg = new Configuration("org.apache");
        final ConfigurationDescription cd = new ConfigurationDescription();
        final PropertyDescription prop = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", prop);

        ConfigurationValidationResult result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());

        cd.setDeprecated("this is deprecated");
        result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());
        assertFalse(result.getWarnings().isEmpty());
        assertEquals("this is deprecated", result.getWarnings().get(0));
    }

    @Test
    public void testMessageWithEnforceAndSinceInfo() {
        final Configuration cfg = new Configuration("org.apache");
        final ConfigurationDescription cd = new ConfigurationDescription();
        final PropertyDescription prop = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", prop);

        ConfigurationValidationResult result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());

        cd.setDeprecated("this is deprecated");
        cd.setSince("1970-01-01");
        cd.setEnforceOn("1970-04-01");
        result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());
        assertFalse(result.getWarnings().isEmpty());
        assertEquals(
                "this is deprecated. Since : 1970-01-01. Enforced on : 1970-04-01",
                result.getWarnings().get(0));
    }

    @Test
    public void testServiceRanking() {
        final Configuration cfg = new Configuration("org.apache");
        final ConfigurationDescription cd = new ConfigurationDescription();
        cfg.getProperties().put(Constants.SERVICE_RANKING, 5);
        final PropertyDescription prop = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", prop);

        ConfigurationValidationResult result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());

        cfg.getProperties().put(Constants.SERVICE_RANKING, "5");
        result = validator.validate(cfg, cd, null);
        assertFalse(result.isValid());
    }

    @Test
    public void testAllowedProperties() {
        final Configuration cfg = new Configuration("org.apache");
        final ConfigurationDescription cd = new ConfigurationDescription();
        final PropertyDescription prop = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", prop);
        cfg.getProperties().put(Constants.SERVICE_DESCRIPTION, "desc");
        cfg.getProperties().put(Constants.SERVICE_VENDOR, "vendor");

        ConfigurationValidationResult result = validator.validate(cfg, cd, null);
        assertTrue(result.isValid());
    }

    @Test
    public void testAdditionalProperties() {
        final Configuration cfg = new Configuration("org.apache");
        cfg.getProperties().put("a", "desc");

        final ConfigurationDescription cd = new ConfigurationDescription();
        final PropertyDescription prop = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", prop);

        ConfigurationValidationResult result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertEquals(1, result.getPropertyResults().size());
        assertTrue(result.getPropertyResults().get("a").isValid());

        cfg.getProperties().put("b", "vendor");
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertFalse(result.isValid());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getPropertyResults().get("a").isValid());
        assertFalse(result.getPropertyResults().get("b").isValid());

        // allowed if internal
        result = validator.validate(cfg, cd, Region.INTERNAL);
        assertTrue(result.isValid());
        assertEquals(2, result.getPropertyResults().size());

        // allowed if lenient
        cd.setMode(Mode.LENIENT);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertEquals(2, result.getPropertyResults().size());
    }

    @Test
    public void testInvalidProperty() {
        final Configuration cfg = new Configuration("org.apache");
        cfg.getProperties().put("a", "desc");
        cfg.getProperties().put("b", "vendor");

        final ConfigurationDescription cd = new ConfigurationDescription();
        final PropertyDescription propA = new PropertyDescription();
        cd.getPropertyDescriptions().put("a", propA);
        final PropertyDescription propB = new PropertyDescription();
        propB.setType(PropertyType.INTEGER);
        cd.getPropertyDescriptions().put("b", propB);

        ConfigurationValidationResult result = validator.validate(cfg, cd, null);
        assertFalse(result.isValid());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getPropertyResults().get("a").isValid());
        assertFalse(result.getPropertyResults().get("b").isValid());
    }

    @Test
    public void testInternalConfiguration() {
        // internal -> valid
        final Configuration cfg = new Configuration("org.apache");
        cfg.getProperties().put("a", "desc");
        cfg.getProperties().put("b", "vendor");

        // description without properties -> internal
        final ConfigurationDescription cd = new ConfigurationDescription();

        ConfigurationValidationResult result = validator.validate(cfg, cd, Region.INTERNAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(2, cfg.getProperties().size());

        // global -> invalid
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertFalse(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getWarnings().isEmpty());

        // global -> invalid, but mode DEFINITIVE
        cd.setMode(Mode.DEFINITIVE);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertTrue(result.isUseDefaultValue());
        assertEquals(1, result.getWarnings().size());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode LENIENT
        cd.setMode(Mode.LENIENT);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode SILENT
        cd.setMode(Mode.SILENT);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode SILENT
        cd.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertTrue(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testInternalFactoryConfiguration() {
        // internal -> valid
        final Configuration cfg = new Configuration("org.apache~foo");
        cfg.getProperties().put("a", "desc");
        cfg.getProperties().put("b", "vendor");

        // description without properties -> internal
        final FactoryConfigurationDescription cd = new FactoryConfigurationDescription();

        ConfigurationValidationResult result = validator.validate(cfg, cd, Region.INTERNAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(2, cfg.getProperties().size());

        // global -> invalid
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertFalse(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getWarnings().isEmpty());

        // global -> invalid, but mode DEFINITIVE
        cd.setMode(Mode.DEFINITIVE);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertTrue(result.isUseDefaultValue());
        assertEquals(1, result.getWarnings().size());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode LENIENT
        cd.setMode(Mode.LENIENT);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode SILENT
        cd.setMode(Mode.SILENT);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertFalse(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getPropertyResults().isEmpty());
        assertTrue(result.getErrors().isEmpty());

        // global -> invalid, but mode SILENT
        cd.setMode(Mode.SILENT_DEFINITIVE);
        result = validator.validate(cfg, cd, Region.GLOBAL);
        assertTrue(result.isValid());
        assertTrue(result.isUseDefaultValue());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(2, result.getPropertyResults().size());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testLiveValidation() {
        assertFalse(this.validator.isLiveValues());
    }
}
