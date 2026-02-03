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
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.apache.felix.cm.json.io.Configurations;

/**
 * Instances of this class represent a single configuration property
 * This class is not thread safe.
 */
public class PropertyDescription extends DescribableEntity {

    /** The property type */
    private PropertyType type;

    /** The property cardinality */
    private int cardinality;

    /** The optional variable */
    private String variable;

    /** The optional range */
    private Range range;

    /** The required includes for an array/collection (optional) */
    private String[] includes;

    /** The required excludes for an array/collection (optional) */
    private String[] excludes;

    /** The optional list of options for the value */
    private List<Option> options;

    /** The optional regex */
    private Pattern pattern;

    /** Required? */
    private boolean required;

    /**
     * The optional default value
     * @since 1.2
     */
    private Object defaultValue;

    /**
     * The validation mode.
     * @since 1.2
     */
    private Mode mode;

    /**
     * The internal mode.
     */
    private Mode internalMode;

    /**
     * The placeholder policy
     * @since 1.3
     */
    private PlaceholderPolicy placeholderPolicy;

    /**
     * A pattern to validate values which contain substitution placeholders
     * @since 1.3
     */
    private Pattern placeholderPattern;

    /**
     * Create a new description
     */
    public PropertyDescription() {
        this.setDefaults();
    }

    protected void setDefaults() {
        super.setDefaults();
        this.setType(PropertyType.STRING);
        this.setCardinality(1);
        this.setRequired(false);
        this.setPlaceholderPolicy(PlaceholderPolicy.DEFAULT);
    }

