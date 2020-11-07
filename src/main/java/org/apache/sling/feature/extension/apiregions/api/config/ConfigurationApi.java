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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;

/**
 * A configuration api describes the set of supported OSGi
 * configurations and framework properties. This object can be
 * stored as an extension inside a feature model.
 */
public class ConfigurationApi extends AttributeableEntity {
    
    /** The name of the api regions extension. */
    public static final String EXTENSION_NAME = "configuration-api";
  
    /**
     * Get the configuration api from the feature - if it exists.
     * 
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
     * 
     * @param ext The extension
     * @return The configuration api or {@code null}.
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
   
    /** The map of configurations */
 	private final Map<String, Configuration> configurations = new LinkedHashMap<>();

    /** The map of factory configurations */
    private final Map<String, FactoryConfiguration> factories = new LinkedHashMap<>();

    /** The map of framework properties */
    private final Map<String, FrameworkProperty> frameworkProperties = new LinkedHashMap<>();

    /** The list of internal configuration names */
    private final List<String> internalConfigurations = new ArrayList<>();

    /** The list of internal factory configuration names */
    private final List<String> internalFactories = new ArrayList<>();

    /** The list of internal framework property names */
    private final List<String> internalFrameworkProperties = new ArrayList<>();
    
    /**
     * Clear the object and remove all metadata
     */
    public void clear() {
        super.clear();
        this.configurations.clear();
        this.factories.clear();
        this.frameworkProperties.clear();
        this.internalConfigurations.clear();
        this.internalFactories.clear();
        this.internalFrameworkProperties.clear();
    }

	/**
	 * Extract the metadata from the JSON object.
	 * This method first calls {@link #clear()}
	 * @param jsonObj The JSON Object
	 * @throws IOException If JSON parsing fails
	 */
    public void fromJSONObject(final JsonObject jsonObj) throws IOException {
        super.fromJSONObject(jsonObj);
        try {
            JsonValue val;
            val = this.getAttributes().remove(InternalConstants.KEY_CONFIGURATIONS);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final Configuration cfg = new Configuration();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getConfigurations().put(innerEntry.getKey(), cfg);
                }
            }
            
            val = this.getAttributes().remove(InternalConstants.KEY_FACTORIES);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final FactoryConfiguration cfg = new FactoryConfiguration();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getFactories().put(innerEntry.getKey(), cfg);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_FWK_PROPERTIES);
            if ( val != null ) {
                for(final Map.Entry<String, JsonValue> innerEntry : val.asJsonObject().entrySet()) {
                    final FrameworkProperty cfg = new FrameworkProperty();
                    cfg.fromJSONObject(innerEntry.getValue().asJsonObject());
                    this.getFrameworkProperties().put(innerEntry.getKey(), cfg);
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
                    this.getInternalFactories().add(getString(innerVal));
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_FWK_PROPERTIES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    this.getInternalFrameworkProperties().add(getString(innerVal));
                }
            }

        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the configurations
	 * @return the configurations
	 */
	public Map<String, Configuration> getConfigurations() {
		return configurations;
	}

	/**
     * Get the factory configurations
	 * @return the factories
	 */
	public Map<String, FactoryConfiguration> getFactories() {
		return factories;
	}

	/**
     * Get the framework properties
	 * @return the frameworkProperties
	 */
	public Map<String, FrameworkProperty> getFrameworkProperties() {
		return frameworkProperties;
	}

	/**
     * Get the internal configuration names
	 * @return the internalConfigurations
	 */
	public List<String> getInternalConfigurations() {
		return internalConfigurations;
	}

	/**
     * Get the internal factory names
	 * @return the internalFactories
	 */
	public List<String> getInternalFactories() {
		return internalFactories;
	}

	/**
     * Get the internal framework property names
	 * @return the internalFrameworkProperties
	 */
	public List<String> getInternalFrameworkProperties() {
		return internalFrameworkProperties;
    }

    /**
     * Convert this object into JSON
     *
     * @return The json object builder
     * @throws IOException If generating the JSON fails
     */
    JsonObjectBuilder createJson() throws IOException {
		final JsonObjectBuilder objBuilder = super.createJson();
        if ( !this.getConfigurations().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, Configuration> entry : this.getConfigurations().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_CONFIGURATIONS, propBuilder);
        }
        if ( !this.getFactories().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, FactoryConfiguration> entry : this.getFactories().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_FACTORIES, propBuilder);
        }
        if ( !this.getFrameworkProperties().isEmpty() ) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for(final Map.Entry<String, FrameworkProperty> entry : this.getFrameworkProperties().entrySet()) {
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
		if ( !this.getInternalFactories().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final String n : this.getInternalFactories()) {
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

		return objBuilder;
    }
}