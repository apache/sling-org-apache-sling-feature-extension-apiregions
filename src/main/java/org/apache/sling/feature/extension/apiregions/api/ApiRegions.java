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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.JsonWriter;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;

/**
 * Configuration of API regions for Java API.
 *
 * This class is not thread safe.
 */
public class ApiRegions {

    /** The name of the api regions extension. */
    public static final String EXTENSION_NAME = "api-regions";

    private static final String NAME_KEY = "name";

    private static final String EXPORTS_KEY = "exports";

    private final List<ApiRegion> regions = new ArrayList<>();

    /**
     * Return the list of regions
     *
     * @return Unmodifiable list of regions, might be empty
     */
    public List<ApiRegion> listRegions() {
        return Collections.unmodifiableList(this.regions);
    }

    /**
     * Get the root regions. The root is the region which does not have a parent
     *
     * @return The root region or {@code null}
     */
    public ApiRegion getRoot() {
        if (this.regions.isEmpty()) {
            return null;
        }
        return this.regions.get(0);
    }

    /**
     * Check if any region exists
     *
     * @return {@code true} if it has any region
     */
    public boolean isEmpty() {
        return this.regions.isEmpty();
    }

    /**
     * Add the region. The region is only added if there isn't already a region with
     * the same name
     *
     * @param region The region to add
     * @return {@code true} if the region could be added, {@code false} otherwise
     */
    public boolean add(final ApiRegion region) {
        return add(this.regions.size(), region);
    }

    /**
     * Add the region. The region is only added if there isn't already a region with
     * the same name
     *
     * @param idx The position to add
     * @param region The region to add
     * @return {@code true} if the region could be added, {@code false} otherwise
     */
    public boolean add(final int idx, final ApiRegion region) {
        for (final ApiRegion c : this.regions) {
            if (c.getName().equals(region.getName())) {
                return false;
            }
        }
        Set<ArtifactId> origins = new LinkedHashSet<>(Arrays.asList(region.getFeatureOrigins()));

        this.regions.stream()
                .filter(existingRegion -> {
                    ArtifactId[] targetOrigins = existingRegion.getFeatureOrigins();
                    return (targetOrigins.length == 0 && origins.isEmpty())
                            || Stream.of(targetOrigins).anyMatch(origins::contains);
                })
                .reduce((a, b) -> b)
                .ifPresent(region::setParent);

        this.regions.add(idx, region);
        return true;
    }

    /**
     * Get a named region
     *
     * @param name The name
     * @return The region or {@code null}
     */
    public ApiRegion getRegionByName(final String name) {
        ApiRegion found = null;

        for (final ApiRegion c : this.regions) {
            if (c.getName().equals(name)) {
                found = c;
                break;
            }
        }

        return found;
    }

    public ApiRegion[] getRegionsByFeature(final ArtifactId featureId) {
        return this.regions.stream()
                .filter(region -> Stream.of(region.getFeatureOrigins()).anyMatch(featureId::equals))
                .toArray(ApiRegion[]::new);
    }

