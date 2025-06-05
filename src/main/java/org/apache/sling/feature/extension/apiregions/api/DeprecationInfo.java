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

import java.util.Calendar;
import java.util.Objects;

/**
 * Deprecation info for a package or member.
 * This class is not thread safe.
 * @since 1.1
 */
public class DeprecationInfo {

    private final String message;

    private String since;

    /**
     * Optional for removal information.
     * @since 1.3.0
     */
    private String forRemoval;

    /**
     * Optional validation mode
     * @since 1.4.0
     */
    private DeprecationValidationMode mode;

    /**
     * Create a new info
     * @param msg The msg
     * @throws IllegalArgumentException if msg is {@code null}
     */
    public DeprecationInfo(final String msg) {
        if (msg == null) {
            throw new IllegalArgumentException();
        }
        this.message = msg;
    }

    /**
     * Get the message
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the optional since information
     * @return The since information or {@code null}
     */
    public String getSince() {
        return since;
    }

    /**
     * Set the since information
     * @param since The new info
     */
    public void setSince(final String since) {
        this.since = since;
    }

    /**
     * Get the optional for removal information. This should either be 'true' or 'false'
     * or a date in the format 'YYYY-MM-DD'.
     * @return The for removal information or {@code null}
     * @since 1.3.0
     */
    public String getForRemoval() {
        return forRemoval;
    }

    /**
     * Set the for removal information. This should either be 'true' or 'false'
     * or a date in the format 'YYYY-MM-DD'.
     * @param value The new removal info
     * @since 1.3.0
     */
    public void setForRemoval(final String value) {
        this.forRemoval = value;
    }

    /**
     * Is this member intended to be removed?
     * @return {@code true} if the member will be removed in the future
     * @since 1.3.0
     */
    public boolean isForRemoval() {
        return this.forRemoval != null && !"false".equalsIgnoreCase(this.forRemoval);
    }

    /**
     * Return a date by which this member will be removed
     * @return A calendar if the value from {@link #getForRemoval()} is formatted as 'YYYY-MM-DD'.
     * @since 1.3.0
     */
    public Calendar getForRemovalBy() {
        if (this.forRemoval != null) {
            final String[] parts = this.forRemoval.split("-");
            if (parts.length == 3) {
                if (parts[0].length() == 4 && parts[1].length() == 2 && parts[2].length() == 2) {
                    try {
                        final int year = Integer.parseInt(parts[0]);
                        final int month = Integer.parseInt(parts[1]);
                        final int day = Integer.parseInt(parts[2]);

                        final Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month - 1);
                        c.set(Calendar.DAY_OF_MONTH, day);

                        return c;
                    } catch (final NumberFormatException ignore) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the optional validation mode.
     * @return The mode or {@code null}
     * @since 1.4.0
     */
    public DeprecationValidationMode getMode() {
        return mode;
    }

    /**
     * Set the validation mode.
     * @param value The new mode
     * @since 1.4.0
     */
    public void setMode(final DeprecationValidationMode value) {
        this.mode = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, since, forRemoval, mode);
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
        DeprecationInfo other = (DeprecationInfo) obj;
        return Objects.equals(message, other.message)
                && Objects.equals(since, other.since)
                && Objects.equals(forRemoval, other.forRemoval)
                && Objects.equals(mode, other.mode);
    }
}
