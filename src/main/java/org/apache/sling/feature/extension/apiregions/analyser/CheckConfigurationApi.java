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
package org.apache.sling.feature.extension.apiregions.analyser;

import java.util.Map;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationApi;
import org.apache.sling.feature.extension.apiregions.api.config.validation.ConfigurationValidationResult;
import org.apache.sling.feature.extension.apiregions.api.config.validation.FeatureValidationResult;
import org.apache.sling.feature.extension.apiregions.api.config.validation.FeatureValidator;
import org.apache.sling.feature.extension.apiregions.api.config.validation.PropertyValidationResult;

public class CheckConfigurationApi implements AnalyserTask {

    @Override
    public String getId() {
        return "configuration-api";
    }

    @Override
    public String getName() {
        return "Configuration API analyser task";
    }

    @Override
    public void execute(final AnalyserTaskContext context) throws Exception {
        final FeatureValidator validator = new FeatureValidator();
        validator.setFeatureProvider(context.getFeatureProvider());

        final ConfigurationApi api = ConfigurationApi.getConfigurationApi(context.getFeature());
        if (api == null) {
            context.reportExtensionWarning(
                    ConfigurationApi.EXTENSION_NAME, "Configuration api is not specified, unable to validate feature");
        } else {
            final FeatureValidationResult result = validator.validate(context.getFeature(), api);
            for (final Map.Entry<String, PropertyValidationResult> entry :
                    result.getFrameworkPropertyResults().entrySet()) {
                for (final String warn : entry.getValue().getWarnings()) {
                    context.reportWarning("Framework property " + entry.getKey() + " : " + warn);
                }
                if (!entry.getValue().isValid()) {
                    for (final String err : entry.getValue().getErrors()) {
                        context.reportError("Framework property " + entry.getKey() + " : " + err);
                    }
                }
            }
            for (final Map.Entry<String, ConfigurationValidationResult> entry :
                    result.getConfigurationResults().entrySet()) {
                for (final String warn : entry.getValue().getWarnings()) {
                    context.reportConfigurationWarning(
                            context.getFeature().getConfigurations().getConfiguration(entry.getKey()), warn);
                }
                for (final Map.Entry<String, PropertyValidationResult> propEntry :
                        entry.getValue().getPropertyResults().entrySet()) {
                    for (final String warn : propEntry.getValue().getWarnings()) {
                        context.reportConfigurationWarning(
                                context.getFeature().getConfigurations().getConfiguration(entry.getKey()),
                                "Property "
                                        .concat(propEntry.getKey())
                                        .concat(" - ")
                                        .concat(warn));
                    }
                }
                if (!entry.getValue().isValid()) {
                    for (final String err : entry.getValue().getErrors()) {
                        context.reportConfigurationError(
                                context.getFeature().getConfigurations().getConfiguration(entry.getKey()), err);
                    }
                    for (final Map.Entry<String, PropertyValidationResult> propEntry :
                            entry.getValue().getPropertyResults().entrySet()) {
                        if (!propEntry.getValue().isValid()) {
                            for (final String err : propEntry.getValue().getErrors()) {
                                context.reportConfigurationError(
                                        context.getFeature().getConfigurations().getConfiguration(entry.getKey()),
                                        "Property "
                                                .concat(propEntry.getKey())
                                                .concat(" - ")
                                                .concat(err));
                            }
                        }
                    }
                }
            }
        }
    }
}
