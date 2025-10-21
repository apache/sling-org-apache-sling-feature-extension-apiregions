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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurableEntity;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.DescribableEntity;
import org.apache.sling.feature.extension.apiregions.api.config.FactoryConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.Region;
import org.osgi.framework.Constants;

/**
 * Validator to validate a configuration or factory configuration
 */
public class ConfigurationValidator {

    /**
     * List of properties which are always allowed
     */
    public static final List<String> ALLOWED_PROPERTIES =
            Arrays.asList(Constants.SERVICE_DESCRIPTION, Constants.SERVICE_VENDOR, Constants.SERVICE_RANKING);

    private final PropertyValidator propertyValidator = new PropertyValidator();

    private boolean liveValues = false;

    private Map<ArtifactId, Region> cache = new HashMap<>();

    /**
     * Are live values validated?
     * @return {@code true} if live values are validated
     * @since 1.4
     */
    public boolean isLiveValues() {
        return liveValues;
    }

    /**
     * Set whether live values are validated.
     * @param value Flag for validating live values
     * @since 1.4
     */
    public void setLiveValues(final boolean value) {
        this.liveValues = value;
        this.propertyValidator.setLiveValues(value);
    }

    /**
     * Validate a configuration
     *
     * @param config The OSGi configuration
     * @param desc The configuration description
     * @param region The optional region for the configuration
     * @return The result
     */
    public ConfigurationValidationResult validate(
            final Configuration config, final ConfigurableEntity desc, final Region region) {
        return this.validate(config, desc, region, Mode.STRICT);
    }

    /**
     * Validate a configuration
     *
     * @param config The OSGi configuration
     * @param desc The configuration description
     * @param region The optional region for the configuration
     * @param mode The optional validation mode. This is used if the configuration/property has no mode is set. Defaults to {@link Mode#STRICT}.
     * @return The result
     * @since 1.2
     */
    public ConfigurationValidationResult validate(
            final Configuration config, final ConfigurableEntity desc, final Region region, final Mode mode) {
        final Mode validationMode = desc.getMode() != null ? desc.getMode() : (mode != null ? mode : Mode.STRICT);

        final ConfigurationValidationResult result = new ConfigurationValidationResult();
        if (config.isFactoryConfiguration()) {
            if (!(desc instanceof FactoryConfigurationDescription)) {
                result.getErrors()
                        .add("Factory configuration cannot be validated against non factory configuration description");
            } else {
                if (desc.getPropertyDescriptions().isEmpty()) {
                    if (region == Region.GLOBAL && !desc.isAllowAdditionalProperties()) {
                        setResult(result, validationMode, desc, "Factory configuration is not allowed");
                        markGlobalProperties(config, result, region);
                    }
                } else {
                    if (region == Region.GLOBAL && desc.getRegion() == Region.INTERNAL) {
                        setResult(result, validationMode, desc, "Factory configuration is not allowed");
                        markGlobalProperties(config, result, region);
                    } else {
                        // check for internal mode
                        final FactoryConfigurationDescription fcd = (FactoryConfigurationDescription) desc;
                        if (fcd.getInternalMode() != null
                                && fcd.getInternalNames().contains(config.getName())) {
                            validateProperties(
                                    config, desc, result.getPropertyResults(), region, fcd.getInternalMode());
                        } else {
                            validateProperties(config, desc, result.getPropertyResults(), region, validationMode);
                        }
                    }
                }
            }
        } else {
            if (!(desc instanceof ConfigurationDescription)) {
                result.getErrors().add("Configuration cannot be validated against factory configuration description");
            } else {
                if (desc.getPropertyDescriptions().isEmpty()) {
                    if (region == Region.GLOBAL && !desc.isAllowAdditionalProperties()) {
                        setResult(result, validationMode, desc, "Configuration is not allowed");
                        markGlobalProperties(config, result, region);
                    }
                } else {
                    if (region == Region.GLOBAL && desc.getRegion() == Region.INTERNAL) {
                        setResult(result, validationMode, desc, "Configuration is not allowed");
                        markGlobalProperties(config, result, region);
                    } else {
                        validateProperties(config, desc, result.getPropertyResults(), region, validationMode);
                    }
                }
            }
        }

        if (desc.getDeprecated() != null) {
            setResult(result, Mode.LENIENT, desc, desc.getDeprecated());
        }

        return result;
    }

