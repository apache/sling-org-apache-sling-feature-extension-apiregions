/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions.launcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;

/**
 * The {@code RegionLauncher} class extends {@link FrameworkLauncher} and is responsible for preparing
 * and running the framework with specific configurations related to API regions.
 *
 * <p>This class handles the creation and management of properties files that map bundle IDs to their
 * symbolic names and versions, as well as bundle IDs to features. These properties files are used
 * during the framework launch process.</p>
 *
 * <p>It overrides the {@code prepare} and {@code run} methods from {@link FrameworkLauncher} to
 * include additional setup steps specific to API regions.</p>
 *
 * <p>Constants:</p>
 * <ul>
 *   <li>{@code IDBSNVER_FILENAME} - The filename for the properties file that maps bundle IDs to their symbolic names and versions.</li>
 *   <li>{@code BUNDLE_FEATURE_FILENAME} - The filename for the properties file that maps bundle IDs to features.</li>
 * </ul>
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>{@code prepare(LauncherPrepareContext context, ArtifactId frameworkId, Feature app)} - Prepares the framework by creating necessary properties files and updating framework properties.</li>
 *   <li>{@code run(LauncherRunContext context, ClassLoader cl)} - Runs the framework with the given context and class loader.</li>
 * </ul>
 *
 * @see FrameworkLauncher
 * @see LauncherPrepareContext
 * @see LauncherRunContext
 * @see ArtifactId
 * @see Feature
 * @see LauncherProperties
 * @see RegionLauncherExtension
 */
public class RegionLauncher extends FrameworkLauncher {

    public static final String IDBSNVER_FILENAME = "idbsnver.properties";
    public static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";

    @Override
    public void prepare(LauncherPrepareContext context, ArtifactId frameworkId, Feature app) throws Exception {
        super.prepare(context, frameworkId, app);

        ArtifactProvider artifactProvider = id -> {
            try {
                return context.getArtifactFile(id);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        // try to get base directory created by region launcher extension
        final String regionFileName = app.getFrameworkProperties().get(LauncherProperties.PROPERTY_PREFIX.concat(RegionLauncherExtension.FEATURE_REGION_FILENAME));
        final File base;
        if (regionFileName != null) {
            base = new File(new URL(regionFileName).toURI()).getParentFile();
        } else {
            base = Files.createTempDirectory("apiregions").toFile();
        }

        final File idbsnverFile = new File(base, IDBSNVER_FILENAME);
        final File bundlesFile = new File(base, BUNDLE_FEATURE_FILENAME);

        LauncherProperties.save(LauncherProperties.getBundleIDtoBSNandVersionMap(app, artifactProvider), idbsnverFile);
        LauncherProperties.save(LauncherProperties.getBundleIDtoFeaturesMap(app), bundlesFile);

        app.getFrameworkProperties().put(LauncherProperties.PROPERTY_PREFIX.concat(IDBSNVER_FILENAME), idbsnverFile.toURI().toURL().toString());
        app.getFrameworkProperties().put(LauncherProperties.PROPERTY_PREFIX.concat(BUNDLE_FEATURE_FILENAME), bundlesFile.toURI().toURL().toString());
    }

    @Override
    public int run(LauncherRunContext context, ClassLoader cl) throws Exception {
        return super.run(context, cl);
    }
}