    /**
     * Clear the object and reset to defaults
     */
    @Override
    public void clear() {
        super.clear();
        this.setVariable(null);
        this.setRange(null);
        this.setIncludes(null);
        this.setExcludes(null);
        this.setOptions(null);
        this.setRegex(null);
        this.setDefaultValue(null);
        this.setMode(null);
        this.setInternalMode(null);
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
            this.setVariable(this.getString(InternalConstants.KEY_VARIABLE));
            this.setCardinality(this.getInteger(InternalConstants.KEY_CARDINALITY, this.getCardinality()));
            this.setRequired(this.getBoolean(InternalConstants.KEY_REQUIRED, this.isRequired()));

            final String typeVal = this.getString(InternalConstants.KEY_TYPE);
            if (typeVal != null) {
                this.setType(PropertyType.valueOf(typeVal.toUpperCase()));
            }
            final JsonValue rangeVal = this.getAttributes().remove(InternalConstants.KEY_RANGE);
            if (rangeVal != null) {
                final Range range = new Range();
                range.fromJSONObject(rangeVal.asJsonObject());
                this.setRange(range);
            }
            final JsonValue incs = this.getAttributes().remove(InternalConstants.KEY_INCLUDES);
            if (incs != null) {
                final List<String> list = new ArrayList<>();
                for (final JsonValue innerVal : incs.asJsonArray()) {
                    list.add(getString(innerVal));
                }
                this.setIncludes(list.toArray(new String[list.size()]));
            }
            final JsonValue excs = this.getAttributes().remove(InternalConstants.KEY_EXCLUDES);
            if (excs != null) {
                final List<String> list = new ArrayList<>();
                for (final JsonValue innerVal : excs.asJsonArray()) {
                    list.add(getString(innerVal));
                }
                this.setExcludes(list.toArray(new String[list.size()]));
            }
            final JsonValue opts = this.getAttributes().remove(InternalConstants.KEY_OPTIONS);
            if (opts != null) {
                final List<Option> list = new ArrayList<>();
                for (final JsonValue innerVal : opts.asJsonArray()) {
                    final Option o = new Option();
                    o.fromJSONObject(innerVal.asJsonObject());
                    list.add(o);
                }
                this.setOptions(list);
            }
            this.setRegex(this.getString(InternalConstants.KEY_REGEX));
            final JsonValue dv = this.getAttributes().remove(InternalConstants.KEY_DEFAULT);
            if (dv != null) {
                this.setDefaultValue(Configurations.convertToObject(dv));
            }
            final String modeVal = this.getString(InternalConstants.KEY_MODE);
            if (modeVal != null) {
                this.setMode(Mode.valueOf(modeVal.toUpperCase()));
            }
            final String internalModeVal = this.getString(InternalConstants.KEY_INTERNAL_MODE);
            if (internalModeVal != null) {
                this.setInternalMode(Mode.valueOf(internalModeVal.toUpperCase()));
            }
            final String policyVal = this.getString(InternalConstants.KEY_PLACEHOLDER_POLICY);
            if (policyVal != null) {
                this.setPlaceholderPolicy(PlaceholderPolicy.valueOf(policyVal.toUpperCase()));
            }
            this.setPlaceholderRegex(this.getString(InternalConstants.KEY_PLACEHOLDER_REGEX));
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

        if (this.getType() != null && this.getType() != PropertyType.STRING) {
            this.setString(
                    objectBuilder, InternalConstants.KEY_TYPE, this.getType().name());
        }
        if (this.getCardinality() != 1) {
            objectBuilder.add(InternalConstants.KEY_CARDINALITY, this.getCardinality());
        }
        if (this.isRequired()) {
            objectBuilder.add(InternalConstants.KEY_REQUIRED, this.isRequired());
        }
        this.setString(objectBuilder, InternalConstants.KEY_VARIABLE, this.getVariable());

        if (this.getRange() != null) {
            objectBuilder.add(InternalConstants.KEY_RANGE, this.getRange().toJSONObject());
        }
        if (this.getIncludes() != null && this.getIncludes().length > 0) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final String v : this.getIncludes()) {
                arrayBuilder.add(v);
            }
            objectBuilder.add(InternalConstants.KEY_INCLUDES, arrayBuilder);
        }
        if (this.getExcludes() != null && this.getExcludes().length > 0) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final String v : this.getExcludes()) {
                arrayBuilder.add(v);
            }
            objectBuilder.add(InternalConstants.KEY_EXCLUDES, arrayBuilder);
        }
        if (this.getOptions() != null && !this.getOptions().isEmpty()) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final Option o : this.getOptions()) {
                arrayBuilder.add(o.toJSONObject());
            }
            objectBuilder.add(InternalConstants.KEY_OPTIONS, arrayBuilder);
        }
        this.setString(objectBuilder, InternalConstants.KEY_REGEX, this.getRegex());
        if (this.getDefaultValue() != null) {
            objectBuilder.add(InternalConstants.KEY_DEFAULT, Configurations.convertToJsonValue(this.getDefaultValue()));
        }
        if (this.getMode() != null) {
            objectBuilder.add(InternalConstants.KEY_MODE, this.getMode().name());
        }
        if (this.getInternalMode() != null) {
            objectBuilder.add(
                    InternalConstants.KEY_INTERNAL_MODE, this.getInternalMode().name());
        }
        if (this.getPlaceholderPolicy() != PlaceholderPolicy.DEFAULT) {
            objectBuilder.add(
                    InternalConstants.KEY_PLACEHOLDER_POLICY,
                    this.getPlaceholderPolicy().name());
        }
        this.setString(objectBuilder, InternalConstants.KEY_PLACEHOLDER_REGEX, this.getPlaceholderRegex());

        return objectBuilder;
    }

    /**
     * Get the property type
     * @return the type
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * Set the property type
     * @param type the type to set
     */
    public void setType(final PropertyType type) {
        this.type = type == null ? PropertyType.STRING : type;
    }

    /**
     * Get the cardinality
     * @return the cardinality
     */
    public int getCardinality() {
        return cardinality;
    }

    /**
     * Set the cardinality
     * The default cardinality is {@code 1}. If the value is greater than zero
     * the property can contain up to that number of values.
     * If the cardinality is {@code -1} the property can hold an unlimited number
     * of values.
     * @param value the cardinality to set
     * @throws IllegalArgumentException If the value is {@code 0} or below {@code -1}.
     */
    public void setCardinality(final int value) {
        if (value == 0 || value < -1) {
            throw new IllegalArgumentException();
        }
        this.cardinality = value;
    }

    /**
     * Get the variable
     * @return the variable or {@code null}
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Set the variable
     * @param variable the variable to set
     */
    public void setVariable(final String variable) {
        this.variable = variable;
    }

    /**
     * Get the range
     * @return the range or {@code null}
     */
    public Range getRange() {
        return range;
    }

    /**
     * Set the range
     * @param range the range to set
     */
    public void setRange(final Range range) {
        this.range = range;
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

    /**
     * Get the excludes
     * @return the excludes or {@code null}
     */
    public String[] getExcludes() {
        return excludes;
    }

    /**
     * Set the excludes
     * @param excludes the excludes to set
     */
    public void setExcludes(final String[] excludes) {
        this.excludes = excludes;
    }

    /**
     * Get the list of options
     * @return the options or {@code null}
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * Set the list of options
     * @param options the options to set
     */
    public void setOptions(final List<Option> options) {
        this.options = options;
    }

    /**
     * Get the regex
     * @return the regex or {@code null}
     */
    public String getRegex() {
        return pattern == null ? null : pattern.pattern();
    }

    /**
     * Set the regex
     * @param regex the regex to set
     * @throws IllegalArgumentException If the pattern is not valid
     */
    public void setRegex(final String regex) {
        if (regex == null) {
            this.pattern = null;
        } else {
            this.pattern = Pattern.compile(regex);
        }
    }

    /**
     * Get the regex pattern
     * @return The pattern or {@code null}
     */
    public Pattern getRegexPattern() {
        return this.pattern;
    }

    /**
     * Is this property required?
     * @return {@code true} if it is required
     */
    public boolean isRequired() {
        return this.required;
    }

    /**
     * Set whether this property is required
     * @param flag The new value
     */
    public void setRequired(final boolean flag) {
        this.required = flag;
    }

    /**
     * Get the optional default value.
     * @return The default value or {@code null}
     * @since 1.2
     */
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Set the optional default value.
     * @param val The default value
     * @since 1.2
     */
    public void setDefaultValue(final Object val) {
        this.defaultValue = val;
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
     * Get the internal mode.
     * @return The internal mode or {@code null}
     */
    public Mode getInternalMode() {
        return this.internalMode;
    }

    /**
     * Set the internal mode
     * @param value The internal mode
     */
    public void setInternalMode(final Mode value) {
        this.internalMode = value;
    }

    /**
     * Get the placeholder policy.
     * @return The policy
     * @since 1.3
     */
    public PlaceholderPolicy getPlaceholderPolicy() {
        return this.placeholderPolicy;
    }

    /**
     * Set the placeholder policy
     * @param policy The new policy
     * @since 1.3
     */
    public void setPlaceholderPolicy(final PlaceholderPolicy policy) {
        this.placeholderPolicy = policy == null ? PlaceholderPolicy.DEFAULT : policy;
    }

    /**
     * Get the placeholder regex
     * @return the placeholder regex or {@code null}
     * @since 1.3
     */
    public String getPlaceholderRegex() {
        return placeholderPattern == null ? null : placeholderPattern.pattern();
    }

    /**
     * Set the placeholder regex
     * @param regex the placeholder regex to set
     * @throws IllegalArgumentException If the pattern is not valid
     * @since 1.3
     */
    public void setPlaceholderRegex(final String regex) {
        if (regex == null) {
            this.placeholderPattern = null;
        } else {
            this.placeholderPattern = Pattern.compile(regex);
        }
    }

    /**
     * Get the placeholder regex pattern
     * @return The pattern or {@code null}
     * @since 1.3
     */
    public Pattern getPlaceholderRegexPattern() {
        return this.placeholderPattern;
    }
}
