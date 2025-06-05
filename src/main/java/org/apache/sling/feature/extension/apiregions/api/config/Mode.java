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
package org.apache.sling.feature.extension.apiregions.api.config;

/**
 * The validation mode
 * @since 1.2
 */
public enum Mode {

    /** Default mode - If validation fails, issue an error. */
    STRICT,

    /** If validation fails, issue a warning (but use the invalid value). */
    LENIENT,

    /** If validation fails, do not report and use the invalid value. */
    SILENT,

    /** If validation fails, use the default value (if provided - otherwise remove value) and issue a warning. */
    DEFINITIVE,

    /** If validation fails, use the default value (if provided - otherwise remove value) and do not issue a warning. */
    SILENT_DEFINITIVE
}
