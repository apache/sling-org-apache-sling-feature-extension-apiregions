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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deprecation state for a package
 *
 * Either the whole package is deprecated or some members.
 * This class is not thread safe.
 * @since 1.1
 */
public class Deprecation {

    private DeprecationInfo packageInfo;

    private final Map<String, DeprecationInfo> memberInfos = new LinkedHashMap<>();

    /**
     * Get the optional package info if the package is deprecated
     * @return The info or {@code null}
     */
    public DeprecationInfo getPackageInfo() {
        return this.packageInfo;
    }

    /**
     * Set the deprecation info for the whole package
     * @param i The info
     * @throws IllegalStateException If a member is already deprecated
     */
    public void setPackageInfo(final DeprecationInfo i) {
        if (!this.memberInfos.isEmpty()) {
            throw new IllegalStateException("Member is already deprecated");
        }
        this.packageInfo = i;
    }

    /**
     * Add deprecation info for a member
     * @param member The member
     * @param i The info
     * @throws IllegalStateException if the package is already deprecated
     */
    public void addMemberInfo(final String member, final DeprecationInfo i) {
        if (this.packageInfo != null) {
            throw new IllegalStateException("Package is already deprecated");
        }
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
     * Returns the class name part of a member string.
     * This is the part before the first hash (or the full string if no hash)
     * @param member The member
     * @return The class name
     */
    public static final String getClassName(final String member) {
        final int pos = member.indexOf("#");
        if (pos == -1) {
            return member;
        }
        return member.substring(0, pos);
    }

    /**
     * Returns the class member name part of a member string.
     * This is the part after the first hash (or {@code null} if no hash
     * @param member The member
     * @return The class member name or {@code null}
     */
    public static final String getClassMemberName(final String member) {
        final int pos = member.indexOf("#");
        if (pos == -1) {
            return null;
        }
        return member.substring(pos + 1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberInfos, packageInfo);
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
        final Deprecation other = (Deprecation) obj;
        return Objects.equals(memberInfos, other.memberInfos) && Objects.equals(packageInfo, other.packageInfo);
    }
}
