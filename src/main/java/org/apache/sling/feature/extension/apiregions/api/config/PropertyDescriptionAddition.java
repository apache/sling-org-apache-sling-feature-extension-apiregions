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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Instances of this class represent an addition to a configuration property
 * This class is not thread safe.
 * @since 1.8
 */
public class PropertyDescriptionAddition extends AttributeableEntity {

	/** The required includes for an array/collection (optional) */
	private String[] includes;

    /**
     * Create a new description
     */
    public PropertyDescriptionAddition() {
        this.setDefaults();
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
	public void clear() {
        super.clear();
		this.setIncludes(null);
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
			final JsonValue incs = this.getAttributes().remove(InternalConstants.KEY_INCLUDES);
			if ( incs != null ) {
				final List<String> list = new ArrayList<>();
				for(final JsonValue innerVal : incs.asJsonArray()) {
                    list.add(getString(innerVal));
                }
                this.setIncludes(list.toArray(new String[list.size()]));
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
		final JsonObjectBuilder objectBuilder = super.createJson();

		if ( this.getIncludes() != null && this.getIncludes().length > 0 ) {
			final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for(final String v : this.getIncludes()) {
				arrayBuilder.add(v);
			}
			objectBuilder.add(InternalConstants.KEY_INCLUDES, arrayBuilder);
		}
        return objectBuilder;
	}

	/**
	 * Get the includes
	 * @return the includes or {@code null}
	 */
	public String[] getIncludes() {
		return includes;
	}

	/**
	 * Set the includes
	 * @param includes the includes to set
	 */
	public void setIncludes(final String[] includes) {
		this.includes = includes;
	}
}
