/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.extension.apiregions.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;

/**
 * Extension to hold information about deprecated JDK API
 * This class is not thread safe.
 * @since 1.1.0
 */
public class JDKDeprecationExtension {

    private static final String MSG_KEY = "msg";

    private static final String SINCE_KEY = "since";

    /**
     * Extension name containing the deprecation. The extension
     * can be used to specify JDK API which is deprecated / should not be used.
     * This extension is of type {@link ExtensionType#JSON} and is optional.
     */
    public static final String EXTENSION_NAME = "jdk-deprecation";

    /**
     * Get the extension from the feature - if it exists.
     * @param feature The feature
     * @return The extension or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static JDKDeprecationExtension getExtension(final Feature feature) {
        final Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getExtension(ext);
    }

    /**
     * Get the execution environment from the extension.
     * @param ext The extension
     * @return The execution environment or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static JDKDeprecationExtension getExtension(final Extension ext) {
        if ( ext == null ) {
            return null;
        }
        if ( ext.getType() != ExtensionType.JSON ) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        }
        return new JDKDeprecationExtension(ext.getJSONStructure());
    }

    private final Map<String, DeprecationInfo> memberInfos = new LinkedHashMap<>();

    private JDKDeprecationExtension(final JsonStructure structure) {
        for(final Map.Entry<String, JsonValue> prop : structure.asJsonObject().entrySet()) {
            if ( prop.getValue().getValueType() == ValueType.STRING ) {
                final DeprecationInfo info = new DeprecationInfo(((JsonString)prop.getValue()).getString());
                this.addMemberInfo(prop.getKey(), info);
            } else if ( prop.getValue().getValueType() == ValueType.OBJECT ) {
                final JsonObject memberObj = prop.getValue().asJsonObject();
                if ( !memberObj.containsKey(MSG_KEY) ) {
                    throw new IllegalArgumentException("No msg property found");
                }
                final DeprecationInfo info = new DeprecationInfo(memberObj.getString(MSG_KEY));
                info.setSince(memberObj.getString(SINCE_KEY, null));
                this.addMemberInfo(prop.getKey(), info);
            } else {
                throw new IllegalArgumentException("Wrong value type " + prop.getValue().getValueType().name());
            }

        }
    }

    /**
     * Add deprecation info for a member
     * @param member The member
     * @param i The info
     * @throws IllegalStateException if the package is already deprecated
     */
    public void addMemberInfo(final String member, final DeprecationInfo i) {
        this.memberInfos.put(member, i);
    }

    /**
     * Remove deprecation info for a member
     * @param member The member
     */
    public void removeMemberInfo(final String member) {
        this.memberInfos.remove(member);
    }

    /**
     * Get all deprecation member infos
     * @return The infos
     */
    public Map<String, DeprecationInfo> getMemberInfos() {
        return this.memberInfos;
    }

    /**
     * Generate a JSON representation
     * @return The JSON Object
     */
    public JsonObject toJSON() {
        final JsonObjectBuilder membersBuilder = Json.createObjectBuilder();
        for(final Map.Entry<String, DeprecationInfo> memberEntry : this.getMemberInfos().entrySet()) {
            if ( memberEntry.getValue().getSince() == null ) {
                membersBuilder.add(memberEntry.getKey(), memberEntry.getValue().getMessage());
            } else {
                final JsonObjectBuilder mBuilder = Json.createObjectBuilder();
                mBuilder.add(MSG_KEY, memberEntry.getValue().getMessage());
                mBuilder.add(SINCE_KEY, memberEntry.getValue().getSince());

                membersBuilder.add(memberEntry.getKey(), mBuilder);
            }
        }

        return membersBuilder.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberInfos);
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
        JDKDeprecationExtension other = (JDKDeprecationExtension) obj;
        return Objects.equals(memberInfos, other.memberInfos);
    }
}
