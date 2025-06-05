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

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckApiRegionsDependencies extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return ApiRegions.EXTENSION_NAME + "-dependencies";
    }

    @Override
    public String getName() {
        return "Api Regions dependecies analyser task";
    }

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        for (int i = 0; i < apiRegions.listRegions().size(); i++) {
            for (int j = i + 1; j < apiRegions.listRegions().size(); j++) {
                execute(
                        ctx,
                        apiRegions,
                        apiRegions.listRegions().get(i),
                        apiRegions.listRegions().get(j));
            }
        }
    }

    private void execute(
            AnalyserTaskContext ctx, ApiRegions apiRegions, ApiRegion exportingApisName, ApiRegion hidingApisName) {
        FeatureDescriptor featureDescriptor = ctx.getFeatureDescriptor();
        for (BundleDescriptor bundleDescriptor : featureDescriptor.getBundleDescriptors()) {
            for (PackageInfo packageInfo : bundleDescriptor.getExportedPackages()) {
                String exportedPackage = packageInfo.getName();

                if (exportingApisName.getExportByName(exportedPackage) != null) {
                    if (hidingApisName.getExportByName(exportedPackage) != null) {
                        String errorMessage = String.format(
                                "Bundle '%s' (defined in feature '%s') exports package '%s' that is declared in both visible '%s' and non-visible '%s' APIs regions",
                                bundleDescriptor.getArtifact().getId(),
                                ctx.getFeature().getId(),
                                exportedPackage,
                                exportingApisName.getName(),
                                hidingApisName.getName());
                        ctx.reportArtifactError(bundleDescriptor.getArtifact().getId(), errorMessage);
                    } else {
                        for (String uses : packageInfo.getUses()) {
                            if (hidingApisName.getExportByName(uses) != null) {
                                String errorMessage = String.format(
                                        "Bundle '%s' (defined in feature '%s') exports package '%s' that is declared in the visible '%s' region, which uses package '%s' that is in the non-visible '%s' region",
                                        bundleDescriptor.getArtifact().getId(),
                                        ctx.getFeature().getId(),
                                        exportedPackage,
                                        exportingApisName.getName(),
                                        uses,
                                        hidingApisName.getName());
                                ctx.reportArtifactError(
                                        bundleDescriptor.getArtifact().getId(), errorMessage);
                            }
                        }
                    }
                }
            }
        }
    }
}
