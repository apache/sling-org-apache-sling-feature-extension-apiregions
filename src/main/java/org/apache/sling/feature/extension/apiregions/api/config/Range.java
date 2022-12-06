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

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.apache.felix.cm.json.io.Configurations;

/**
 * A numerical value range
 * This class is not thread safe.
 */
public class Range extends AttributeableEntity {

    /** The optional min value */
    private Number min;

    /** The optional max value */
    private Number max;

    /**
     * Clear the object and reset to defaults
     */
    @Override
	public void clear() {
        super.clear();
        this.setMax(null);
        this.setMin(null);
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
			this.setMin(this.getNumber(InternalConstants.KEY_MIN));
			this.setMax(this.getNumber(InternalConstants.KEY_MAX));
 		} catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
	}

	/**
     * Get the min value
	 * @return the min or {@code null}
	 */
	public Number getMin() {
		return min;
	}

	/**
     * Set the min value
	 * @param min the min to set
	 */
	public void setMin(final Number min) {
		this.min = min;
	}

	/**
     * Get the max value
	 * @return the max or {@code null}
	 */
	public Number getMax() {
		return max;
	}

	/**
     * Set the max value
	 * @param max the max to set
	 */
	public void setMax(final Number max) {
		this.max = max;
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

        if ( this.getMin() != null ) {
            objectBuilder.add(InternalConstants.KEY_MIN, Configurations.convertToJsonValue(this.getMin()));
        }
        if ( this.getMax() != null ) {
            objectBuilder.add(InternalConstants.KEY_MAX, Configurations.convertToJsonValue(this.getMax()));
        }

		return objectBuilder;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Range [min=" + getMax() + ", max=" + getMax() + "]";
	}	
}
