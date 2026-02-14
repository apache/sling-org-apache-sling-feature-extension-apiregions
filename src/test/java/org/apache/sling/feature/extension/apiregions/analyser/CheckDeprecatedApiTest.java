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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.analyser.CheckDeprecatedApi.DeprecatedPackage;
import org.apache.sling.feature.extension.apiregions.api.ApiExport;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.extension.apiregions.api.DeprecationInfo;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;

public class CheckDeprecatedApiTest {

    private static final String API_REGIONS_JSON = "[{\"name\":\"global\",\"feature-origins\":[\"g:feature:1\"],"
            + "\"exports\":[{\"name\":\"org.foo.deprecated\",\"deprecated\":\"deprecated\"}]}]";

    @Test
    public void testIsInAllowedRegion() {
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();

        final Set<String> allowedRegions = new HashSet<>(Arrays.asList("deprecated", "global"));

        assertTrue(analyser.isInAllowedRegion(
                new HashSet<>(Arrays.asList("deprecated", "global")), "deprecated", allowedRegions));
        assertFalse(analyser.isInAllowedRegion(
                new HashSet<>(Arrays.asList("deprecated", "global", "internal")), "deprecated", allowedRegions));
        assertTrue(
                analyser.isInAllowedRegion(new HashSet<>(Arrays.asList("deprecated")), "deprecated", allowedRegions));
        assertFalse(analyser.isInAllowedRegion(new HashSet<>(Arrays.asList("foo")), "deprecated", allowedRegions));
    }

    @Test
    public void testGetAllowedRegions() {
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();

        final ApiRegions regions = new ApiRegions();
        regions.add(new ApiRegion("global"));
        regions.add(new ApiRegion("deprecated"));
        regions.add(new ApiRegion("internal"));
        assertEquals(
                new HashSet<>(Arrays.asList("global")), analyser.getAllowedRegions(regions.getRegionByName("global")));
        assertEquals(
                new HashSet<>(Arrays.asList("global", "deprecated")),
                analyser.getAllowedRegions(regions.getRegionByName("deprecated")));
        assertEquals(
                new HashSet<>(Arrays.asList("global", "deprecated", "internal")),
                analyser.getAllowedRegions(regions.getRegionByName("internal")));
    }

    @Test
    public void testCalculateDeprecatedPackages() {
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();

        final ApiRegion region = new ApiRegion("global");
        final ApiExport e1 = new ApiExport("e1");
        e1.getDeprecation().setPackageInfo(new DeprecationInfo("deprecated-e1"));
        final ApiExport e2 = new ApiExport("e2");
        final ApiExport e3 = new ApiExport("e3");
        e3.getDeprecation().addMemberInfo("Foo", new DeprecationInfo("deprecated-e3"));

        region.add(e1);
        region.add(e2);
        region.add(e3);

        // only e1 should be returned
        final Map<String, DeprecatedPackage> exports =
                analyser.calculateDeprecatedPackages(region, Collections.emptyMap());
        assertEquals(1, exports.size());
        final DeprecatedPackage exp = exports.get("e1");
        assertNotNull(exp);
        assertEquals(
                e1.getDeprecation().getPackageInfo().getMessage(),
                exp.getDeprecationInfo().getMessage());
    }

