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

import java.util.HashMap;
import java.util.Map;

/**
 * Validation result for a feature
 * This class is not thread safe.
 */
public class FeatureValidationResult {

    private final Map<String, ConfigurationValidationResult> configurationResults = new HashMap<>();

    private final Map<String, PropertyValidationResult> frameworkPropertyResults = new HashMap<>();

    /**
     * Is the configuration of the feature valid?
     * @return {@code true} if it is valid
     */
    public boolean isValid() {
        boolean valid = true;
        for (final ConfigurationValidationResult r : this.configurationResults.values()) {
            if (!r.isValid()) {
                valid = false;
                break;
            }
        }
        if (valid) {
            for (final PropertyValidationResult r : this.frameworkPropertyResults.values()) {
                if (!r.isValid()) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    /**
     * Get the confiugration validation results.
     * @return The results keyed by configuration PIDs
     */
    public Map<String, ConfigurationValidationResult> getConfigurationResults() {
        return this.configurationResults;
    }

    /**
     * Get the framework property validation results
     * @return The results keyed by framework property name
     */
    public Map<String, PropertyValidationResult> getFrameworkPropertyResults() {
        return this.frameworkPropertyResults;
    }
}
