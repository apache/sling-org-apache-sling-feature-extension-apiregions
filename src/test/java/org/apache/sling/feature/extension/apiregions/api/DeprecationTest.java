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

import org.junit.Test;

public class DeprecationTest {

    @Test(expected = IllegalStateException.class)
    public void testNoMemberIfPackageIsDeprecated() throws Exception {
        final Deprecation dep = new Deprecation();
        final DeprecationInfo info = new DeprecationInfo("msg");

        // setting package info is ok
        dep.setPackageInfo(info);
        // setting member now is not
        dep.addMemberInfo("foo", info);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoPackageIfMemberIsAvailable() throws Exception {
        final Deprecation dep = new Deprecation();
        final DeprecationInfo info = new DeprecationInfo("msg");

        // add member is ok
        dep.addMemberInfo("foo", info);
        // setting package info is not ok
        dep.setPackageInfo(info);
    }
}
