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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A configuration validation result is returned by the {@code ConfigurationValidator}.
 * This class is not thread safe.
 */
public class ConfigurationValidationResult {

    private final Map<String, PropertyValidationResult> propertyResults = new HashMap<>();

    private final List<String> errors = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    /**
     * Should the default configuration be used?
     * @since 1.3
     */
    private boolean useDefault = false;

    /**
     * Is the configuration valid?
     * @return {@code true} if it is valid
     */
    public boolean isValid() {
        boolean valid = errors.isEmpty();
        if (valid) {
            for (final PropertyValidationResult r : this.propertyResults.values()) {
                if (!r.isValid()) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    /**
     * Return the list of errors
     * @return A list of errors. Might be empty.
     */
    public List<String> getErrors() {
        return this.errors;
    }

    /**
     * Get a property validation result for each property of the configuration
     * @return A map of property results keyed by property name
     */
    public Map<String, PropertyValidationResult> getPropertyResults() {
        return propertyResults;
    }

    /**
     * Return the list of warnings
     * @return The list of warnings - might be empty
     */
    public List<String> getWarnings() {
        return this.warnings;
    }

    /**
     * Should the default be used instead of the configuration values?
     * @return {@code true} if the default should be used.
     * @since 1.3
     */
    public boolean isUseDefaultValue() {
        return useDefault;
    }

    /**
     * Set whether the default values should be used
     * @param useDefault boolean flag
     * @since 1.3
     */
    public void setUseDefaultValue(final boolean useDefault) {
        this.useDefault = useDefault;
    }
}
