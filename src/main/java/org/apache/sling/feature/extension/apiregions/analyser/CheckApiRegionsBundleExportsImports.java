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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckApiRegionsBundleExportsImports implements AnalyserTask {

    private static final String IGNORE_API_REGIONS_CONFIG_KEY = "ignoreAPIRegions";
    private static final String GLOBAL_REGION = "global";
    private static final String NO_REGION = " __NO_REGION__ ";
    private static final String OWN_FEATURE = " __OWN_FEATURE__ ";

    @Override
    public String getName() {
        return "Bundle Import/Export Check";
    }

    @Override
    public String getId() {
        return ApiRegions.EXTENSION_NAME + "-exportsimports";
    }

    public static final class Report {

        public List<PackageInfo> exportMatchingSeveral = new ArrayList<>();

        public List<PackageInfo> missingExports = new ArrayList<>();

        public List<PackageInfo> missingExportsWithVersion = new ArrayList<>();

        public List<PackageInfo> missingExportsForOptional = new ArrayList<>();

        public Map<PackageInfo, Map.Entry<Set<String>, Set<String>>> regionInfo = new HashMap<>();
    }

    private Report getReport(final Map<BundleDescriptor, Report> reports, final BundleDescriptor info) {
        return reports.computeIfAbsent(info, key -> new Report());
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        boolean ignoreAPIRegions = ctx.getConfiguration()
                .getOrDefault(IGNORE_API_REGIONS_CONFIG_KEY, "false")
                .equalsIgnoreCase("true");

        final Map<BundleDescriptor, Report> reports = new HashMap<>();

        final SortedMap<Integer, List<BundleDescriptor>> bundlesMap = new TreeMap<>();
        for (final BundleDescriptor bi : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            final List<BundleDescriptor> list =
                    bundlesMap.computeIfAbsent(bi.getArtifact().getStartOrder(), key -> new ArrayList<>());
            list.add(bi);
        }

        // add all system packages
        final List<BundleDescriptor> exportingBundles = new ArrayList<>();
        if (ctx.getFrameworkDescriptor() != null) {
            exportingBundles.add(ctx.getFrameworkDescriptor());
        }

        // extract and check the api-regions
        ApiRegions apiRegions = null;
        try {
            apiRegions = ApiRegions.getApiRegions(ctx.getFeature());
        } catch (final IllegalArgumentException e) {
            ctx.reportError("API Region does not represent a valid JSON 'api-regions': " + e.getMessage());
            return;
        }
        if (apiRegions == null) {
            apiRegions = new ApiRegions(); // Empty region as default
        }

        for (final Map.Entry<Integer, List<BundleDescriptor>> entry : bundlesMap.entrySet()) {
            // first add all exporting bundles
            for (final BundleDescriptor info : entry.getValue()) {
                if (!info.getExportedPackages().isEmpty()) {
                    exportingBundles.add(info);
                }
            }
            // check importing bundles
            for (final BundleDescriptor info : entry.getValue()) {
                for (final PackageInfo pck : info.getImportedPackages()) {
                    final Map<BundleDescriptor, Set<String>> candidates =
                            getCandidates(exportingBundles, pck, info, apiRegions, ignoreAPIRegions);
                    if (candidates.isEmpty()) {
                        if (pck.isOptional()) {
                            getReport(reports, info).missingExportsForOptional.add(pck);
                        } else {
                            getReport(reports, info).missingExports.add(pck);
                        }
                    } else {
                        final List<BundleDescriptor> matchingCandidates = new ArrayList<>();

                        Set<String> exportingRegions = new HashSet<>();
                        Set<String> importingRegions = new HashSet<>();
                        for (final Map.Entry<BundleDescriptor, Set<String>> candidate : candidates.entrySet()) {
                            BundleDescriptor bd = candidate.getKey();
                            if (bd.isExportingPackage(pck)) {
                                Set<String> exRegions = candidate.getValue();
                                if (exRegions.contains(NO_REGION)) {
                                    // If an export is defined outside of a region, it always matches
                                    matchingCandidates.add(bd);
                                    continue;
                                }
                                if (exRegions.contains(GLOBAL_REGION)) {
                                    // Everyone can import from the global regin
                                    matchingCandidates.add(bd);
                                    continue;
                                }
                                if (exRegions.contains(OWN_FEATURE)) {
                                    // A feature can always import packages from bundles in itself
                                    matchingCandidates.add(bd);
                                    continue;
                                }

                                // Find out what regions the importing bundle is in
                                Set<String> imRegions = getBundleRegions(info, apiRegions, ignoreAPIRegions);

                                // Record the exporting and importing regions for diagnostics
                                exportingRegions.addAll(exRegions);

                                Set<String> regions = new HashSet<>();
                                for (String region : imRegions) {
                                    for (ApiRegion r = apiRegions.getRegionByName(region);
                                            r != null;
                                            r = r.getParent()) {
                                        regions.add(r.getName());
                                    }
                                }

                                importingRegions.addAll(regions);

                                // Only keep the regions that also export the package
                                regions.retainAll(exRegions);

                                if (!regions.isEmpty()) {
                                    // there is an overlapping region
                                    matchingCandidates.add(bd);
                                }
                            }
                        }

                        if (matchingCandidates.isEmpty()) {
                            if (pck.isOptional()) {
                                getReport(reports, info)
                                        .missingExportsForOptional
                                        .add(pck);
                            } else {
                                getReport(reports, info)
                                        .missingExportsWithVersion
                                        .add(pck);
                                getReport(reports, info)
                                        .regionInfo
                                        .put(pck, new AbstractMap.SimpleEntry<>(exportingRegions, importingRegions));
                            }
                        } else if (matchingCandidates.size() > 1) {
                            getReport(reports, info).exportMatchingSeveral.add(pck);
                        }
                    }
                }
            }
        }

        boolean errorReported = false;
        for (final Map.Entry<BundleDescriptor, Report> entry : reports.entrySet()) {
            final String key = "Bundle " + entry.getKey().getArtifact().getId().getArtifactId() + ":"
                    + entry.getKey().getArtifact().getId().getVersion();

            if (!entry.getValue().missingExports.isEmpty()) {
                ctx.reportArtifactError(
                        entry.getKey().getArtifact().getId(),
                        key + " is importing package(s) " + getPackageInfo(entry.getValue().missingExports, false)
                                + " in start level "
                                + String.valueOf(entry.getKey().getArtifact().getStartOrder())
                                + " but no bundle is exporting these for that start level.");
                errorReported = true;
            }
            if (!entry.getValue().missingExportsWithVersion.isEmpty()) {
                StringBuilder message = new StringBuilder(
                        key + " is importing package(s) "
                                + getPackageInfo(entry.getValue().missingExportsWithVersion, true) + " in start level "
                                + String.valueOf(entry.getKey().getArtifact().getStartOrder())
                                + " but no visible bundle is exporting these for that start level in the required version range.");

                for (Map.Entry<PackageInfo, Map.Entry<Set<String>, Set<String>>> regionInfoEntry :
                        entry.getValue().regionInfo.entrySet()) {
                    PackageInfo pkg = regionInfoEntry.getKey();
                    Map.Entry<Set<String>, Set<String>> regions = regionInfoEntry.getValue();
                    if (regions.getKey().size() > 0) {
                        message.append("\n" + pkg.getName() + " is exported in regions " + regions.getKey()
                                + " but it is imported in regions " + regions.getValue());
                    }
                }
                ctx.reportArtifactError(entry.getKey().getArtifact().getId(), message.toString());
                errorReported = true;
            }
        }
        if (errorReported && ctx.getFeature().isComplete()) {
            ctx.reportError(ctx.getFeature().getId().toMvnId() + " is marked as 'complete' but has missing imports.");
        }
    }

    private Set<String> getBundleRegions(BundleDescriptor info, ApiRegions regions, boolean ignoreAPIRegions) {
        Set<String> result = ignoreAPIRegions
                ? Collections.emptySet()
                : Stream.of(info.getArtifact().getFeatureOrigins())
                        .map(regions::getRegionsByFeature)
                        .flatMap(Stream::of)
                        .map(ApiRegion::getName)
                        .collect(Collectors.toSet());

        if (result.isEmpty()) {
            result = new HashSet<>();
            result.add(NO_REGION);
        }
        return result;
    }

    private String getPackageInfo(final List<PackageInfo> pcks, final boolean includeVersion) {
        if (pcks.size() == 1) {
            if (includeVersion) {
                return pcks.get(0).toString();
            } else {
                return pcks.get(0).getName();
            }
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append('[');
        for (final PackageInfo info : pcks) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (includeVersion) {
                sb.append(info.toString());
            } else {
                sb.append(info.getName());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private Map<BundleDescriptor, Set<String>> getCandidates(
            final List<BundleDescriptor> exportingBundles,
            final PackageInfo pck,
            final BundleDescriptor requestingBundle,
            final ApiRegions apiRegions,
            boolean ignoreAPIRegions) {
        Set<String> rf = ignoreAPIRegions
                ? Collections.emptySet()
                : Stream.of(requestingBundle.getArtifact().getFeatureOrigins())
                        .map(ArtifactId::toMvnId)
                        .collect(Collectors.toSet());

        final Set<String> requestingFeatures = rf;

        final Map<BundleDescriptor, Set<String>> candidates = new HashMap<>();
        for (final BundleDescriptor info : exportingBundles) {
            if (info.isExportingPackage(pck.getName())) {
                Set<String> providingFeatures = ignoreAPIRegions
                        ? Collections.emptySet()
                        : Stream.of(info.getArtifact().getFeatureOrigins())
                                .map(ArtifactId::toMvnId)
                                .collect(Collectors.toSet());

                // Compute the intersection without modifying the sets
                Set<String> intersection = providingFeatures.stream()
                        .filter(s -> requestingFeatures.contains(s))
                        .collect(Collectors.toSet());
                if (!intersection.isEmpty()) {
                    // A requesting bundle can see all exported packages inside its own feature
                    candidates.put(info, Collections.singleton(OWN_FEATURE));
                    continue;
                }

                for (String region : getBundleRegions(info, apiRegions, ignoreAPIRegions)) {
                    if (!NO_REGION.equals(region)
                            && (apiRegions.getRegionByName(region) == null
                                    || apiRegions.getRegionByName(region).getAllExportByName(pck.getName()) == null))
                        continue;

                    final Set<String> regions = candidates.computeIfAbsent(info, key -> new HashSet<>());
                    regions.add(region);
                }
            }
        }
        return candidates;
    }
}
