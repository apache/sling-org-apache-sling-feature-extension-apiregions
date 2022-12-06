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

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.extension.apiregions.api.config.AttributeableEntity;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * A rule to validate the version of an artifact.
 * This class is not thread safe.
 */
public class VersionRule extends AttributeableEntity {

    /** The optional validation mode */
    private Mode mode;

    /** The artifact id. */
    private ArtifactId artifactId;

    /** The message */
    private String message;

    /** The allowed version ranges */
    private VersionRange[] allowedVersionRanges;

    /** The denied version ranges */
    private VersionRange[] deniedVersionRanges;

    /**
     * Create a new rules object
     */
    public VersionRule() {
        this.setDefaults();
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.setArtifactId(null);
        this.setMode(null);
        this.setMessage(null);
        this.setAllowedVersionRanges(null);
        this.setDeniedVersionRanges(null);
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
        if ( this.getMode() != null ) {
            objBuilder.add(InternalConstants.KEY_MODE, this.getMode().name());
        }

        if ( this.getArtifactId() != null ) {
            objBuilder.add(InternalConstants.KEY_ARTIFACT_ID, this.getArtifactId().toMvnId());
        }

        this.setString(objBuilder, InternalConstants.KEY_MESSAGE, this.getMessage());

        if ( this.getAllowedVersionRanges() != null && this.getAllowedVersionRanges().length > 0 ) {
            final String[] arr = new String[this.getAllowedVersionRanges().length];
            for(int i=0;i<this.getAllowedVersionRanges().length;i++) {
                arr[i] = this.getAllowedVersionRanges()[i].toString();
            }
            this.setStringArray(objBuilder, InternalConstants.KEY_ALLOWED_VERSION_RANGES, arr);
        }

        if ( this.getDeniedVersionRanges() != null && this.getDeniedVersionRanges().length > 0 ) {
            final String[] arr = new String[this.getDeniedVersionRanges().length];
            for(int i=0;i<this.getDeniedVersionRanges().length;i++) {
                arr[i] = this.getDeniedVersionRanges()[i].toString();
            }
            this.setStringArray(objBuilder, InternalConstants.KEY_DENIED_VERSION_RANGES, arr);
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
			String val = this.getString(InternalConstants.KEY_MODE);
			if ( val != null ) {
                this.setMode(Mode.valueOf(val.toUpperCase()));
			}
            
            val = this.getString(InternalConstants.KEY_ARTIFACT_ID);
            if ( val != null ) {
                this.setArtifactId(ArtifactId.parse(val));
            }

            this.setMessage(this.getString(InternalConstants.KEY_MESSAGE));

            String[] arr = this.getStringArray(InternalConstants.KEY_ALLOWED_VERSION_RANGES);
            if ( arr != null && arr.length > 0 ) {
                final VersionRange[] ranges = new VersionRange[arr.length];
                for(int i=0;i<arr.length;i++) {
                    try  {
                        ranges[i] = new VersionRange(arr[i]);
                    } catch ( final IllegalArgumentException iae) {
                        throw new IOException("Illegal argument for allowed version range: " + arr[i]);
                    }
                }
                this.setAllowedVersionRanges(ranges);
            }

            arr = this.getStringArray(InternalConstants.KEY_DENIED_VERSION_RANGES);
            if ( arr != null && arr.length > 0 ) {
                final VersionRange[] ranges = new VersionRange[arr.length];
                for(int i=0;i<arr.length;i++) {
                    try  {
                        ranges[i] = new VersionRange(arr[i]);
                    } catch ( final IllegalArgumentException iae) {
                        throw new IOException("Illegal argument for allowed version range: " + arr[i]);
                    }
                }
                this.setDeniedVersionRanges(ranges);
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
        this.mode = value;
    }

    /**
     * Get the artifact id
     * @return the artifactId
     */
    public ArtifactId getArtifactId() {
        return artifactId;
    }

    /**
     * Set the artifact id
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(final ArtifactId artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * The validation message
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the validation message
     * @param message the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * The allowed version ranges
     * @return the allowedVersions or {@code null}
     */
    public VersionRange[] getAllowedVersionRanges() {
        return allowedVersionRanges;
    }

    /**
     * Set the allowed version ranges
     * @param allowedVersions the allowedVersions to set
     */
    public void setAllowedVersionRanges(final VersionRange[] allowedVersions) {
        this.allowedVersionRanges = allowedVersions;
    }

    /**
     * Get the denied version ranges
     * @return the deniedVersions or {@code null}
     */
    public VersionRange[] getDeniedVersionRanges() {
        return deniedVersionRanges;
    }

    /**
     * Set the denied version ranges
     * @param deniedVersions the deniedVersions to set
     */
    public void setDeniedVersionRanges(final VersionRange[] deniedVersions) {
        this.deniedVersionRanges = deniedVersions;
    }

    /**
     * Check if a version is allowed according to the rules
     * @param artifactVersion The version
     * @return {@code true} if it is allowed, {@code false} otherwise
     */
    public boolean isAllowed(final Version artifactVersion) {
        boolean result = false;
        if ( this.getAllowedVersionRanges() != null && this.getAllowedVersionRanges().length > 0 ) {
            for(final VersionRange range : this.getAllowedVersionRanges()) {
                if ( range.includes(artifactVersion) ) {
                    result = true;
                    break;
                }
            }
            if ( result && this.getDeniedVersionRanges() != null ) {
                for(final VersionRange range : this.getDeniedVersionRanges()) {
                    if ( range.includes(artifactVersion) ) {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;

    }
}
