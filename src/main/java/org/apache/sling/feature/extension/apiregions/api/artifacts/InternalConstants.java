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
package org.apache.sling.feature.extension.apiregions.api.artifacts;

/**
 * Constants used in this implementation
 */
abstract class InternalConstants {

    static final String KEY_MODE = "mode";

    static final String KEY_ARTIFACT_ID = "artifact-id";

    static final String KEY_ALLOWED_VERSION_RANGES = "allowed-version-ranges";

    static final String KEY_DENIED_VERSION_RANGES = "denied-version-ranges";

    static final String KEY_MESSAGE = "message";

    static final String KEY_BUNDLE_VERSION_RULES = "bundle-version-rules";

    static final String KEY_ARTIFACT_VERSION_RULES = "artifact-version-rules";

    /** @since 2.10 */
    static final String KEY_ENFORCE_ON = "enforce-on";
}
