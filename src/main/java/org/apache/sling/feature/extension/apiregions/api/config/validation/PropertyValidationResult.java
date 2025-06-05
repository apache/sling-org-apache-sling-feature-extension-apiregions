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
package org.apache.sling.feature.extension.apiregions.api.config.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result for a property
 * This class is not thread safe.
 */
public class PropertyValidationResult {

    private final List<String> errors = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    private boolean skipped = false;

    /**
     * Should excludes/includes be used?
     * @since 1.6
     */
    private String[] includes;

    /**
     * Should excludes/includes be used?
     * @since 1.6
     */
    private String[] excludes;

    /**
     * Should the default be used?
     * @since 1.2
     */
    private boolean useDefault = false;

    /**
     * The default value
     * @since 1.2
     */
    private Object defaultValue;

    /**
     * Is the property value valid?
     * @return {@code true} if the value is valid
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * If {@link #isValid()} returns {@code false} this returns
     * a list of human readable errors.
     * @return A list of errors - empty if {@link #isValid()} returns {@code true}
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Return the list of warnings
     * @return The list of warnings - might be empty
     */
    public List<String> getWarnings() {
        return this.warnings;
    }

    /**
     * Has the validation for this property be skipped?
     * @return {@code true} if it has been skipped
     */
    public boolean isSkipped() {
        return this.skipped;
    }

    /**
     * Mark the property to be skipped during validation
     */
    public void markSkipped() {
        this.skipped = true;
    }

    /**
     * Should the default be used instead of the configuration value?
     * @return {@code true} if the default should be used.
     * @see #getDefaultValue()
     * @since 1.2
     */
    public boolean isUseDefaultValue() {
        return useDefault;
    }

    /**
     * Set whether the default value should be used
     * @param useDefault boolean flag
     * @since 1.2
     */
    public void setUseDefaultValue(final boolean useDefault) {
        this.useDefault = useDefault;
    }

    /**
     * Get the default value. The default value is only returned
     * if {@link #isUseDefaultValue()} returns {@code true}.
     * @return the defaultValue (it might be {@code null})
     * @since 1.2
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the default value
     * @param defaultValue the defaultValue to set
     * @since 1.2
     */
    public void setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Get the excludes to be used
     * @return The excludes or {@code null}
     * @since 1.6
     */
    public String[] getUseExcludes() {
        return this.excludes;
    }

    /**
     * Get the includes to be used
     * @return The includes or {@code null}
     * @since 1.6
     */
    public String[] getUseIncludes() {
        return this.includes;
    }

    /**
     * Set whether the excludes and includes should be used. At least one of it should not be {@code null}
     * @param includes The includes to use
     * @param excludes The excludes to use
     * @since 1.6
     */
    public void setUseIncludesAndExcludes(final String[] includes, final String[] excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }
}
