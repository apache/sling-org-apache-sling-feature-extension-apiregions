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

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CheckApiRegionsDependenciesTest extends AbstractApiRegionsAnalyserTaskTest<CheckApiRegionsDependencies> {

    @Override
    protected CheckApiRegionsDependencies newTask() {
        return new CheckApiRegionsDependencies();
    }

    @Test
    public void testNotValidApiRegionJson() throws Exception {
        List<String> errors = execute(
                "[{\"name\": \"global\",\"exports\": [\"org.osgi.util.function\"]},{\"name\": \"deprecated\",\"exports\": [\"org.objectweb.asm\"]}]");

        assertFalse(errors.isEmpty());
        assertEquals(
                "Bundle 'org.osgi:org.osgi.util.function:1.0.0' (defined in feature 'org.apache.sling.testing:org.apache.sling.testing.apiregions:1.0.0') exports package 'org.osgi.util.function' that is declared in the visible 'global' region, which uses package 'org.objectweb.asm' that is in the non-visible 'deprecated' region",
                errors.iterator().next());
    }

    @Test
    public void testPackageEnlistedInBothRegions() throws Exception {
        List<String> errors = execute(
                "[{\"name\": \"global\",\"exports\": [\"org.osgi.util.function\"]},{\"name\": \"deprecated\",\"exports\": [\"org.osgi.util.function\"]}]");

        assertFalse(errors.isEmpty());
        assertEquals(
                "Bundle 'org.osgi:org.osgi.util.function:1.0.0' (defined in feature 'org.apache.sling.testing:org.apache.sling.testing.apiregions:1.0.0') exports package 'org.osgi.util.function' that is declared in both visible 'global' and non-visible 'deprecated' APIs regions",
                errors.iterator().next());
    }

    @Test
    public void testValidApiRegionJson() throws Exception {
        List<String> errors = execute("[{\"name\": \"global\",\"exports\": [\"org.osgi.util.function\"]}]");
        assertTrue(errors.isEmpty());
    }
}
