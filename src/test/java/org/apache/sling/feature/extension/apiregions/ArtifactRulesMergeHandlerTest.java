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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.extension.apiregions.api.artifacts.ArtifactRules;
import org.apache.sling.feature.extension.apiregions.api.artifacts.Mode;
import org.apache.sling.feature.extension.apiregions.api.artifacts.VersionRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ArtifactRulesMergeHandlerTest {

    @Test
    public void testModeMerging() {
        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ArtifactRules rulesA = new ArtifactRules();
        rulesA.setMode(Mode.LENIENT);
        ArtifactRules.setArtifactRules(featureA, rulesA);
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ArtifactRules rulesB = new ArtifactRules();
        rulesB.setMode(Mode.STRICT);
        ArtifactRules.setArtifactRules(featureB, rulesB);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ArtifactRulesMergeHandler());

        final Feature result = FeatureBuilder.assemble(ArtifactId.parse("g:f:1"), context, featureA, featureB);
        final ArtifactRules rules = ArtifactRules.getArtifactRules(result);
        assertNotNull(rules);
        assertEquals(Mode.STRICT, rules.getMode());
    }

    @Test
    public void testRuleMerging() {
        final Feature featureA = new Feature(ArtifactId.parse("g:a:1"));
        final ArtifactRules rulesA = new ArtifactRules();
        final VersionRule vrA = new VersionRule();
        rulesA.getBundleVersionRules().add(vrA);
        ArtifactRules.setArtifactRules(featureA, rulesA);
        final Feature featureB = new Feature(ArtifactId.parse("g:b:1"));
        final ArtifactRules rulesB = new ArtifactRules();
        final VersionRule vrB = new VersionRule();
        rulesB.getBundleVersionRules().add(vrB);
        ArtifactRules.setArtifactRules(featureB, rulesB);

        final BuilderContext context = new BuilderContext(id -> null);
        context.addMergeExtensions(new ArtifactRulesMergeHandler());

        final Feature result = FeatureBuilder.assemble(ArtifactId.parse("g:f:1"), context, featureA, featureB);
        final ArtifactRules rules = ArtifactRules.getArtifactRules(result);
        assertNotNull(rules);
        assertEquals(2, rules.getBundleVersionRules().size());
    }
}
