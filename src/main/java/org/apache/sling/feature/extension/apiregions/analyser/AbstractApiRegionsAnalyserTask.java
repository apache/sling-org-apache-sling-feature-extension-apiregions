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

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;

public abstract class AbstractApiRegionsAnalyserTask implements AnalyserTask {

    @Override
    public final void execute(AnalyserTaskContext ctx) throws Exception {
        // read the api-regions
        ApiRegions apiRegions;
        try {
            apiRegions = ApiRegions.getApiRegions(ctx.getFeature());
        } catch (final IllegalArgumentException e) {
            ctx.reportError("API Regions does not represent a valid JSON 'api-regions': " + e.getMessage());
            return;
        }
        if (apiRegions == null) {
            // no need to be analysed
            return;
        }
        execute(apiRegions, ctx);
    }

    protected abstract void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception;
}
