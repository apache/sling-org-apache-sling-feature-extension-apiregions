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
package org.apache.sling.feature.extension.apiregions.launcher;

import java.io.IOException;
import java.util.Properties;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Constants;

public class LauncherPropertiesTest {
    @Test
    public void testInheritedFeature() throws IOException {
        ApiRegions apiRegions = ApiRegions.parse(
                "[" + "{\"name\": \"region1\", \"exports\": [\"org.foo.b\"],\"feature-origins\":[\"f:f1:1\"]},"
                        + "{\"name\": \"region2\", \"exports\": [\"org.foo.c\"],\"feature-origins\":[\"f:f1:1\",\"f:f2:1\"]},"
                        + "{\"name\": \"region3\", \"exports\": [],\"feature-origins\":[\"f:f2:1\"]},"
                        + "{\"name\": \"region4\", \"exports\": [\"org.foo.b\"],\"feature-origins\":[\"f:f1:1\"]}"
                        + "]");

        Properties properties = LauncherProperties.getFeatureIDtoRegionsMap(apiRegions);

        Assert.assertNotNull(properties);
        Assert.assertEquals("region1,region2,region4", properties.getProperty("f:f1:1"));
        Assert.assertEquals("region1,region2,region3", properties.getProperty("f:f2:1"));
        Assert.assertEquals("region1,region2,region3,region4", properties.getProperty("__region.order__"));
    }

    @Test
    public void testNotInheritedFeature() throws IOException {
        ApiRegions apiRegions = ApiRegions.parse(
                "[" + "{\"name\": \"region1\", \"exports\": [\"org.foo.b\"],\"feature-origins\":[\"f:f1:1\"]},"
                        + "{\"name\": \"region2\", \"exports\": [\"org.foo.c\"],\"feature-origins\":[\"f:f1:1\"]},"
                        + "{\"name\": \"region3\", \"exports\": [],\"feature-origins\":[\"f:f2:1\"]},"
                        + "{\"name\": \"region4\", \"exports\": [],\"feature-origins\":[\"f:f2:1\"]},"
                        + "{\"name\": \"region5\", \"exports\": [],\"feature-origins\":[\"f:f3:1\"]}"
                        + "]");

        Properties properties = LauncherProperties.getFeatureIDtoRegionsMap(apiRegions);

        Assert.assertNotNull(properties);
        Assert.assertEquals("region1,region2", properties.getProperty("f:f1:1"));
        Assert.assertEquals("region3,region4", properties.getProperty("f:f2:1"));
        Assert.assertEquals("region1,region2,region3,region4,region5", properties.getProperty("__region.order__"));
    }

    @Test
    public void testGetBundleIDtoFeaturesMapWithOrigins() {
        final ArtifactId featureId = ArtifactId.parse("g:f:1");
        final ArtifactId artifactId = ArtifactId.parse("g:a:2");
        final ArtifactId originId = ArtifactId.parse("g:o:5");

        final Feature f = new Feature(featureId);
        final Artifact a = new Artifact(artifactId);
        a.setFeatureOrigins(originId);
        f.getBundles().add(a);

        final Properties prop = LauncherProperties.getBundleIDtoFeaturesMap(f);
        Assert.assertEquals(1, prop.size());
        Assert.assertEquals(originId.toMvnId(), prop.get(artifactId.toMvnId()));
    }

    @Test
    public void testGetBundleIDtoFeaturesMapWithoutOrigins() {
        final ArtifactId featureId = ArtifactId.parse("g:f:1");
        final ArtifactId artifactId = ArtifactId.parse("g:a:2");

        final Feature f = new Feature(featureId);
        final Artifact a = new Artifact(artifactId);
        f.getBundles().add(a);

        final Properties prop = LauncherProperties.getBundleIDtoFeaturesMap(f);
        Assert.assertEquals(1, prop.size());
        Assert.assertEquals(featureId.toMvnId(), prop.get(artifactId.toMvnId()));
    }

    @Test
    public void testGetBundleIDtoBSNandVersionMap() {
        final ArtifactId featureId = ArtifactId.parse("g:f:1");
        final ArtifactId artifactId = ArtifactId.parse("g:a:2");

        final Feature f = new Feature(featureId);
        final Artifact a = new Artifact(artifactId);
        a.getMetadata().put(Constants.BUNDLE_SYMBOLICNAME, "my.bundle");
        a.getMetadata().put(Constants.BUNDLE_VERSION, "1.0");
        f.getBundles().add(a);

        final Properties prop = LauncherProperties.getBundleIDtoBSNandVersionMap(f, null);
        Assert.assertEquals(1, prop.size());
        Assert.assertEquals("my.bundle~1.0.0", prop.get(a.getId().toMvnId()));
    }
}