    /**
     * Set all global properties to use default value if mode is definitive
     * @param configuration The OSGi configuration
     * @param result The result for the configuration
     * @param region The configuration region
     */
    void markGlobalProperties(
            final Configuration configuration, final ConfigurationValidationResult result, final Region region) {
        if (result.isUseDefaultValue()) {
            final List<String> names = new ArrayList<>(
                    Collections.list(configuration.getConfigurationProperties().keys()));
            for (final String propName : names) {
                // detect the region
                final Region propRegion = FeatureValidator.getRegionInfo(region, configuration, propName, this.cache);
                if (propRegion == Region.GLOBAL) {
                    final PropertyValidationResult pvr = new PropertyValidationResult();
                    pvr.setUseDefaultValue(true);
                    result.getPropertyResults().put(propName, pvr);
                }
            }
        }
    }

    /**
     * Validate all properties
     * @param configuration The OSGi configuration
     * @param desc The configuration description
     * @param results The map of results per property
     * @param region The configuration region
     * @param mode The validation mode.
     */
    void validateProperties(
            final Configuration configuration,
            final ConfigurableEntity desc,
            final Map<String, PropertyValidationResult> results,
            final Region region,
            final Mode mode) {
        final boolean isInternalFactoryConfig = (desc instanceof FactoryConfigurationDescription)
                && ((FactoryConfigurationDescription) desc).getInternalNames().contains(configuration.getName());
        final Dictionary<String, Object> properties = configuration.getConfigurationProperties();

        // validate the described properties
        for (final Map.Entry<String, PropertyDescription> propEntry :
                desc.getPropertyDescriptions().entrySet()) {
            final Object value = properties.get(propEntry.getKey());
            final Mode propMode;
            if (isInternalFactoryConfig && propEntry.getValue().getInternalMode() != null) {
                propMode = propEntry.getValue().getInternalMode();
            } else {
                propMode = mode;
            }
            final PropertyValidationResult result = propertyValidator.validate(value, propEntry.getValue(), propMode);
            results.put(propEntry.getKey(), result);
        }

        // validate additional properties
        final Enumeration<String> keyEnum = properties.keys();
        while (keyEnum.hasMoreElements()) {
            final String propName = keyEnum.nextElement();
            if (!desc.getPropertyDescriptions().containsKey(propName)) {
                // detect the region
                final Region propRegion = FeatureValidator.getRegionInfo(region, configuration, propName, this.cache);

                final PropertyValidationResult result = new PropertyValidationResult();
                results.put(propName, result);

                if (desc.getInternalPropertyNames().contains(propName)) {
                    if (propRegion != Region.INTERNAL) {
                        PropertyValidator.setResult(result, null, mode, desc, "Property is not allowed");
                    }
                } else if (Constants.SERVICE_RANKING.equalsIgnoreCase(propName)) {
                    final Object value = properties.get(propName);
                    if (!(value instanceof Integer)) {
                        PropertyValidator.setResult(result, 0, mode, desc, "service.ranking must be of type Integer");
                    }
                } else if (!isAllowedProperty(propName)
                        && propRegion != Region.INTERNAL
                        && !desc.isAllowAdditionalProperties()) {
                    PropertyValidator.setResult(result, null, mode, desc, "Property is not allowed");
                }
            }
        }
    }

    static void setResult(
            final ConfigurationValidationResult result,
            final Mode validationMode,
            final DescribableEntity desc,
            final String msg) {
        // set postfix to the message if since or enforce-on are set
        String postfixMsg = "";
        if (desc != null && desc.getSince() != null) {
            postfixMsg = postfixMsg.concat(". Since : ").concat(desc.getSince());
        }
        if (desc != null && desc.getEnforceOn() != null) {
            postfixMsg = postfixMsg.concat(". Enforced on : ").concat(desc.getEnforceOn());
        }
        String finalMsg = msg + postfixMsg;
        if (validationMode == Mode.STRICT) {
            result.getErrors().add(finalMsg);
        } else if (validationMode == Mode.LENIENT || validationMode == Mode.DEFINITIVE) {
            result.getWarnings().add(finalMsg);
        }
        if (validationMode == Mode.DEFINITIVE || validationMode == Mode.SILENT_DEFINITIVE) {
            result.setUseDefaultValue(true);
        }
    }

    private boolean isAllowedProperty(final String name) {
        for (final String allowed : ALLOWED_PROPERTIES) {
            if (allowed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    void setCache(Map<ArtifactId, Region> cache) {
        this.cache = cache;
    }
}
