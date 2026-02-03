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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiExport;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.extension.apiregions.api.DeprecationInfo;
import org.apache.sling.feature.extension.apiregions.api.DeprecationValidationMode;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class CheckDeprecatedApi implements AnalyserTask {

    public static final String CFG_REGIONS = "regions";

    private static final String CFG_STRICT = "strict";

    private static final String CFG_REMOVAL_PERIOD = "removal-period";

    private static final String CFG_CHECK_OPTIONAL_IMPORTS = "check-optional-imports";

    private static final String PROP_VERSION = "version";

    @Override
    public String getId() {
        return "region-deprecated-api";
    }

    @Override
    public String getName() {
        return "Region Deprecated API analyser task";
    }

    @Override
    public void execute(final AnalyserTaskContext context) throws Exception {
        final ApiRegions regions = ApiRegions.getApiRegions(context.getFeature());
        if (regions == null) {
            context.reportExtensionError(ApiRegions.EXTENSION_NAME, "No regions configured");
        } else {
            final Map<BundleDescriptor, Set<String>> bundleRegions = this.calculateBundleRegions(context, regions);
            final boolean strict =
                    Boolean.parseBoolean(context.getConfiguration().getOrDefault(CFG_STRICT, "false"));
            final Integer removalPeriod =
                    Integer.parseInt(context.getConfiguration().getOrDefault(CFG_REMOVAL_PERIOD, "-1"));
            final boolean checkOptionalImports =
                    Boolean.parseBoolean(context.getConfiguration().getOrDefault(CFG_CHECK_OPTIONAL_IMPORTS, "false"));
            final String regionNames = context.getConfiguration().getOrDefault(CFG_REGIONS, ApiRegion.GLOBAL);
            for (final String r : regionNames.split(",")) {
                final ApiRegion region = regions.getRegionByName(r.trim());
                if (region == null) {
                    context.reportExtensionError(ApiRegions.EXTENSION_NAME, "Region not found:" + r.trim());
                } else {
                    checkBundlesForRegion(context, region, bundleRegions, strict, removalPeriod, checkOptionalImports);
                }
            }
        }
    }

    private Map<BundleDescriptor, Set<String>> calculateBundleRegions(AnalyserTaskContext context, ApiRegions regions) {
        final Map<BundleDescriptor, Set<String>> result = new LinkedHashMap<>();
        for (final BundleDescriptor bd : context.getFeatureDescriptor().getBundleDescriptors()) {
            final Set<String> regionNames = getBundleRegions(bd, regions);
            result.put(bd, regionNames);
        }
        return result;
    }

    private void checkBundlesForRegion(
            final AnalyserTaskContext context,
            final ApiRegion region,
            final Map<BundleDescriptor, Set<String>> bundleRegions,
            final boolean strict,
            final int removalPeriod,
            final boolean checkOptionalImports) {
        final Calendar checkDate;
        if (removalPeriod > 0) {
            checkDate = Calendar.getInstance();
            checkDate.set(Calendar.HOUR_OF_DAY, 23);
            checkDate.set(Calendar.MINUTE, 59);
            checkDate.add(Calendar.DAY_OF_YEAR, removalPeriod);
        } else {
            checkDate = null;
        }

        final Set<ApiExport> exports = this.calculateDeprecatedPackages(region, bundleRegions);

        final Set<String> allowedNames = getAllowedRegions(region);

        for (final BundleDescriptor bd : context.getFeatureDescriptor().getBundleDescriptors()) {
            if (isInAllowedRegion(bundleRegions.get(bd), region.getName(), allowedNames)) {
                for (final PackageInfo pi : bd.getImportedPackages()) {
                    if (!checkOptionalImports && pi.isOptional()) {
                        continue;
                    }
                    final VersionRange importRange = pi.getPackageVersionRange();
                    DeprecationInfo deprecationInfo = null;
                    for (final ApiExport exp : exports) {
                        if (pi.getName().equals(exp.getName())) {
                            String version = exp.getProperties().get(PROP_VERSION);
                            if (version == null || importRange == null || importRange.includes(new Version(version))) {
                                deprecationInfo = exp.getDeprecation().getPackageInfo();
                                break;
                            }
                        }
                    }
                    if (deprecationInfo != null) {
                        String msg = "Usage of deprecated package found : "
                                .concat(pi.getName())
                                .concat(" : ")
                                .concat(deprecationInfo.getMessage());
                        if (deprecationInfo.getSince() != null) {
                            msg = msg.concat(" Deprecated since ").concat(deprecationInfo.getSince());
                        }
                        boolean isError;
                        if (deprecationInfo.getMode() != null) {
                            isError = deprecationInfo.getMode() == DeprecationValidationMode.STRICT;
                        } else {
                            isError = strict;
                        }
                        if (deprecationInfo.isForRemoval()) {
                            boolean printRemoval = true;
                            if (checkDate != null) {
                                final Calendar c = deprecationInfo.getForRemovalBy();
                                if (c != null && c.before(checkDate)) {
                                    isError = true;
                                    printRemoval = false;
                                    msg = msg.concat(" The package is scheduled to be removed in less than ")
                                            .concat(String.valueOf(removalPeriod))
                                            .concat(" days by ")
                                            .concat(deprecationInfo.getForRemoval());
                                }
                            }
                            if (printRemoval) {
                                msg = msg.concat(" For removal : ").concat(deprecationInfo.getForRemoval());
                            }
                        }
                        if (isError) {
                            context.reportArtifactError(bd.getArtifact().getId(), msg);
                        } else {
                            context.reportArtifactWarning(bd.getArtifact().getId(), msg);
                        }
                    }
                }
            }
        }
    }

    boolean isInAllowedRegion(
            final Set<String> bundleRegions, final String regionName, final Set<String> allowedRegions) {
        if (bundleRegions.contains(regionName)) {
            for (final String name : bundleRegions) {
                if (!allowedRegions.contains(name)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    Set<String> getAllowedRegions(final ApiRegion region) {
        final Set<String> allowedNames = new HashSet<>();
        ApiRegion r = region;
        while (r != null) {
            allowedNames.add(r.getName());
            r = r.getParent();
        }

        return allowedNames;
    }

    Set<ApiExport> calculateDeprecatedPackages(
            final ApiRegion region, final Map<BundleDescriptor, Set<String>> bundleRegions) {
        final Set<ApiExport> result = new LinkedHashSet<>();
        for (final ApiExport export : region.listAllExports()) {
            if (export.getDeprecation().getPackageInfo() != null) {
                final String version = getVersion(bundleRegions, region.getName(), export.getName());
                // create new ApiExport to add version
                final ApiExport clone = new ApiExport(export.getName());
                clone.getDeprecation().setPackageInfo(export.getDeprecation().getPackageInfo());
                if (version != null) {
                    clone.getProperties().put(PROP_VERSION, version);
                }
                result.add(clone);
            }
        }
        return result;
    }

    String getVersion(
            final Map<BundleDescriptor, Set<String>> bundleRegions, final String regionName, final String packageName) {
        String version = null;
        for (final Map.Entry<BundleDescriptor, Set<String>> entry : bundleRegions.entrySet()) {
            if (entry.getValue().contains(regionName)) {
                for (final PackageInfo info : entry.getKey().getExportedPackages()) {
                    if (info.getName().equals(packageName)) {
                        version = info.getVersion();
                        break;
                    }
                }
                if (version != null) {
                    break;
                }
            }
        }
        return version;
    }

    private Set<String> getBundleRegions(final BundleDescriptor info, final ApiRegions regions) {
        return Stream.of(info.getArtifact().getFeatureOrigins())
                .map(regions::getRegionsByFeature)
                .flatMap(Stream::of)
                .map(ApiRegion::getName)
                .collect(Collectors.toSet());
    }
}
