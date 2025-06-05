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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.artifacts.ArtifactRules;
import org.apache.sling.feature.extension.apiregions.api.artifacts.Mode;
import org.apache.sling.feature.extension.apiregions.api.artifacts.VersionRule;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class CheckArtifactRulesTest {

    private CheckArtifactRules analyser = new CheckArtifactRules();

    private AnalyserTaskContext newContext(final Feature f) {
        final AnalyserTaskContext context = Mockito.mock(AnalyserTaskContext.class);

        when(context.getFeature()).thenReturn(f);
        final FeatureDescriptor fd = new FeatureDescriptorImpl(f);
        when(context.getFeatureDescriptor()).thenReturn(fd);

        for (final Artifact b : f.getBundles()) {
            final BundleDescriptor bd = Mockito.mock(BundleDescriptor.class);
            when(bd.getArtifact()).thenReturn(b);
            fd.getBundleDescriptors().add(bd);
        }

        for (final Extension ext : f.getExtensions()) {
            if (ext.getType() == ExtensionType.ARTIFACTS) {
                for (final Artifact a : ext.getArtifacts()) {
                    final ArtifactDescriptor bd = Mockito.mock(ArtifactDescriptor.class);
                    when(bd.getArtifact()).thenReturn(a);
                    fd.getArtifactDescriptors().add(bd);
                }
            }
        }

        return context;
    }

    @Test
    public void testValidateFeatureNoRules() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));

        final AnalyserTaskContext context = newContext(f);
        analyser.execute(context);

        Mockito.verify(context, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(context, Mockito.atLeastOnce())
                .reportExtensionWarning(Mockito.eq(ArtifactRules.EXTENSION_NAME), Mockito.anyString());
    }

    @Test
    public void testValidateFeature() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));
        final Artifact bundle = new Artifact(ArtifactId.parse("g:b:1.1"));
        f.getBundles().add(bundle);

        final Extension ext = new Extension(ExtensionType.ARTIFACTS, "artifacts", ExtensionState.OPTIONAL);
        f.getExtensions().add(ext);
        final Artifact artifact = new Artifact(ArtifactId.parse("g:c:3.0"));
        ext.getArtifacts().add(artifact);

        final VersionRule r = new VersionRule();
        r.setArtifactId(bundle.getId());
        r.setMode(Mode.STRICT);
        r.setMessage("foo");

        final VersionRule r2 = new VersionRule();
        r2.setArtifactId(artifact.getId());
        r2.setMode(Mode.STRICT);
        r2.setMessage("bar");

        final ArtifactRules rules = new ArtifactRules();
        rules.getBundleVersionRules().add(r);
        rules.getArtifactVersionRules().add(r2);

        ArtifactRules.setArtifactRules(f, rules);
        final AnalyserTaskContext context = newContext(f);
        analyser.execute(context);

        Mockito.verify(context, Mockito.atLeastOnce())
                .reportArtifactError(Mockito.eq(bundle.getId()), Mockito.eq(r.getMessage()));
        Mockito.verify(context, Mockito.atLeastOnce())
                .reportArtifactError(Mockito.eq(artifact.getId()), Mockito.eq(r2.getMessage()));
    }

    @Test
    public void testValidateFeatureEnforceOnNotReached() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));
        final Artifact bundle = new Artifact(ArtifactId.parse("g:b:1.1"));
        f.getBundles().add(bundle);

        final Calendar tomorrow = Calendar.getInstance();
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        final VersionRule r = new VersionRule();
        r.setArtifactId(bundle.getId());
        r.setMode(Mode.STRICT);
        r.setMessage("foo");
        r.setEnforceOn(df.format(tomorrow.getTime()));

        final ArtifactRules rules = new ArtifactRules();
        rules.getBundleVersionRules().add(r);

        ArtifactRules.setArtifactRules(f, rules);
        final AnalyserTaskContext context = newContext(f);
        analyser.execute(context);

        final String reportMsg = r.getMessage().concat(" Enforce on: ").concat(r.getEnforceOn());
        Mockito.verify(context, Mockito.atLeastOnce())
                .reportArtifactWarning(Mockito.eq(bundle.getId()), Mockito.eq(reportMsg));
    }

    @Test
    public void testValidateFeatureEnforceOnReached() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));
        final Artifact bundle = new Artifact(ArtifactId.parse("g:b:1.1"));
        f.getBundles().add(bundle);

        final Calendar tomorrow = Calendar.getInstance();
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        tomorrow.add(Calendar.DAY_OF_MONTH, -5);
        final VersionRule r = new VersionRule();
        r.setArtifactId(bundle.getId());
        r.setMode(Mode.STRICT);
        r.setMessage("foo");
        r.setEnforceOn(df.format(tomorrow.getTime()));

        final ArtifactRules rules = new ArtifactRules();
        rules.getBundleVersionRules().add(r);

        ArtifactRules.setArtifactRules(f, rules);
        final AnalyserTaskContext context = newContext(f);
        analyser.execute(context);

        final String reportMsg = r.getMessage().concat(" Enforce on: ").concat(r.getEnforceOn());
        Mockito.verify(context, Mockito.atLeastOnce())
                .reportArtifactError(Mockito.eq(bundle.getId()), Mockito.eq(reportMsg));

        // test again with date of today
        r.setEnforceOn(df.format(Calendar.getInstance().getTime()));
        ArtifactRules.setArtifactRules(f, rules);
        final AnalyserTaskContext context2 = newContext(f);
        analyser.execute(context2);

        final String reportMsg2 = r.getMessage().concat(" Enforce on: ").concat(r.getEnforceOn());
        Mockito.verify(context2, Mockito.atLeastOnce())
                .reportArtifactError(Mockito.eq(bundle.getId()), Mockito.eq(reportMsg2));
    }
}
