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
package org.apache.sling.feature.extension.apiregions.api.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.apache.felix.cm.json.io.Configurations;

/**
 * A configurable entity has properties
 * This class is not thread safe.
 */
public abstract class ConfigurableEntity extends DescribableEntity {

    /** The properties */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final Map<String, PropertyDescription> properties = (Map) Configurations.newConfiguration();

    /**
     * The validation mode.
     * @since 1.2
     */
    private Mode mode;

    /**
     * Allow additional properties
     * @since 1.4
     */
    private boolean allowAdditional;

    /**
     * The region
     * @since 1.4
     */
    private Region region;

    /**
     *
     * @since 1.4
     */
    private final List<String> internalProperties = new ArrayList<>();

    protected void setDefaults() {
        super.setDefaults();
        this.setAllowAdditionalProperties(false);
        this.setRegion(Region.GLOBAL);
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.properties.clear();
        this.setMode(null);
        this.getInternalPropertyNames().clear();
    }

    /**
     * Extract the metadata from the JSON object.
     * This method first calls {@link #clear()}
     *
     * @param jsonObj The JSON Object
     * @throws IOException If JSON parsing fails
     */
    @Override
    public void fromJSONObject(final JsonObject jsonObj) throws IOException {
        super.fromJSONObject(jsonObj);
        try {
            JsonValue val = this.getAttributes().remove(InternalConstants.KEY_PROPERTIES);
            if (val != null) {
                for (final Map.Entry<String, JsonValue> innerEntry :
                        val.asJsonObject().entrySet()) {
                    final PropertyDescription prop = new PropertyDescription();
                    prop.fromJSONObject(innerEntry.getValue().asJsonObject());
                    if (this.getPropertyDescriptions().put(innerEntry.getKey(), prop) != null) {
                        throw new IOException("Duplicate key for property description (keys are case-insensitive) : "
                                .concat(innerEntry.getKey()));
                    }
                }
            }
            final String modeVal = this.getString(InternalConstants.KEY_MODE);
            if (modeVal != null) {
                this.setMode(Mode.valueOf(modeVal.toUpperCase()));
            }
            final String regionVal = this.getString(InternalConstants.KEY_REGION);
            if (regionVal != null) {
                this.setRegion(Region.valueOf(regionVal.toUpperCase()));
            }
            this.setAllowAdditionalProperties(this.getBoolean(
                    InternalConstants.KEY_ALLOW_ADDITIONAL_PROPERTIES, this.isAllowAdditionalProperties()));
            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_PROPERTIES);
            if (val != null) {
                for (final JsonValue v : val.asJsonArray()) {
                    this.getInternalPropertyNames().add(getString(v));
                }
            }
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the properties
     * @return Mutable map of properties by property name
     */
    public Map<String, PropertyDescription> getPropertyDescriptions() {
        return this.properties;
    }

    /**
     * Get the validation mode.
     * @return The mode or {@code null}
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
        this.mode = value;
    }

    /**
     * Are additional properties allowed?
     * @return {@code true} if additional properties are allowed
     * @since 1.4
     */
    public boolean isAllowAdditionalProperties() {
        return allowAdditional;
    }

    /**
     * Set whether additional properties are allowed
     * @param flag Set to {@code true} to allow additional properties
     * @since 1.4
     */
    public void setAllowAdditionalProperties(final boolean flag) {
        this.allowAdditional = flag;
    }

    /**
     * Which region does this entity apply to?
     * @return the region
     * @since 1.4
     */
    public Region getRegion() {
        return this.region;
    }

    /**
     * Set the region of this entity.
     * @param region The region
     * @since 1.4
     */
    public void setRegion(final Region region) {
        this.region = region == null ? Region.GLOBAL : region;
    }

    /**
     * Get the list of internal property names.
     * @return the mutable list of internal property names
     * @since 1.4
     */
    public List<String> getInternalPropertyNames() {
        return internalProperties;
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

        if (!this.getPropertyDescriptions().isEmpty()) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for (final Map.Entry<String, PropertyDescription> entry :
                    this.getPropertyDescriptions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_PROPERTIES, propBuilder);
        }
        if (this.getMode() != null) {
            objBuilder.add(InternalConstants.KEY_MODE, this.getMode().name());
        }
        if (this.getRegion() != Region.GLOBAL) {
            objBuilder.add(InternalConstants.KEY_REGION, this.getRegion().name());
        }
        if (this.isAllowAdditionalProperties()) {
            objBuilder.add(InternalConstants.KEY_ALLOW_ADDITIONAL_PROPERTIES, this.isAllowAdditionalProperties());
        }
        if (!this.getInternalPropertyNames().isEmpty()) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final String name : this.getInternalPropertyNames()) {
                arrayBuilder.add(name);
            }
            objBuilder.add(InternalConstants.KEY_INTERNAL_PROPERTIES, arrayBuilder);
        }

        return objBuilder;
    }
}