    /**
     * Get the names of the regions
     *
     * @return The list of regions, might be empty
     */
    public List<String> getRegionNames() {
        final List<String> names = new ArrayList<>();
        for (final ApiRegion c : this.regions) {
            names.add(c.getName());
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Convert regions into json
     *
     * @return The json array
     * @throws IOException If generating the JSON fails
     */
    public JsonArray toJSONArray() throws IOException {
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (final ApiRegion region : this.regions) {
            final JsonObjectBuilder regionBuilder = Json.createObjectBuilder();
            regionBuilder.add(NAME_KEY, region.getName());

            if (!region.listExports().isEmpty()) {
                final JsonArrayBuilder expArrayBuilder = Json.createArrayBuilder();
                for (final ApiExport exp : region.listExports()) {
                    expArrayBuilder.add(exp.toJSONValue());
                }

                regionBuilder.add(EXPORTS_KEY, expArrayBuilder);
            }
            ArtifactId[] origins = region.getFeatureOrigins();
            if (origins.length > 0) {
                final JsonArrayBuilder originBuilder = Json.createArrayBuilder();
                for (ArtifactId origin : origins) {
                    originBuilder.add(origin.toMvnId());
                }
                regionBuilder.add(Artifact.KEY_FEATURE_ORIGINS, originBuilder);
            }
            for (final Map.Entry<String, String> entry : region.getProperties().entrySet()) {
                regionBuilder.add(entry.getKey(), entry.getValue());
            }

            arrayBuilder.add(regionBuilder);
        }

        return arrayBuilder.build();
    }

    /**
     * Convert regions into json
     *
     * @return The json array as a string
     * @throws IOException If generating the JSON fails
     */
    public String toJSON() throws IOException {
        final JsonArray array = this.toJSONArray();
        try (final StringWriter stringWriter = new StringWriter();
                final JsonWriter writer = Json.createWriter(stringWriter)) {
            writer.writeArray(array);
            return stringWriter.toString();
        }
    }

    /**
     * Parse a JSON array into an api regions object
     *
     * @param json The json as a string
     * @return The api regions
     * @throws IOException If the json could not be parsed
     */
    public static ApiRegions parse(final String json) throws IOException {
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return parse(reader.readArray());
        }
    }

    /**
     * Parse a JSON array into an api regions object
     *
     * @param json The json
     * @return The api regions
     * @throws IOException If the json could not be parsed
     */
    public static ApiRegions parse(final JsonArray json) throws IOException {
        try {
            final ApiRegions regions = new ApiRegions();

            for (final JsonValue value : json) {
                if (value.getValueType() != ValueType.OBJECT) {
                    throw new IOException("Illegal api regions json " + json);
                }
                final JsonObject obj = (JsonObject) value;

                final ApiRegion region = new ApiRegion(obj.getString(NAME_KEY));

                for (final Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                    if (NAME_KEY.equals(entry.getKey())) {
                        continue; // already set
                    } else if (entry.getKey().equals(EXPORTS_KEY)) {
                        for (final JsonValue e : (JsonArray) entry.getValue()) {
                            ApiExport.fromJson(region, e);
                        }
                    } else if (entry.getKey().equals(Artifact.KEY_FEATURE_ORIGINS)) {
                        final Set<ArtifactId> origins = new LinkedHashSet<>();
                        for (final JsonValue origin : (JsonArray) entry.getValue()) {
                            origins.add(ArtifactId.fromMvnId(((JsonString) origin).getString()));
                        }
                        region.setFeatureOrigins(origins.toArray(new ArtifactId[0]));

                        // everything else is stored as a string property
                    } else {
                        region.getProperties().put(entry.getKey(), ((JsonString) entry.getValue()).getString());
                    }
                }
                if (!regions.add(region)) {
                    throw new IOException("Region " + region.getName() + " is defined twice");
                }
            }
            return regions;
        } catch (final JsonException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the api regions from the feature - if it exists.
     * @param feature The feature
     * @return The api regions or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     * @since 1.1
     */
    public static ApiRegions getApiRegions(final Feature feature) {
        final Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getApiRegions(ext);
    }

    /**
     * Get the api regions from the extension.
     * @param ext The extension
     * @return The api regions or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     * @since 1.1
     */
    public static ApiRegions getApiRegions(final Extension ext) {
        if (ext == null) {
            return null;
        }
        if (ext.getType() != ExtensionType.JSON) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        }
        if (ext.getJSONStructure() == null) {
            return new ApiRegions();
        }
        try {
            return parse(ext.getJSONStructure().asJsonArray());
        } catch (final IOException ioe) {
            throw new IllegalArgumentException(ioe.getMessage(), ioe);
        }
    }

    @Override
    public String toString() {
        return "ApiRegions [regions=" + regions + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(regions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ApiRegions other = (ApiRegions) obj;
        return Objects.equals(regions, other.regions);
    }
}
