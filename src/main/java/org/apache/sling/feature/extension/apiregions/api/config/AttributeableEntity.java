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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import org.apache.felix.cm.json.io.Configurations;

/**
 * Abstract class used by all entities which allow additional attributes to be stored.
 * This class is not thread safe.
 */
public abstract class AttributeableEntity {

	/** The additional attributes */
	private final Map<String, JsonValue> attributes = new LinkedHashMap<>();

    /**
     * Apply the non-null default values.
     */
    protected void setDefaults() {
        // nothing to do
    }

        /**
     * Clear the object and reset to defaults
     */
	public void clear() {
        this.setDefaults();
		this.attributes.clear();
	}

   /**
     * Convert this object into JSON
     *
     * @return The json object
     * @throws IOException If generating the JSON fails
     */
    public JsonObject toJSONObject() throws IOException {
        final JsonObjectBuilder objectBuilder = this.createJson();
        return objectBuilder.build();
    }

	/**
	 * Extract the metadata from the JSON object.
	 * This method first calls {@link #clear()}
     *
	 * @param jsonObj The JSON Object
	 * @throws IOException If JSON parsing fails
	 */
	public void fromJSONObject(final JsonObject jsonObj) throws IOException {
		this.clear();
        try {
            for(final Map.Entry<String, JsonValue> entry : jsonObj.entrySet()) {
				this.getAttributes().put(entry.getKey(), entry.getValue());
			}
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
		}
	}

	/**
	 * Get the attributes
	 * @return Mutable map of attributes, by attribute name
	 */
	public Map<String, JsonValue> getAttributes() {
        return this.attributes;
    }

	/**
     * Convert this object into JSON
     *
     * @return The json object builder
     * @throws IOException If generating the JSON fails
     */
    protected JsonObjectBuilder createJson() throws IOException {
		final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

		for(final Map.Entry<String, JsonValue> entry : this.getAttributes().entrySet()) {
			objectBuilder.add(entry.getKey(), entry.getValue());
		}

		return objectBuilder;
	}

	/**
	 * Helper method to get a string value from a JsonValue
	 * @param jsonValue The json value
	 * @return The string value or {@code null}.
	 */
    protected String getString(final JsonValue jsonValue) {
		final Object obj = Configurations.convertToObject(jsonValue);
		if ( obj != null ) {
			return obj.toString();
		}
		return null;
	}

	/**
	 * Helper method to get a string value from an attribute
	 * @param attributeName The attribute name
	 * @return The string value or {@code null}.
	 */
    protected String getString(final String attributeName) {
		final JsonValue val = this.getAttributes().remove(attributeName);
		if ( val != null ) {
			final Object obj = Configurations.convertToObject(val);
			if ( obj != null ) {
				return obj.toString();
			}
		}
		return null;
	}

	/**
	 * Helper method to get a string array from an attribute
	 * @param attributeName The attribute name
	 * @return The string array or {@code null}.
     * @since 1.6.0
	 */
    protected String[] getStringArray(final String attributeName) throws IOException {
		final JsonValue val = this.getAttributes().remove(attributeName);
		if ( val != null ) {
            if ( val.getValueType() == ValueType.ARRAY ) {
                final JsonArray array = val.asJsonArray();
                final String[] result = new String[array.size()];
                int i = 0;
                for(final JsonValue v : array) {
                    result[i] = Configurations.convertToObject(v).toString();
                    i++;
                }
                return result;
            } else if ( val.getValueType() == ValueType.STRING ) {
                return new String[] {((JsonString)val).getString()};
            } else {
                throw new IOException("Invalid type for string array value " + attributeName + " : " + val.getValueType().name());
            }
		}
		return null;
	}

    /**
	 * Helper method to get a number value from an attribute
	 * @param attributeName The attribute name
	 * @return The string value or {@code null}.
     * @throws IOException If the attribute value is not of type boolean
	 */
    protected Number getNumber(final String attributeName) throws IOException {
		final JsonValue val = this.getAttributes().remove(attributeName);
		if ( val != null ) {
			final Object obj = Configurations.convertToObject(val);
			if ( obj instanceof Number ) {
				return (Number)obj;
			}
			throw new IOException("Invalid type for number value " + attributeName + " : " + val.getValueType().name());
		}
		return null;
	}

    /**
     * Helper method to set a string value
     * @param builder The json object builder
     * @param attributeName The name of the attribute
     * @param value The string value
     */
    protected void setString(final JsonObjectBuilder builder, final String attributeName, final String value) {
		if ( value != null ) {
			builder.add(attributeName, value);
		}
	}

    /**
     * Helper method to set a string array
     * @param builder The json object builder
     * @param attributeName The name of the attribute
     * @param value The string array
     * @since 1.6.0
     */
    protected void setStringArray(final JsonObjectBuilder builder, final String attributeName, final String[] value) {
		if ( value != null && value.length > 0 ) {
            final JsonArrayBuilder jab = Json.createArrayBuilder();
            for(final String v : value) {
                jab.add(v);
            }
			builder.add(attributeName, jab);
		}
	}

    /**
	 * Helper method to get a integer value from an attribute
	 * @param attributeName The attribute name
	 * @param defaultValue default value
	 * @return The integer value or the default value
	 */
    protected int getInteger(final String attributeName, final int defaultValue) {
		final String val = this.getString(attributeName);
		if ( val != null ) {
			return Integer.parseInt(val);
		}
		return defaultValue;
	}

	/**
	 * Helper method to get a boolean value from an attribute
	 * @param attributeName The attribute name
	 * @param defaultValue default value
	 * @return The boolean value or the default value
     * @throws IOException If the attribute value is not of type boolean
	 */
    protected boolean getBoolean(final String attributeName, final boolean defaultValue) throws IOException {
		final JsonValue val = this.getAttributes().remove(attributeName);
		if ( val != null ) {
			final Object obj = Configurations.convertToObject(val);
			if ( obj instanceof Boolean ) {
                return ((Boolean)obj).booleanValue();
			}
			throw new IOException("Invalid type for boolean value " + attributeName + " : " + val.getValueType().name());
		}
		return defaultValue;
	}
}
