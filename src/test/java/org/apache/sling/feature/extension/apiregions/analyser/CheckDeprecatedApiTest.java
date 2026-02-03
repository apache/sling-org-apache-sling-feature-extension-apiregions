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
        final Set<ApiExport> exports = analyser.calculateDeprecatedPackages(region, Collections.emptyMap());
        assertEquals(1, exports.size());
        final ApiExport exp = exports.iterator().next();
        assertEquals(e1.getName(), exp.getName());
        assertEquals(
                e1.getDeprecation().getPackageInfo().getMessage(),
                exp.getDeprecation().getPackageInfo().getMessage());
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
