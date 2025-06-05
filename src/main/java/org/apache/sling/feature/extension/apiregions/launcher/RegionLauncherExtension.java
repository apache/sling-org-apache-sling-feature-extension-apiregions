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

import java.io.File;
import java.nio.file.Files;

import jakarta.json.JsonArray;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

/**
 * The {@code RegionLauncherExtension} class implements the {@code ExtensionHandler} interface
 * to handle the API regions extension in the Sling feature model.
 *
 * <p>This class processes the API regions extension, generates temporary files for feature
 * regions and region packages, and adds the corresponding framework properties to the extension context.</p>
 *
 * <p>It defines two constants for the filenames of the generated properties files:</p>
 * <ul>
 *   <li>{@link #FEATURE_REGION_FILENAME} - The filename for the feature regions properties file.</li>
 *   <li>{@link #REGION_PACKAGE_FILENAME} - The filename for the region packages properties file.</li>
 * </ul>
 *
 * <p>The {@code handle} method performs the following steps:</p>
 * <ol>
 *   <li>Checks if the extension name matches the expected API regions extension name.</li>
 *   <li>Creates a temporary directory for storing the generated properties files.</li>
 *   <li>Parses the API regions from the extension's JSON structure.</li>
 *   <li>Saves the feature regions and region packages mappings to the respective properties files.</li>
 *   <li>Adds the generated properties files as framework properties to the extension context.</li>
 * </ol>
 *
 * @see ExtensionHandler
 * @see ExtensionContext
 * @see Extension
 * @see ApiRegions
 * @see LauncherProperties
 */
public class RegionLauncherExtension implements ExtensionHandler {

    public static final String FEATURE_REGION_FILENAME = "features.properties";
    public static final String REGION_PACKAGE_FILENAME = "regions.properties";

    @Override
    public boolean handle(ExtensionContext extensionContext, Extension extension) throws Exception {
        if (!extension.getName().equals(ApiRegions.EXTENSION_NAME)) {
            return false;
        }

        final File base = Files.createTempDirectory("apiregions").toFile();
        final File featuresFile = new File(base, FEATURE_REGION_FILENAME);
        final File regionsFile = new File(base, REGION_PACKAGE_FILENAME);

        final ApiRegions apiRegions = ApiRegions.parse((JsonArray) extension.getJSONStructure());

        LauncherProperties.save(LauncherProperties.getFeatureIDtoRegionsMap(apiRegions), featuresFile);
        LauncherProperties.save(LauncherProperties.getRegionNametoPackagesMap(apiRegions), regionsFile);

        extensionContext.addFrameworkProperty(
                LauncherProperties.PROPERTY_PREFIX.concat(FEATURE_REGION_FILENAME),
                featuresFile.toURI().toURL().toString());
        extensionContext.addFrameworkProperty(
                LauncherProperties.PROPERTY_PREFIX.concat(REGION_PACKAGE_FILENAME),
                regionsFile.toURI().toURL().toString());

        return true;
    }
}
