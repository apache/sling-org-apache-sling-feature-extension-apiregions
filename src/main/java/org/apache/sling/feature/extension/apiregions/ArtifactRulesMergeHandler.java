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
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.MergeHandler;
import org.apache.sling.feature.extension.apiregions.api.artifacts.ArtifactRules;

/**
 * Merge the artifact rules extension
 */
public class ArtifactRulesMergeHandler implements MergeHandler {

    @Override
    public boolean canMerge(final Extension extension) {
        return ArtifactRules.EXTENSION_NAME.equals(extension.getName());
    }

    @Override
    public void merge(
            final HandlerContext context,
            final Feature targetFeature,
            final Feature sourceFeature,
            final Extension targetExtension,
            final Extension sourceExtension) {

        if (targetExtension == null) {
            // no target available yet, just copy source
            final ArtifactRules sourceRules = ArtifactRules.getArtifactRules(sourceExtension);
            ArtifactRules.setArtifactRules(targetFeature, sourceRules);
        } else {
            final ArtifactRules sourceRules = ArtifactRules.getArtifactRules(sourceExtension);
            final ArtifactRules targetRules = ArtifactRules.getArtifactRules(targetExtension);

            // mode merging
            if (context.isInitialMerge()) {
                targetRules.setMode(sourceRules.getMode());
            } else {
                if (targetRules.getMode().ordinal() > sourceRules.getMode().ordinal()) {
                    targetRules.setMode(sourceRules.getMode());
                }
            }

            // merge - just add
            targetRules.getBundleVersionRules().addAll(sourceRules.getBundleVersionRules());

            ArtifactRules.setArtifactRules(targetFeature, targetRules);
        }
    }
}
