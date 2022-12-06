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

/**
 * Abstract class for all describable entities, having an optional title,
 * description and deprecation info.
 * This class is not thread safe.
 */
public abstract class DescribableEntity extends AttributeableEntity {
	
	/** The title */
    private String title;

	/** The description */
    private String description;

	/** The optional deprecation text */
	private String deprecated;

	/**
	 * Optional since information.
	 * @since 1.3.6
	 */
	private String since;

	/**
	 * Optional enforce on information.
	 * @since 1.3.6
	 */
	private String enforceOn;

	/**
     * Clear the object and reset to defaults
     */
    @Override
	public void clear() {
		super.clear();
		this.setTitle(null);
		this.setDescription(null);
		this.setDeprecated(null);
		this.setSince(null);
		this.setEnforceOn(null);
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
			this.setTitle(this.getString(InternalConstants.KEY_TITLE));
			this.setDescription(this.getString(InternalConstants.KEY_DESCRIPTION));
			this.setDeprecated(this.getString(InternalConstants.KEY_DEPRECATED));
			this.setSince(this.getString(InternalConstants.KEY_SINCE));
			this.setEnforceOn(this.getString(InternalConstants.KEY_ENFORCE_ON));
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
		}
	}

	/**
	 * Get the title
	 * @return The title or {@code null}
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the title
	 * @param title the title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Get the description
	 * @return the description or {@code null}
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description the description to set
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * Get the deprecation text
	 * @return the deprecation text or {@code null}
	 */
	public String getDeprecated() {
		return deprecated;
	}

	/**
	 * Set the deprecation text
	 * @param deprecated the deprecation text to set
	 */
	public void setDeprecated(final String deprecated) {
		this.deprecated = deprecated;
	}

	/**
	 * Get the optional since information
	 * @return The since information or {@code null}
	 */
	public String getSince() {
		return since;
	}

	/**
	 * Set the since information. This should a date in the format 'YYYY-MM-DD'.
	 * @param since The new info
	 */
	public void setSince(final String since) {
		this.since = since;
	}


	/**
	 * Get the optional since information
	 * @return The since information or {@code null}
	 */
	public String getEnforceOn() {
		return enforceOn;
	}

	/**
	 * Set the enforce on information.
	 * @param enforceOn The new info
	 */
	public void setEnforceOn(final String enforceOn) {
		this.enforceOn = enforceOn;
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

		this.setString(objectBuilder, InternalConstants.KEY_TITLE, this.getTitle());
		this.setString(objectBuilder, InternalConstants.KEY_DESCRIPTION, this.getDescription());
		this.setString(objectBuilder, InternalConstants.KEY_DEPRECATED, this.getDeprecated());
		this.setString(objectBuilder, InternalConstants.KEY_SINCE, this.getSince());
		this.setString(objectBuilder, InternalConstants.KEY_ENFORCE_ON, this.getEnforceOn());

		return objectBuilder;
	}
}
