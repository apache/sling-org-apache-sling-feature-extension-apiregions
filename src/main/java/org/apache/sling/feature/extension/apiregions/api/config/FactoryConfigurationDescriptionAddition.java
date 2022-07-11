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
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Description of an OSGi factory configuration addition
 * This class is not thread safe.
 */
public class FactoryConfigurationDescriptionAddition extends ConfigurableEntityAddition {
    
    private final List<String> internalNames = new ArrayList<>();

    public FactoryConfigurationDescriptionAddition() {
        this.setDefaults();
    }
    
    /**
     * Clear the object and set the defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.internalNames.clear();
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
            JsonValue val;
            val = this.getAttributes().remove(InternalConstants.KEY_INTERNAL_NAMES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    this.getInternalNames().add(getString(innerVal));
                }
            }

		} catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

	/**
     * Get the internal factory configuration name
	 * @return Mutable list of internal names
	 */
	public List<String> getInternalNames() {
		return internalNames;
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
		
		if ( !this.getInternalNames().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final String n : this.getInternalNames()) {
                arrayBuilder.add(n);
            }
			objBuilder.add(InternalConstants.KEY_INTERNAL_NAMES, arrayBuilder);
		}
		return objBuilder;
   }
}
