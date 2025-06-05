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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.extension.apiregions.api.ApiRegion;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;

public class CheckApiRegionsOrder extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return ApiRegions.EXTENSION_NAME + "-check-order";
    }

    @Override
    public String getName() {
        return "Api Regions check order analyser task";
    }

    @Override
    public final void execute(final ApiRegions apiRegions, final AnalyserTaskContext ctx) throws Exception {
        final String order = ctx.getConfiguration().get("order");
        if (order == null) {
            reportError(
                    ctx,
                    "This analyser task must be configured: " + getId() + " for feature "
                            + ctx.getFeature().getId());
            reportError(ctx, "Must specify configuration key 'order'.");
            return;
        }

        final String[] sl = order.split("[,]");
        List<String> prescribedOrder = new ArrayList<>();
        for (String s : sl) {
            s = s.trim();
            if (s.length() > 0) prescribedOrder.add(s);
        }
        if (prescribedOrder.size() == 0) {
            reportError(ctx, "No regions declared in the 'order' configuration");
            return;
        }

        int regionIdx = 0;
        for (final ApiRegion region : apiRegions.listRegions()) {
            String name = region.getName();
            if (!prescribedOrder.contains(name)) {
                reportError(ctx, "Region found with undeclared name: " + name);
                return;
            }
            int prevIdx = regionIdx;
            regionIdx = validateRegion(regionIdx, prescribedOrder, name);
            if (regionIdx < 0) {
                reportError(
                        ctx,
                        "Region '" + name + "' appears in the wrong order. It appears after '"
                                + prescribedOrder.get(prevIdx) + "'. Order of regions should be " + prescribedOrder);
                return;
            }
        }
    }

    private int validateRegion(int regionIdx, List<String> order, String name) {
        for (int i = regionIdx; i < order.size(); i++) {
            if (name.equals(order.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String getPrefix() {
        return getId() + ": ";
    }

    private void reportError(AnalyserTaskContext ctx, String err) {
        ctx.reportError(getPrefix() + err);
    }
}
