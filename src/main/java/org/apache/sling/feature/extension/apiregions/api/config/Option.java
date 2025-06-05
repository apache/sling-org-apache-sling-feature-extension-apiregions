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

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Option for a property value
 * This class is not thread safe.
 */
public class Option extends DescribableEntity {

    /** The value for the option */
    private String value;

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.setValue(null);
    }

    /**
     * Extract the metadata from the JSON object.
     * This method first calls {@link #clear()}
     * @param jsonObj The JSON Object
     * @throws IOException If JSON parsing fails
     */
    @Override
    public void fromJSONObject(final JsonObject jsonObj) throws IOException {
        super.fromJSONObject(jsonObj);
        try {
            this.setValue(this.getString(InternalConstants.KEY_VALUE));
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the value for the option
     * @return the value or {@code null}
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value for the option
     * @param value the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Convert this object into JSON
     *
     * @return The json object builder
     * @throws IOException If generating the JSON fails
     */
    @Override
    protected JsonObjectBuilder createJson() throws IOException {
        final JsonObjectBuilder objectBuilder = super.createJson();

        this.setString(objectBuilder, InternalConstants.KEY_VALUE, this.getValue());

        return objectBuilder;
    }
}
