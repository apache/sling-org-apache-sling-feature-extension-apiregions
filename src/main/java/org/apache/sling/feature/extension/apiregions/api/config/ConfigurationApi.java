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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;

/**
 * A configuration api describes the set of supported OSGi
 * configurations and framework properties. This object can be
 * stored as an extension inside a feature model.
 * This class is not thread safe.
 */
public class ConfigurationApi extends AttributeableEntity {

    /** The name of the api regions extension. */
    public static final String EXTENSION_NAME = "configuration-api";

    /**
     * Get the configuration api from the feature - if it exists.
     * If the configuration api is updated, the containing feature is left untouched.
     * {@link #setConfigurationApi(Feature, ConfigurationApi)} can be used to update
     * the feature.
     * @param feature The feature
     * @return The configuration api or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ConfigurationApi getConfigurationApi(final Feature feature) {
        final Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getConfigurationApi(ext);
    }

    /**
     * Get the configuration api from the extension.
     * If the configuration api is updated, the containing extension is left untouched.
     * @param ext The extension
     * @return The configuration api or {@code null} if the extension is {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ConfigurationApi getConfigurationApi(final Extension ext) {
        if ( ext == null ) {
            return null;
        }
        if ( ext.getType() != ExtensionType.JSON ) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        }
        try {
            final ConfigurationApi result = new ConfigurationApi();
            result.fromJSONObject(ext.getJSONStructure().asJsonObject());
            return result;
        } catch ( final IOException ioe) {
            throw new IllegalArgumentException(ioe.getMessage(), ioe);
        }
    }

    /**
     * Set the configuration api as an extension to the feature
     * @param feature The feature
     * @param api The configuration api
     * @throws IllegalStateException If the feature has already an extension of a wrong type
     * @throws IllegalArgumentException If the api configuration can't be serialized to JSON
     */
    public static void setConfigurationApi(final Feature feature, final ConfigurationApi api) {
        Extension ext = feature.getExtensions().getByName(EXTENSION_NAME);
        if ( api == null ) {
            if ( ext != null ) {
                feature.getExtensions().remove(ext);
            }
        } else {
            if ( ext == null ) {
                ext = new Extension(ExtensionType.JSON, EXTENSION_NAME, ExtensionState.OPTIONAL);
                feature.getExtensions().add(ext);
            }
            try {
                ext.setJSONStructure(api.toJSONObject());
            } catch ( final IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }
    }

    /** The map of configurations */
 	private final Map<String, ConfigurationDescription> configurations = new LinkedHashMap<>();

    /** The map of factory configurations */
    private final Map<String, FactoryConfigurationDescription> factories = new LinkedHashMap<>();

    /** The map of configuration additions @since 1.8 */
    private final Map<String, ConfigurationDescriptionAddition> configurationAdditions = new LinkedHashMap<>();

    /** The map of factory configuration additions @since 1.8 */
    private final Map<String, FactoryConfigurationDescriptionAddition> factoryAdditions = new LinkedHashMap<>();

    /** The map of framework properties */
    private final Map<String, FrameworkPropertyDescription> frameworkProperties = new LinkedHashMap<>();

    /** The set of internal configuration names */
    private final Set<String> internalConfigurations = new TreeSet<>();

    /** The set of internal factory configuration names */
    private final Set<String> internalFactories = new TreeSet<>();

    /** The set of internal framework property names */
    private final Set<String> internalFrameworkProperties = new TreeSet<>();

    /** The configuration region of this feature */
    private Region region;

    /** The cached region information for feature origins */
    private final Map<ArtifactId, Region> regionCache = new LinkedHashMap<>();

    /**
     * The default validation mode.
     * @since 1.2
     */
    private Mode mode;

    public ConfigurationApi() {
        this.setDefaults();
    }

    @Override
    protected void setDefaults() {
        super.setDefaults();
        this.setMode(Mode.STRICT);
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.configurations.clear();
        this.factories.clear();
        this.frameworkProperties.clear();
        this.internalConfigurations.clear();
        this.internalFactories.clear();
        this.internalFrameworkProperties.clear();
        this.setRegion(null);
        this.getFeatureToRegionCache().clear();
        this.getConfigurationDescriptionAdditions().clear();
        this.getFactoryConfigurationDescriptionAdditions().clear();
    }

	/**
	 * Extract the metadata from the JSON object.
	 * This method first calls {@link #clear()}.
     *
	 * @param jsonObj The JSON Object
	 * @throws IOException If JSON parsing fails
	 */
    @Override
    public void fromJSONObject(final JsonObject jsonObj) throws IOException {
        super.fromJSONObject(jsonObj);
        try {
			final String typeVal = this.getString(InternalConstants.KEY_REGION);
			if ( typeVal != null ) {
                this.setRegion(Region.valueOf(typeVal.toUpperCase()));
			}

            JsonValue val;
            val = this.getAttributes().remove(InternalConstants.KEY_CONFIGURATIONS);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final ConfigurationDescription cfg = new ConfigurationDescription();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getConfigurationDescriptions().put(innerEntry.getKey(), cfg);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_FACTORIES);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final FactoryConfigurationDescription cfg = new FactoryConfigurationDescription();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getFactoryConfigurationDescriptions().put(innerEntry.getKey(), cfg);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_FWK_PROPERTIES);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final FrameworkPropertyDescription cfg = new FrameworkPropertyDescription();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getFrameworkPropertyDescriptions().put(innerEntry.getKey(), cfg);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_CONFIGURATIONS);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    this.getInternalConfigurations().add(getString(innerVal));
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_FACTORIES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    this.getInternalFactoryConfigurations().add(getString(innerVal));
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_FWK_PROPERTIES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    this.getInternalFrameworkProperties().add(getString(innerVal));
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_REGION_CACHE);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    this.getFeatureToRegionCache().put(ArtifactId.parse(innerEntry.getKey()),
                        Region.valueOf(getString(innerEntry.getValue()).toUpperCase()));
                }
            }

			final String modeVal = this.getString(InternalConstants.KEY_MODE);
			if ( modeVal != null ) {
                this.setMode(Mode.valueOf(modeVal.toUpperCase()));
			}

            val = this.getAttributes().remove(InternalConstants.KEY_CONFIGURATION_ADDITIONS);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final ConfigurationDescriptionAddition cfg = new ConfigurationDescriptionAddition();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getConfigurationDescriptionAdditions().put(innerEntry.getKey(), cfg);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_FACTORY_ADDITIONS);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final FactoryConfigurationDescriptionAddition cfg = new FactoryConfigurationDescriptionAddition();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getFactoryConfigurationDescriptionAdditions().put(innerEntry.getKey(), cfg);
                }
            }
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the configuration descriptions
	 * @return Mutable map of configuration descriptions by pid
	 */
	public Map<String, ConfigurationDescription> getConfigurationDescriptions() {
		return configurations;
	}

	/**
     * Get the factory configuration descriptions
	 * @return Mutable map of factory descriptions by factory pid
	 */
	public Map<String, FactoryConfigurationDescription> getFactoryConfigurationDescriptions() {
		return factories;
	}

	/**
     * Get the framework properties
	 * @return Mutable map of framework properties
	 */
	public Map<String, FrameworkPropertyDescription> getFrameworkPropertyDescriptions() {
		return frameworkProperties;
	}

	/**
     * Get the internal configuration pids
	 * @return Mutable set of internal configuration pids
     * @deprecated Please use empty configuration descriptions via {@link #getConfigurationDescriptions()}
	 */
	@Deprecated
    public Set<String> getInternalConfigurations() {
		return internalConfigurations;
	}

	/**
     * Get the internal factory pids
	 * @return Mutable set of internal factory configuration pids
     * @deprecated Please use empty factory configuration descriptions via {@link #getFactoryConfigurationDescriptions()}
	 */
	@Deprecated
    public Set<String> getInternalFactoryConfigurations() {
		return internalFactories;
	}

    /**
     * Check if the configuration is an internal configuration
     * @param pid The pid
     * @return {@code true} if it is an internal configuration
     * @since 1.7.0
     */
    public boolean isInternalConfiguration(final String pid) {
        boolean result = this.internalConfigurations.contains(pid);
        if ( !result ) {
            final ConfigurationDescription desc = this.configurations.get(pid);
            if ( desc != null ) {
                result = desc.getPropertyDescriptions().isEmpty();
            }
        }
        return result;
    }

    /**
     * Check if the factory configuration is an internal configuration
     * @param factoryPid The factory pid
     * @param name Optional name of the configuration
     * @return {@code true} if it is an internal factory configuration
     * @since 1.7.0
     */
    public boolean isInternalFactoryConfiguration(final String factoryPid, final String name) {
        boolean result = this.internalFactories.contains(factoryPid);
        if ( !result ) {
            final FactoryConfigurationDescription desc = this.factories.get(factoryPid);
            if ( desc != null ) {
                result = desc.getPropertyDescriptions().isEmpty();
                if ( !result && name != null ) {
                    result = desc.getInternalNames().contains(name);
                }
            }
        }
        return result;
    }

    /**
     * Get the internal framework property names
	 * @return Mutable set of internal framework property names
	 */
	public Set<String> getInternalFrameworkProperties() {
		return internalFrameworkProperties;
    }

    /**
     * Get the api configuration region
     * @return The region or {@code null}
     */
    public Region getRegion() {
        return this.region;
    }

    /**
     * Set the api configuration region
     * @param value The region to set
     */
    public void setRegion(final Region value) {
        this.region = value;
    }

    /**
     * Detect the region, either return the stored region or the default (GLOBAL)
     * @return The region
     * @since 1.1
     */
    public Region detectRegion() {
        if ( this.getRegion() != null ) {
            return this.getRegion();
        }
        return Region.GLOBAL;
    }

    /**
     * Get the feature to region cache to keep track of regions for origin features
     * @return The cache
     * @since 1.1.
     */
    public Map<ArtifactId, Region> getFeatureToRegionCache() {
        return this.regionCache;
    }

    /**
     * Get the validation mode.
     * The default is {@link Mode#STRICT}
     * @return The mode
     * @since 1.2
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Set the validation mode
     * @param value The validation mode
     * @since 1.2
     */
    public void setMode(final Mode value) {
        this.mode = value == null ? Mode.STRICT : value;
    }

    /**
     * Convert this object into JSON
     *
     * @return The json object builder
     * @throws IOException If generating the JSON fails
     */
    @Override
    protected JsonObjectBuilder createJson() throws IOException {
		final JsonObjectBuilder objBuilder = super.createJson();
        if ( this.getRegion() != null ) {
            objBuilder.add(InternalConstants.KEY_REGION, this.getRegion().name());
        }
        if ( !this.getConfigurationDescriptions().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, ConfigurationDescription> entry : this.getConfigurationDescriptions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_CONFIGURATIONS, propBuilder);
        }
        if ( !this.getFactoryConfigurationDescriptions().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, FactoryConfigurationDescription> entry : this.getFactoryConfigurationDescriptions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_FACTORIES, propBuilder);
        }
        if ( !this.getFrameworkPropertyDescriptions().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, FrameworkPropertyDescription> entry : this.getFrameworkPropertyDescriptions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_FWK_PROPERTIES, propBuilder);
        }
        if ( !this.getInternalConfigurations().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final String n : this.getInternalConfigurations()) {
                arrayBuilder.add(n);
            }
			objBuilder.add(InternalConstants.KEY_INTERNAL_CONFIGURATIONS, arrayBuilder);
		}
		if ( !this.getInternalFactoryConfigurations().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final String n : this.getInternalFactoryConfigurations()) {
                arrayBuilder.add(n);
            }
			objBuilder.add(InternalConstants.KEY_INTERNAL_FACTORIES, arrayBuilder);
		}
		if ( !this.getInternalFrameworkProperties().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final String n : this.getInternalFrameworkProperties()) {
                arrayBuilder.add(n);
            }
			objBuilder.add(InternalConstants.KEY_INTERNAL_FWK_PROPERTIES, arrayBuilder);
        }
        if ( !this.getFeatureToRegionCache().isEmpty()) {
            final JsonObjectBuilder cacheBuilder = Json.createObjectBuilder();
            for(final Map.Entry<ArtifactId, Region> entry : this.getFeatureToRegionCache().entrySet()) {
                cacheBuilder.add(entry.getKey().toMvnId(), entry.getValue().name());
            }
            objBuilder.add(InternalConstants.KEY_REGION_CACHE, cacheBuilder);
        }
        if ( this.getMode() != Mode.STRICT ) {
            objBuilder.add(InternalConstants.KEY_MODE, this.getMode().name());
        }
        if ( !this.getConfigurationDescriptionAdditions().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, ConfigurationDescriptionAddition> entry : this.getConfigurationDescriptionAdditions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_CONFIGURATION_ADDITIONS, propBuilder);
        }
        if ( !this.getFactoryConfigurationDescriptionAdditions().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, FactoryConfigurationDescriptionAddition> entry : this.getFactoryConfigurationDescriptionAdditions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_FACTORY_ADDITIONS, propBuilder);
        }

		return objBuilder;
    }

    /**
     * Get the map of configuration description additions
     * @return The map, might be empty
     * @since 1.8
     */
    public Map<String, ConfigurationDescriptionAddition> getConfigurationDescriptionAdditions() {
        return this.configurationAdditions;
    }

    /**
     * Get the map of factory configuration description additions
     * @return The map, might be empty
     * @since 1.8
     */
    public Map<String, FactoryConfigurationDescriptionAddition> getFactoryConfigurationDescriptionAdditions() {
        return this.factoryAdditions;
    }
}
