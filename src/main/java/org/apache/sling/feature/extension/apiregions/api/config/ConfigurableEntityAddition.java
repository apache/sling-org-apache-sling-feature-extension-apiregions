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
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.apache.felix.cm.json.io.Configurations;

/**
 * A description of an OSGi configuration addition
 * This class is not thread safe.
 * @since 1.8
 */
public abstract class ConfigurableEntityAddition extends AttributeableEntity {

    /** The properties */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final Map<String, PropertyDescriptionAddition> properties = (Map) Configurations.newConfiguration();

    /**
     * Create a new addition
     */
    public ConfigurableEntityAddition() {
        this.setDefaults();
    }

    /**
     * Get the property additions
     * @return The map of property additions
     */
    public Map<String, PropertyDescriptionAddition> getPropertyDescriptionAdditions() {
        return properties;
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.getPropertyDescriptionAdditions().clear();
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
                    final PropertyDescriptionAddition prop = new PropertyDescriptionAddition();
                    prop.fromJSONObject(innerEntry.getValue().asJsonObject());
                    if (this.getPropertyDescriptionAdditions().put(innerEntry.getKey(), prop) != null) {
                        throw new IOException("Duplicate key for property description (keys are case-insensitive) : "
                                .concat(innerEntry.getKey()));
                    }
                }
            }
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
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

        if (!this.getPropertyDescriptionAdditions().isEmpty()) {
            final JsonObjectBuilder propBuilder = Json.createObjectBuilder();
            for (final Map.Entry<String, PropertyDescriptionAddition> entry :
                    this.getPropertyDescriptionAdditions().entrySet()) {
                propBuilder.add(entry.getKey(), entry.getValue().createJson());
            }
            objBuilder.add(InternalConstants.KEY_PROPERTIES, propBuilder);
        }

        return objBuilder;
    }
}
