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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiExport;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckApiRegionsCrossFeatureDups extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return ApiRegions.EXTENSION_NAME + "-crossfeature-dups";
    }

    @Override
    public String getName() {
        return "Api Regions cross-feature duplicate export task";
    }

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        Set<String> checkedRegions = splitListConfig(ctx.getConfiguration().get("regions"));
        Set<String> ignoredPackages = splitListConfig(ctx.getConfiguration().get("ignoredPackages"));
        Set<String> warningPackages = splitListConfig(ctx.getConfiguration().get("warningPackages"));
        Set<String> definingFeatures = splitListConfig(ctx.getConfiguration().get("definingFeatures"));

        Map<String, Set<String>> regionExports = new HashMap<>();
        Set<ArtifactId> apiRegionsFeatures = new HashSet<>();

        for (ApiRegion r : apiRegions.listRegions()) {
            apiRegionsFeatures.addAll(Arrays.asList(r.getFeatureOrigins()));
            if (checkedRegions.isEmpty() || checkedRegions.contains(r.getName())) {
                Set<String> exports = regionExports.get(r.getName());
                if (exports == null) {
                    exports = new HashSet<>();
                    regionExports.put(r.getName(), exports);
                }
                exports.addAll(r.listExports().stream().map(ApiExport::getName).collect(Collectors.toSet()));
            }
        }

        if (definingFeatures.isEmpty()) {
            definingFeatures =
                    apiRegionsFeatures.stream().map(ArtifactId::toMvnId).collect(Collectors.toSet());
        }

        FeatureDescriptor f = ctx.getFeatureDescriptor();
        for (BundleDescriptor bd : f.getBundleDescriptors()) {
            List<ArtifactId> borgs =
                    new ArrayList<>(Arrays.asList(bd.getArtifact().getFeatureOrigins()));
            removeDefiningFeatures(definingFeatures, borgs);

            if (!borgs.isEmpty()) {
                Set<String> reportedPackages = new HashSet<>();
                for (PackageInfo pi : bd.getExportedPackages()) {
                    String pkgName = pi.getName();
                    for (Map.Entry<String, Set<String>> entry : regionExports.entrySet()) {
                        if (entry.getValue().contains(pkgName) && !reportedPackages.contains(pkgName)) {
                            if (matchesSet(pkgName, ignoredPackages)) {
                                continue;
                            }

                            if (allOtherExportersNonDefining(pi, f, definingFeatures)) {
                                // If all exports are done by non-defining features then that's ok
                                continue;
                            }

                            reportedPackages.add(pi.getName());

                            String msg = "Package overlap found between region " + entry.getKey()
                                    + " and bundle " + bd.getBundleSymbolicName() + " " + bd.getBundleVersion()
                                    + " which comes from feature: " + borgs
                                    + ". Both export package: " + pi.getName();
                            if (matchesSet(pkgName, warningPackages)) {
                                ctx.reportArtifactWarning(bd.getArtifact().getId(), msg);
                            } else {
                                ctx.reportArtifactError(bd.getArtifact().getId(), msg);
                            }
                        }
                    }
                }
            }
        }
    }

    // Check if all exports of this package are done by non-defining features
    private boolean allOtherExportersNonDefining(PackageInfo pi, FeatureDescriptor f, Set<String> definingFeatures) {
        List<ArtifactId> declaringFeatures = new ArrayList<>();

        for (BundleDescriptor bd : f.getBundleDescriptors()) {
            if (bd.getExportedPackages().contains(pi)) {
                declaringFeatures.addAll(Arrays.asList(
                        bd.getArtifact().getFeatureOrigins(f.getFeature().getId())));
            }
        }

        for (ArtifactId feature : declaringFeatures) {
            for (String definingFeature : definingFeatures) {
                if (definingFeature.endsWith("*")) {
                    String prefix = definingFeature.substring(0, definingFeature.length() - 1);
                    if (feature.toMvnId().startsWith(prefix)) {
                        return false;
                    }
                } else {
                    if (feature.toMvnId().equals(definingFeature)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void removeDefiningFeatures(Set<String> definingFeatures, List<ArtifactId> features) {
        for (Iterator<ArtifactId> it = features.iterator(); it.hasNext(); ) {
            ArtifactId feature = it.next();
            for (String definingFeature : definingFeatures) {
                if (definingFeature.endsWith("*")) {
                    String prefix = definingFeature.substring(0, definingFeature.length() - 1);
                    if (feature.toMvnId().startsWith(prefix)) {
                        it.remove();
                    }
                } else {
                    if (feature.toMvnId().equals(definingFeature)) {
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean matchesSet(String pkg, Set<String> set) {
        for (String e : set) {
            if (e.endsWith("*")) {
                if (pkg.startsWith(e.substring(0, e.length() - 1))) {
                    return true;
                }
            } else {
                if (pkg.equals(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> splitListConfig(String value) {
        if (value == null) {
            return Collections.emptySet();
        } else {
            return Arrays.asList(value.split(",")).stream().map(String::trim).collect(Collectors.toSet());
        }
    }
}
