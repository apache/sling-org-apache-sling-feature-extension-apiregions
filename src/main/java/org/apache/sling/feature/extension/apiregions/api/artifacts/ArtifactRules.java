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
package org.apache.sling.feature.extension.apiregions.api.artifacts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.apiregions.api.config.AttributeableEntity;

/**
 * Artifact rules define additional rules for artifacts in a feature model.
 * The rules are stored as an extension in the feature model.
 * This class is not thread safe.
 */
public class ArtifactRules extends AttributeableEntity {

    /** The name of the feature model extension. */
    public static final String EXTENSION_NAME = "artifact-rules";

    /**
     * Get the artifact rules from the feature - if it exists.
     * If the rules are updated, the containing feature is left untouched.
     * {@link #setArtifactRules(Feature, ArtifactRules)} can be used to update
     * the feature.
     * @param feature The feature
     * @return The rules or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ArtifactRules getArtifactRules(final Feature feature) {
        final Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getArtifactRules(ext);
    }

    /**
     * Get the artifact rules from the extension - if it exists.
     * If the rules are updated, the containing feature is left untouched.
     * {@link #setArtifactRules(Feature, ArtifactRules)} can be used to update
     * the feature.
     * @param ext The extension
     * @return The rules or {@code null} if the extension is {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ArtifactRules getArtifactRules(final Extension ext) {
        if ( ext == null ) {
            return null;
        }
        if ( ext.getType() != ExtensionType.JSON ) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        }
        try {
            final ArtifactRules result = new ArtifactRules();
            result.fromJSONObject(ext.getJSONStructure().asJsonObject());
            return result;
        } catch ( final IOException ioe) {
            throw new IllegalArgumentException(ioe.getMessage(), ioe);
        }
    }

    /**
     * Set the rules as an extension to the feature
     * @param feature The feature
     * @param rules The rules. If {@code null} the extension will be removed.
     * @throws IllegalStateException If the feature has already an extension of a wrong type
     * @throws IllegalArgumentException If the rules can't be serialized to JSON
     */
    public static void setArtifactRules(final Feature feature, final ArtifactRules rules) {
        Extension ext = feature.getExtensions().getByName(EXTENSION_NAME);
        if ( rules == null ) {
            if ( ext != null ) {
                feature.getExtensions().remove(ext);
            }
        } else {
            if ( ext == null ) {
                ext = new Extension(ExtensionType.JSON, EXTENSION_NAME, ExtensionState.OPTIONAL);
                feature.getExtensions().add(ext);
            }
            try {
                ext.setJSONStructure(rules.toJSONObject());
            } catch ( final IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }
    }

    /** The validation mode */
    private Mode mode;

    /** The version rules for bundles */
    private final List<VersionRule> bundleVersionRules = new ArrayList<>();

    /** The version rules for artifacts */
    private final List<VersionRule> artifactVersionRules = new ArrayList<>();

    /**
     * Create a new rules object
     */
    public ArtifactRules() {
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
        this.getBundleVersionRules().clear();
        this.getArtifactVersionRules().clear();
    }

    /**
     * Convert this object into JSON
     *
     * @return The json object builder
     * @throws IOException If generating the JSON fails
     */
    @Override
    public JsonObjectBuilder createJson() throws IOException {
        final JsonObjectBuilder objBuilder = super.createJson();
        if ( this.getMode() != Mode.STRICT ) {
            objBuilder.add(InternalConstants.KEY_MODE, this.getMode().name());
        }

        if ( !this.getBundleVersionRules().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final VersionRule rule : this.getBundleVersionRules()) {
                arrayBuilder.add(rule.createJson());
            }
            objBuilder.add(InternalConstants.KEY_BUNDLE_VERSION_RULES, arrayBuilder);
        }

        if ( !this.getArtifactVersionRules().isEmpty() ) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(final VersionRule rule : this.getArtifactVersionRules()) {
                arrayBuilder.add(rule.createJson());
            }
            objBuilder.add(InternalConstants.KEY_ARTIFACT_VERSION_RULES, arrayBuilder);
        }

        return objBuilder;
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
			final String modeVal = this.getString(InternalConstants.KEY_MODE);
			if ( modeVal != null ) {
                this.setMode(Mode.valueOf(modeVal.toUpperCase()));
			}

            JsonValue val = this.getAttributes().remove(InternalConstants.KEY_BUNDLE_VERSION_RULES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    final VersionRule rule = new VersionRule();
                    rule.fromJSONObject(innerVal.asJsonObject());
                    this.getBundleVersionRules().add(rule);
                }
            }

            val = this.getAttributes().remove(InternalConstants.KEY_ARTIFACT_VERSION_RULES);
            if ( val != null ) {
                for(final JsonValue innerVal : val.asJsonArray()) {
                    final VersionRule rule = new VersionRule();
                    rule.fromJSONObject(innerVal.asJsonObject());
                    this.getArtifactVersionRules().add(rule);
                }
            }
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the validation mode.
     * The default is {@link Mode#STRICT}
     * @return The mode
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Set the validation mode
     * @param value The validation mode
     */
    public void setMode(final Mode value) {
        this.mode = value == null ? Mode.STRICT : value;
    }

    /**
     * Return the list of version rules for bundles. The returned list is mutable.
     * @return The list of rules, might be empty.
     */
    public List<VersionRule> getBundleVersionRules() {
        return this.bundleVersionRules;
    }

    /**
     * Return the list of version rules for artifacts. The returned list is mutable.
     * @return the list of rules, might be empty.
     */
    public List<VersionRule> getArtifactVersionRules() {
        return artifactVersionRules;
    }
}
