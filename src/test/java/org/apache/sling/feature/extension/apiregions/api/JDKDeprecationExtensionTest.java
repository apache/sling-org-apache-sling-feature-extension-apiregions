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
package org.apache.sling.feature.extension.apiregions.api;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JDKDeprecationExtensionTest {

    private static final String MSG = "deprecated!";

    private static final String SINCE = "now";

    @Test
    public void testNullFeature() {
        assertNull(JDKDeprecationExtension.getExtension((Feature) null));
    }

    @Test
    public void testNullExtension() {
        assertNull(JDKDeprecationExtension.getExtension((Extension) null));
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        assertNull(JDKDeprecationExtension.getExtension(f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongExtensionType() {
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        final Extension e =
                new Extension(ExtensionType.TEXT, JDKDeprecationExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        f.getExtensions().add(e);
        JDKDeprecationExtension.getExtension(f);
    }

    @Test
    public void testSimpleMembers() throws Exception {
        final Extension ext =
                new Extension(ExtensionType.JSON, JDKDeprecationExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        ext.setJSON("{\"foo\":\"" + MSG + "\",\"bar\":\"" + MSG + MSG + "\"}");
        final JDKDeprecationExtension jdk = JDKDeprecationExtension.getExtension(ext);

        assertEquals(2, jdk.getMemberInfos().size());

        final DeprecationInfo foo = jdk.getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = jdk.getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertNull(bar.getSince());

        assertEquals(ext.getJSONStructure(), jdk.toJSON());
    }

    @Test
    public void testComplexMembers() throws Exception {
        final Extension ext =
                new Extension(ExtensionType.JSON, JDKDeprecationExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        ext.setJSON("{\"foo\":{\"msg\":\"" + MSG + "\"},\"bar\":{\"msg\":\"" + MSG + MSG + "\",\"since\":\"" + SINCE
                + "\"}}");
        final JDKDeprecationExtension jdk = JDKDeprecationExtension.getExtension(ext);

        assertEquals(2, jdk.getMemberInfos().size());

        final DeprecationInfo foo = jdk.getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = jdk.getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertEquals(SINCE, bar.getSince());

        // the expected JSON is actually a mixed JSON (not the input!)
        ext.setJSON("{\"foo\":\"" + MSG + "\",\"bar\":{\"msg\":\"" + MSG + MSG + "\",\"since\":\"" + SINCE + "\"}}");
        assertEquals(ext.getJSONStructure(), jdk.toJSON());
    }

    @Test
    public void testMixedMembers() throws Exception {
        final Extension ext =
                new Extension(ExtensionType.JSON, JDKDeprecationExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        ext.setJSON("{\"foo\":\"" + MSG + "\",\"bar\":{\"msg\":\"" + MSG + MSG + "\",\"since\":\"" + SINCE + "\"}}");
        final JDKDeprecationExtension jdk = JDKDeprecationExtension.getExtension(ext);

        assertEquals(2, jdk.getMemberInfos().size());

        final DeprecationInfo foo = jdk.getMemberInfos().get("foo");
        assertEquals(MSG, foo.getMessage());
        assertNull(foo.getSince());
        final DeprecationInfo bar = jdk.getMemberInfos().get("bar");
        assertEquals(MSG + MSG, bar.getMessage());
        assertEquals(SINCE, bar.getSince());

        assertEquals(ext.getJSONStructure(), jdk.toJSON());
    }
}
