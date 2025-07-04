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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.analyser.AnalyserResult;
import org.apache.sling.feature.analyser.AnalyserResult.ArtifactReport;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.ContainerDescriptor;
import org.apache.sling.feature.scanner.Scanner;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckApiRegionsCrossFeatureDupsTest {
    @Test
    public void testOverlapError() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Map<String, Map<String, String>> configs = Collections.singletonMap(
                "api-regions-crossfeature-dups", Collections.singletonMap("warningPackages", "x.y.z"));
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(1, res.getArtifactErrors().size());
        assertEquals(0, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());

        ArtifactReport err = res.getArtifactErrors().get(0);
        assertEquals(ArtifactId.parse("g:exp2:1"), err.getKey());
        assertTrue(err.getValue().contains("a.b.c"));
        assertTrue(err.getValue().contains("g:f3:1"));
        assertTrue(err.getValue().contains("feature-export2"));
    }

    @Test
    public void testOverlapWarning() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner();
        CheckApiRegionsCrossFeatureDups at = new CheckApiRegionsCrossFeatureDups();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("warningPackages", "a.b.c");
        cfg.put("ignoredPackages", "x.y.z");
        Map<String, Map<String, String>> configs = Collections.singletonMap("api-regions-crossfeature-dups", cfg);
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(0, res.getArtifactErrors().size());
        assertEquals(1, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());

        ArtifactReport err = res.getArtifactWarnings().get(0);
        assertEquals(ArtifactId.parse("g:exp2:1"), err.getKey());
        assertTrue(err.getValue().contains("a.b.c"));
        assertTrue(err.getValue().contains("g:f3:1"));
        assertTrue(err.getValue().contains("feature-export2"));
    }

    @Test
    public void testOverlapWarning2() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner();
        CheckApiRegionsCrossFeatureDups at = new CheckApiRegionsCrossFeatureDups();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("warningPackages", "x.*, a.*");
        Map<String, Map<String, String>> configs = Collections.singletonMap("api-regions-crossfeature-dups", cfg);
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(0, res.getArtifactErrors().size());
        assertEquals(1, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());

        ArtifactReport err = res.getArtifactWarnings().get(0);
        assertEquals(ArtifactId.parse("g:exp2:1"), err.getKey());
        assertTrue(err.getValue().contains("a.b.c"));
        assertTrue(err.getValue().contains("g:f3:1"));
        assertTrue(err.getValue().contains("feature-export2"));
    }

    @Test
    public void testOverlapIgnore() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner();
        CheckApiRegionsCrossFeatureDups at = new CheckApiRegionsCrossFeatureDups();
        Map<String, String> cfg = new HashMap<>();
        cfg.put("ignoredPackages", "a.b.c");
        Map<String, Map<String, String>> configs = Collections.singletonMap("api-regions-crossfeature-dups", cfg);
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(0, res.getArtifactErrors().size());
        assertEquals(0, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());
    }

    @Test
    public void testOverlapError2() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner2();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Analyser a = new Analyser(scanner, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(2, res.getArtifactErrors().size());
        assertEquals(0, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());

        ArtifactReport err1 = res.getArtifactErrors().get(0);
        assertEquals(ArtifactId.parse("g:extra:1"), err1.getKey());
        assertTrue(err1.getValue().contains("zzz.zzz"));

        ArtifactReport err2 = res.getArtifactErrors().get(1);
        assertEquals(ArtifactId.parse("g:exp2:1"), err2.getKey());
        assertTrue(err2.getValue().contains("a.b.c"));
    }

    @Test
    public void testOverlapErrorOnlyGlobal() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner2();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Map<String, Map<String, String>> configs = Collections.singletonMap(
                "api-regions-crossfeature-dups", Collections.singletonMap("regions", "something,global"));
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(1, res.getArtifactErrors().size());
        assertEquals(0, res.getArtifactWarnings().size());
        assertEquals(0, res.getGlobalWarnings().size());
        assertEquals(0, res.getGlobalErrors().size());

        ArtifactReport err1 = res.getArtifactErrors().get(0);
        assertEquals(ArtifactId.parse("g:extra:1"), err1.getKey());
        assertTrue(err1.getValue().contains("zzz.zzz"));
    }

    @Test
    public void testDefiningFeatures() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm2.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner3();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Map<String, Map<String, String>> configs = Collections.singletonMap(
                "api-regions-crossfeature-dups", Collections.singletonMap("definingFeatures", "g:f1:1"));
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);

        assertEquals(1, res.getErrors().size());
        assertEquals(0, res.getWarnings().size());

        String err = res.getErrors().get(0);
        assertTrue(err.contains("org.foo.bar.bingo"));
        assertTrue(err.contains("g:f2:1"));
        assertTrue(err.contains("feature-export"));
    }

    @Test
    public void testDefiningFeaturesRegex() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm2.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner3();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Map<String, Map<String, String>> configs = Collections.singletonMap(
                "api-regions-crossfeature-dups", Collections.singletonMap("definingFeatures", "g:f1:*"));
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);

        assertEquals(1, res.getErrors().size());
        assertEquals(0, res.getWarnings().size());

        String err = res.getErrors().get(0);
        assertTrue(err.contains("org.foo.bar.bingo"));
        assertTrue(err.contains("g:f2:1"));
        assertTrue(err.contains("feature-export"));
    }

    @Test
    public void testMultipleExportsFromNondefiningFeatures() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm3.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner3();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Map<String, Map<String, String>> configs = Collections.singletonMap(
                "api-regions-crossfeature-dups", Collections.singletonMap("definingFeatures", "g:f2:*"));
        Analyser a = new Analyser(scanner, configs, at);
        AnalyserResult res = a.analyse(f);

        assertEquals(0, res.getErrors().size());
        assertEquals(0, res.getWarnings().size());
    }

    private Scanner getScanner() throws IOException {
        ArtifactProvider ap = new ArtifactProvider() {
            @Override
            public URL provide(ArtifactId id) {
                switch (id.toMvnId()) {
                    case "g:exp1:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export.jar");
                    case "g:exp2:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export2.jar");
                    case "g:noexp:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/no-exports.jar");
                    case "g:extra:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/no-exports.jar");
                    case "g:extra:2":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export4.jar");
                }
                return null;
            }
        };
        Scanner scanner = new Scanner(ap, Collections.singletonList(new RegionScanner()), Collections.emptyList());
        return scanner;
    }

    private Scanner getScanner2() throws IOException {
        ArtifactProvider ap = new ArtifactProvider() {
            @Override
            public URL provide(ArtifactId id) {
                switch (id.toMvnId()) {
                    case "g:exp1:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export.jar");
                    case "g:exp2:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export2.jar");
                    case "g:noexp:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/no-exports.jar");
                    case "g:extra:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export3.jar");
                    case "g:extra:2":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export4.jar");
                }
                return null;
            }
        };
        Scanner scanner = new Scanner(ap, Collections.singletonList(new RegionScanner()), Collections.emptyList());
        return scanner;
    }

    private Scanner getScanner3() throws IOException {
        ArtifactProvider ap = new ArtifactProvider() {
            @Override
            public URL provide(ArtifactId id) {
                switch (id.toMvnId()) {
                    case "g:exp0:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export.jar");
                    case "g:exp1:1":
                        return getClass().getResource("/crossfeatdups/test-bundles/feature-export1.jar");
                }
                return null;
            }
        };
        Scanner scanner = new Scanner(ap, Collections.singletonList(new RegionScanner()), Collections.emptyList());
        return scanner;
    }

    private static final class RegionScanner implements ExtensionScanner {

        @Override
        public String getId() {
            return "region";
        }

        @Override
        public String getName() {
            return "region";
        }

        @Override
        public ContainerDescriptor scan(Feature arg0, Extension arg1, ArtifactProvider arg2) throws IOException {
            if (arg1.getName().equals(ApiRegions.EXTENSION_NAME)) {
                final ContainerDescriptor desc = new ContainerDescriptor(arg1.getName()) {};

                desc.lock();
                return desc;
            }
            return null;
        }
    }
}