    @Test
    public void testOptionalImportIgnoredByDefault() throws Exception {
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();
        final AnalyserTaskContext ctx = createContext(Collections.emptyMap(), true);

        analyser.execute(ctx);

        Mockito.verify(ctx, never()).reportArtifactWarning(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportArtifactError(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testOptionalImportReportedWhenEnabled() throws Exception {
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();
        final Map<String, String> cfg = new HashMap<>();
        cfg.put("check-optional-imports", "true");
        final AnalyserTaskContext ctx = createContext(cfg, true);

        analyser.execute(ctx);

        Mockito.verify(ctx)
                .reportArtifactWarning(
                        Mockito.eq(ArtifactId.fromMvnId("g:b:1.0.0")), Mockito.contains("org.foo.deprecated"));
    }

    @Test
    public void testVersionRangeChecking() throws Exception {
        // Test that version ranges are properly checked
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();

        // Create a feature with deprecated package exported at version 2.0.0
        final Feature feature = new Feature(ArtifactId.fromMvnId("g:feature:1"));
        final Extension extension =
                new Extension(ExtensionType.JSON, ApiRegions.EXTENSION_NAME, ExtensionState.OPTIONAL);
        extension.setJSON("[{\"name\":\"global\",\"feature-origins\":[\"g:feature:1\"],"
                + "\"exports\":[{\"name\":\"org.foo.deprecated\",\"deprecated\":\"This package is deprecated\"}]}]");
        feature.getExtensions().add(extension);

        final FeatureDescriptor fd = new FeatureDescriptorImpl(feature);

        // Bundle that exports the deprecated package at version 2.0.0
        final Artifact exportBundle = new Artifact(ArtifactId.fromMvnId("g:exporter:1.0.0"));
        exportBundle.setFeatureOrigins(feature.getId());
        final BundleDescriptor exportBd = new TestBundleDescriptor(exportBundle);
        exportBd.getExportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "2.0.0", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(exportBd);

        // Bundle that imports with version range [1.0.0,2.0.0) - should NOT be flagged
        final Artifact importBundle1 = new Artifact(ArtifactId.fromMvnId("g:importer1:1.0.0"));
        importBundle1.setFeatureOrigins(feature.getId());
        final BundleDescriptor importBd1 = new TestBundleDescriptor(importBundle1);
        final PackageInfo import1 =
                new PackageInfo("org.foo.deprecated", "[1.0.0,2.0.0)", false, Collections.emptySet());
        importBd1.getImportedPackages().add(import1);
        fd.getBundleDescriptors().add(importBd1);

        // Bundle that imports with version range [2.0.0,3.0.0) - SHOULD be flagged
        final Artifact importBundle2 = new Artifact(ArtifactId.fromMvnId("g:importer2:1.0.0"));
        importBundle2.setFeatureOrigins(feature.getId());
        final BundleDescriptor importBd2 = new TestBundleDescriptor(importBundle2);
        final PackageInfo import2 =
                new PackageInfo("org.foo.deprecated", "[2.0.0,3.0.0)", false, Collections.emptySet());
        importBd2.getImportedPackages().add(import2);
        fd.getBundleDescriptors().add(importBd2);

        // Bundle that imports without version - SHOULD be flagged (matches any version)
        final Artifact importBundle3 = new Artifact(ArtifactId.fromMvnId("g:importer3:1.0.0"));
        importBundle3.setFeatureOrigins(feature.getId());
        final BundleDescriptor importBd3 = new TestBundleDescriptor(importBundle3);
        final PackageInfo import3 = new PackageInfo("org.foo.deprecated", null, false, Collections.emptySet());
        importBd3.getImportedPackages().add(import3);
        fd.getBundleDescriptors().add(importBd3);

        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(Collections.emptyMap());

        analyser.execute(ctx);

        // importer1 should NOT be flagged (version range excludes 2.0.0)
        Mockito.verify(ctx, never())
                .reportArtifactWarning(Mockito.eq(ArtifactId.fromMvnId("g:importer1:1.0.0")), Mockito.anyString());

        // importer2 SHOULD be flagged (version range includes 2.0.0)
        Mockito.verify(ctx)
                .reportArtifactWarning(
                        Mockito.eq(ArtifactId.fromMvnId("g:importer2:1.0.0")), Mockito.contains("org.foo.deprecated"));

        // importer3 SHOULD be flagged (no version constraint)
        Mockito.verify(ctx)
                .reportArtifactWarning(
                        Mockito.eq(ArtifactId.fromMvnId("g:importer3:1.0.0")), Mockito.contains("org.foo.deprecated"));
    }

    @Test
    public void testMultipleExportVersions() throws Exception {
        // Test that when multiple versions of a deprecated package exist,
        // only imports matching the actual exported versions are flagged
        final CheckDeprecatedApi analyser = new CheckDeprecatedApi();

        final Feature feature = new Feature(ArtifactId.fromMvnId("g:feature:1"));
        final Extension extension =
                new Extension(ExtensionType.JSON, ApiRegions.EXTENSION_NAME, ExtensionState.OPTIONAL);
        extension.setJSON("[{\"name\":\"global\",\"feature-origins\":[\"g:feature:1\"],"
                + "\"exports\":[{\"name\":\"org.foo.deprecated\",\"deprecated\":\"This package is deprecated\"}]}]");
        feature.getExtensions().add(extension);

        final FeatureDescriptor fd = new FeatureDescriptorImpl(feature);

        // Bundle that exports the deprecated package at version 1.0.0
        final Artifact exportBundle1 = new Artifact(ArtifactId.fromMvnId("g:exporter1:1.0.0"));
        exportBundle1.setFeatureOrigins(feature.getId());
        final BundleDescriptor exportBd1 = new TestBundleDescriptor(exportBundle1);
        exportBd1
                .getExportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "1.0.0", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(exportBd1);

        // DIFFERENT bundle that exports the SAME package at version 3.0.0
        final Artifact exportBundle2 = new Artifact(ArtifactId.fromMvnId("g:exporter2:1.0.0"));
        exportBundle2.setFeatureOrigins(feature.getId());
        final BundleDescriptor exportBd2 = new TestBundleDescriptor(exportBundle2);
        exportBd2
                .getExportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "3.0.0", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(exportBd2);

        // Bundle that imports version [1.0.0,2.0.0) - matches only the 1.0.0 export
        final Artifact importBundle1 = new Artifact(ArtifactId.fromMvnId("g:importer1:1.0.0"));
        importBundle1.setFeatureOrigins(feature.getId());
        final BundleDescriptor importBd1 = new TestBundleDescriptor(importBundle1);
        importBd1
                .getImportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "[1.0.0,2.0.0)", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(importBd1);

        // Bundle that imports version [2.5.0,4.0.0) - matches only the 3.0.0 export
        final Artifact importBundle2 = new Artifact(ArtifactId.fromMvnId("g:importer2:1.0.0"));
        importBundle2.setFeatureOrigins(feature.getId());
        final BundleDescriptor importBd2 = new TestBundleDescriptor(importBundle2);
        importBd2
                .getImportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "[2.5.0,4.0.0)", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(importBd2);

        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(Collections.emptyMap());

        analyser.execute(ctx);

        // BOTH importers should be flagged since both versions (1.0.0 and 3.0.0) are available
        // The implementation checks all matching exported versions for this package.
        Mockito.verify(ctx)
                .reportArtifactWarning(
                        Mockito.eq(ArtifactId.fromMvnId("g:importer1:1.0.0")), Mockito.contains("org.foo.deprecated"));
        Mockito.verify(ctx)
                .reportArtifactWarning(
                        Mockito.eq(ArtifactId.fromMvnId("g:importer2:1.0.0")), Mockito.contains("org.foo.deprecated"));
    }

    private AnalyserTaskContext createContext(final Map<String, String> config, final boolean optionalImport) {
        final Feature feature = new Feature(ArtifactId.fromMvnId("g:feature:1"));
        final Extension extension =
                new Extension(ExtensionType.JSON, ApiRegions.EXTENSION_NAME, ExtensionState.OPTIONAL);
        extension.setJSON(API_REGIONS_JSON);
        feature.getExtensions().add(extension);

        final FeatureDescriptor fd = new FeatureDescriptorImpl(feature);

        final Artifact bundle = new Artifact(ArtifactId.fromMvnId("g:b:1.0.0"));
        bundle.setFeatureOrigins(feature.getId());
        final BundleDescriptor bd = new TestBundleDescriptor(bundle);
        bd.getImportedPackages()
                .add(new PackageInfo("org.foo.deprecated", "1.0", optionalImport, Collections.emptySet()));
        fd.getBundleDescriptors().add(bd);

        final Artifact apiBundle = new Artifact(ArtifactId.fromMvnId("g:c:1.0.0"));
        apiBundle.setFeatureOrigins(feature.getId());
        final BundleDescriptor apiDesc = new TestBundleDescriptor(apiBundle);
        apiDesc.getExportedPackages().add(new PackageInfo("org.foo.deprecated", "1.0", false, Collections.emptySet()));
        fd.getBundleDescriptors().add(apiDesc);

        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(config);
        return ctx;
    }

    private static final class TestBundleDescriptor extends BundleDescriptor {
        private final Artifact artifact;

        TestBundleDescriptor(final Artifact artifact) {
            super(artifact.getId().toMvnId());
            this.artifact = artifact;
        }

        @Override
        public URL getArtifactFile() {
            return null;
        }

        @Override
        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public Manifest getManifest() {
            return null;
        }

        @Override
        public String getBundleVersion() {
            return null;
        }

        @Override
        public String getBundleSymbolicName() {
            return null;
        }
    }
}
