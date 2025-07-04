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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationApi;
import org.apache.sling.feature.extension.apiregions.api.config.ConfigurationDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class CheckConfigurationApiTest {

    private CheckConfigurationApi analyser = new CheckConfigurationApi();

    private AnalyserTaskContext newContext(final Feature f) {
        final AnalyserTaskContext context = Mockito.mock(AnalyserTaskContext.class);

        when(context.getFeature()).thenReturn(f);

        return context;
    }

    @Test
    public void testValidateFeature() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));

        final Configuration c = new Configuration("pid");
        c.getProperties().put("a", "x");
        f.getConfigurations().add(c);

        final ConfigurationApi api = new ConfigurationApi();
        api.getConfigurationDescriptions().put("pid", new ConfigurationDescription());
        api.getConfigurationDescriptions().get("pid").getPropertyDescriptions().put("a", new PropertyDescription());
        ConfigurationApi.setConfigurationApi(f, api);

        // no error
        AnalyserTaskContext context = newContext(f);
        analyser.execute(context);
        Mockito.verify(context, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(context, Mockito.never()).reportWarning(Mockito.anyString());
        Mockito.verify(context, Mockito.never())
                .reportConfigurationError(Mockito.any(Configuration.class), Mockito.anyString());
        Mockito.verify(context, Mockito.never())
                .reportConfigurationWarning(Mockito.any(Configuration.class), Mockito.anyString());

        // integer -> validation error
        api.getConfigurationDescriptions()
                .get("pid")
                .getPropertyDescriptions()
                .get("a")
                .setType(PropertyType.INTEGER);
        ConfigurationApi.setConfigurationApi(f, api);
        context = newContext(f);
        analyser.execute(context);
        Mockito.verify(context, Mockito.atLeastOnce())
                .reportConfigurationError(Mockito.any(Configuration.class), Mockito.anyString());
        Mockito.verify(context, Mockito.never())
                .reportConfigurationWarning(Mockito.any(Configuration.class), Mockito.anyString());
        Mockito.verify(context, Mockito.never()).reportWarning(Mockito.anyString());
        Mockito.verify(context, Mockito.never()).reportWarning(Mockito.anyString());
    }
}
