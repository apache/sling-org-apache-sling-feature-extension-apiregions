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

import java.util.Calendar;
import java.util.List;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.artifacts.ArtifactRules;
import org.apache.sling.feature.extension.apiregions.api.artifacts.Mode;
import org.apache.sling.feature.extension.apiregions.api.artifacts.VersionRule;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;

public class CheckArtifactRules implements AnalyserTask {

    @Override
    public String getId() {
        return "artifact-rules";
    }

    @Override
    public String getName() {
        return "Artifact rules analyser task";
    }

    @Override
    public void execute(final AnalyserTaskContext context) throws Exception {
        final ArtifactRules rules = ArtifactRules.getArtifactRules(context.getFeature());
        if (rules == null) {
            context.reportExtensionWarning(
                    ArtifactRules.EXTENSION_NAME, "Artifact rules are not specified, unable to validate feature");
        } else {
            for (final BundleDescriptor bundle : context.getFeatureDescriptor().getBundleDescriptors()) {
                this.checkArtifact(
                        context,
                        rules.getBundleVersionRules(),
                        rules.getMode(),
                        bundle.getArtifact().getId());
            }
            for (final ArtifactDescriptor desc : context.getFeatureDescriptor().getArtifactDescriptors()) {
                this.checkArtifact(
                        context,
                        rules.getArtifactVersionRules(),
                        rules.getMode(),
                        desc.getArtifact().getId());
            }
        }
    }

    void checkArtifact(
            final AnalyserTaskContext context,
            final List<VersionRule> rules,
            final Mode defaultMode,
            final ArtifactId id) {
        final Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 1);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        for (final VersionRule rule : rules) {
            if (rule.getArtifactId() != null && rule.getArtifactId().isSame(id)) {
                if (!rule.isAllowed(id.getOSGiVersion())) {
                    String msg = rule.getMessage();
                    if (msg == null) {
                        msg = "Artifact with version " + id.getVersion() + " is not allowed.";
                    }
                    if (rule.getEnforceOn() != null) {
                        msg = msg.concat(" Enforce on: " + rule.getEnforceOn());
                    }
                    final boolean enforce = !rule.getEnforceOnDate().after(now);
                    Mode m = defaultMode;
                    if (rule.getMode() != null) {
                        m = rule.getMode();
                    }
                    if (m == Mode.LENIENT || !enforce) {
                        context.reportArtifactWarning(id, msg);
                    } else {
                        context.reportArtifactError(id, msg);
                    }
                }
            }
        }
    }
}
