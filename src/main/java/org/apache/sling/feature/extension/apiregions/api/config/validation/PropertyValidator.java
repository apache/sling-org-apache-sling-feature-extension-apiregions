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
package org.apache.sling.feature.extension.apiregions.api.config.validation;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.sling.feature.extension.apiregions.api.config.DescribableEntity;
import org.apache.sling.feature.extension.apiregions.api.config.Mode;
import org.apache.sling.feature.extension.apiregions.api.config.Option;
import org.apache.sling.feature.extension.apiregions.api.config.PlaceholderPolicy;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyDescription;
import org.apache.sling.feature.extension.apiregions.api.config.PropertyType;

/**
 * Validate a configuration property or framework property
 */
public class PropertyValidator {

    private boolean liveValues = false;

    /**
     * Are live values validated?
     * @return {@code true} if live values are validated
     * @since 1.4
     */
    public boolean isLiveValues() {
        return liveValues;
    }

    /**
     * Set whether live values are validated.
     * @param value Flag for validating live values
     * @since 1.4
     */
    public void setLiveValues(final boolean value) {
        this.liveValues = value;
    }

    /**
     * Validate the value against the property definition
     * @param value The value to validate
     * @param desc The property description
     * @return A property validation result
     */
    public PropertyValidationResult validate(final Object value, final PropertyDescription desc) {
        return this.validate(value, desc, Mode.STRICT);
    }

    /**
     * Validate the value against the property definition
     * @param value The value to validate
     * @param desc The property description
     * @param mode Optional validation mode - this mode is used if the description does not define a mode. Defaults to {@link Mode#STRICT}.
     * @return A property validation result
     * @since 1.2.0
     */
    public PropertyValidationResult validate(final Object value, final PropertyDescription desc, final Mode mode) {
        final Context context = new Context();
        context.description = desc;
        context.validationMode = desc.getMode() != null ? desc.getMode() : (mode != null ? mode : Mode.STRICT);

        if (value == null) {
            if (desc.isRequired()) {
                setResult(context, desc, "No value provided");
            }
        } else {
            final List<Object> values;
            if (value.getClass().isArray()) {
                // array
                values = new ArrayList<>();
                for (int i = 0; i < Array.getLength(value); i++) {
                    values.add(Array.get(value, i));
                }
            } else if (value instanceof Collection) {
                // collection
                values = new ArrayList<>();
                final Collection<?> c = (Collection<?>) value;
                for (final Object o : c) {
                    values.add(o);
                }
            } else {
                // single value
                values = null;
                validateValue(context, desc, value);
            }

            if (values != null) {
                // array or collection
                for (final Object val : values) {
                    validateValue(context, desc, val);
                }
                validateList(context, desc, values);
            }

            if (desc.getDeprecated() != null) {
                setResult(context.result, null, Mode.LENIENT, desc, desc.getDeprecated());
            }
        }
        return context.result;
    }

    void setResult(final Context context, final DescribableEntity desc, final String msg) {
        setResult(context.result, context.description.getDefaultValue(), context.validationMode, desc, msg);
    }

    static void setResult(
            final PropertyValidationResult result,
            final Object defaultValue,
            final Mode validationMode,
            final DescribableEntity desc,
            final String msg) {
        // set postfix to the message if since or enforce-on are set
        String postfixMsg = "";
        if (desc != null && desc.getSince() != null) {
            postfixMsg = postfixMsg.concat(". Since : ").concat(desc.getSince());
        }
        if (desc != null && desc.getEnforceOn() != null) {
            postfixMsg = postfixMsg.concat(". Enforced on : ").concat(desc.getEnforceOn());
        }
        String finalMsg = msg + postfixMsg;
        if (validationMode == Mode.STRICT) {
            result.getErrors().add(finalMsg);
        } else if (validationMode == Mode.LENIENT || validationMode == Mode.DEFINITIVE) {
            result.getWarnings().add(finalMsg);
        }
        if (validationMode == Mode.DEFINITIVE || validationMode == Mode.SILENT_DEFINITIVE) {
            result.setUseDefaultValue(true);
            result.setDefaultValue(defaultValue);
        }
    }

    /**
     * Validate a multi value
     * @param context The current context
     * @param desc describable entity
     * @param values The values
     */
    void validateList(final Context context, final DescribableEntity desc, final List<Object> values) {
        if (context.description.getCardinality() > 0 && values.size() > context.description.getCardinality()) {
            setResult(
                    context,
                    desc,
                    "Array/collection contains too many elements, only " + context.description.getCardinality()
                            + " allowed");
        }
        if (context.description.getIncludes() != null) {
            for (final String inc : context.description.getIncludes()) {
                boolean found = false;
                for (final Object val : values) {
                    if (inc.equals(val.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    setResult(context, desc, "Required included value " + inc + " not found");
                    if (context.result.isUseDefaultValue()) {
                        context.result.setUseIncludesAndExcludes(
                                context.description.getIncludes(), context.description.getExcludes());
                    }
                }
            }
        }
        if (context.description.getExcludes() != null) {
            for (final String exc : context.description.getExcludes()) {
                boolean found = false;
                for (final Object val : values) {
                    if (exc.equals(val.toString())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    setResult(context, desc, "Not allowed excluded value " + exc + " found");
                    if (context.result.isUseDefaultValue()) {
                        context.result.setUseIncludesAndExcludes(
                                context.description.getIncludes(), context.description.getExcludes());
                    }
                }
            }
        }
    }

    private static final List<String> PLACEHOLDERS = Arrays.asList("$[env:", "$[secret:", "$[prop:");

    void validateValue(final Context context, final PropertyDescription desc, final Object value) {
        if (value != null) {
            // check for placeholder
            boolean hasPlaceholder = false;
            if (value instanceof String) {
                final String strVal = (String) value;
                for (final String p : PLACEHOLDERS) {
                    if (strVal.contains(p)) {
                        hasPlaceholder = true;
                        break;
                    }
                }
            }
            if (!hasPlaceholder) {
                switch (context.description.getType()) {
                    case BOOLEAN:
                        validateBoolean(context, desc, value);
                        break;
                    case BYTE:
                        validateByte(context, desc, value);
                        break;
                    case CHARACTER:
                        validateCharacter(context, desc, value);
                        break;
                    case DOUBLE:
                        validateDouble(context, desc, value);
                        break;
                    case FLOAT:
                        validateFloat(context, desc, value);
                        break;
                    case INTEGER:
                        validateInteger(context, desc, value);
                        break;
                    case LONG:
                        validateLong(context, desc, value);
                        break;
                    case SHORT:
                        validateShort(context, desc, value);
                        break;
                    case STRING:
                        validateRequired(context, desc, value);
                        break;
                    case EMAIL:
                        validateEmail(context, desc, value);
                        break;
                    case PASSWORD:
                        validatePassword(context, desc, value, false);
                        break;
                    case URL:
                        validateURL(context, desc, value);
                        break;
                    case PATH:
                        validatePath(context, desc, value);
                        break;
                    default:
                        context.result
                                .getErrors()
                                .add("Unable to validate value - unknown property type : "
                                        + context.description.getType());
                }
                validateRegex(context, desc, context.description.getRegexPattern(), value);
                validateOptions(context, desc, value);
                if (context.description.getType() != PropertyType.PASSWORD) {
                    validatePlaceholderPolicy(context, desc, value, false);
                }
            } else {
                // placeholder is present
                if (context.description.getType() == PropertyType.PASSWORD) {
                    validatePassword(context, desc, value, true);
                } else if (context.description.getType() == PropertyType.STRING) {
                    validateRegex(context, desc, context.description.getPlaceholderRegexPattern(), value);

                    // we mark the result as skipped if a regex or options are set or if a value is marked as required.
                    if (context.description.getRegex() != null
                            || context.description.getOptions() != null
                            || context.description.isRequired()) {
                        context.result.markSkipped();
                    }
                } else {
                    context.result.markSkipped();
                }
                if (context.description.getType() != PropertyType.PASSWORD) {
                    validatePlaceholderPolicy(context, desc, value, true);
                }
            }
        } else {
            setResult(context, desc, "Null value provided for validation");
        }
    }

    void validateRequired(final Context context, final DescribableEntity desc, final Object value) {
        if (context.description.isRequired()) {
            final String val = value.toString();
            if (val.isEmpty()) {
                setResult(context, desc, "Value is required");
            }
        }
    }

    void validateBoolean(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Boolean)) {
            if (value instanceof String) {
                final String v = (String) value;
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    setResult(context, desc, "Boolean value must either be true or false, but not " + value);
                }
            } else {
                setResult(context, desc, "Boolean value must either be of type Boolean or String : " + value);
            }
        }
    }

    void validateByte(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Byte)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Byte.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Byte : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).byteValue());
            } else {
                setResult(context, desc, "Byte value must either be of type Byte or String : " + value);
            }
        } else {
            validateRange(context, desc, (Byte) value);
        }
    }

    void validateShort(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Short)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Short.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Short : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).shortValue());
            } else {
                setResult(context, desc, "Short value must either be of type Short or String : " + value);
            }
        } else {
            validateRange(context, desc, (Short) value);
        }
    }

    void validateInteger(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Integer)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Integer.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Integer : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).intValue());
            } else {
                setResult(context, desc, "Integer value must either be of type Integer or String : " + value);
            }
        } else {
            validateRange(context, desc, (Integer) value);
        }
    }

    void validateLong(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Long)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Long.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Long : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).longValue());
            } else {
                setResult(context, desc, "Long value must either be of type Long or String : " + value);
            }
        } else {
            validateRange(context, desc, (Long) value);
        }
    }

    void validateFloat(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Float)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Float.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Float : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).floatValue());
            } else {
                setResult(context, desc, "Float value must either be of type Float or String : " + value);
            }
        } else {
            validateRange(context, desc, (Float) value);
        }
    }

    void validateDouble(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Double)) {
            if (value instanceof String) {
                final String v = (String) value;
                try {
                    validateRange(context, desc, Double.valueOf(v));
                } catch (final NumberFormatException nfe) {
                    setResult(context, desc, "Value is not a valid Double : " + value);
                }
            } else if (value instanceof Number) {
                validateRange(context, desc, ((Number) value).doubleValue());
            } else {
                setResult(context, desc, "Double value must either be of type Double or String : " + value);
            }
        } else {
            validateRange(context, desc, (Double) value);
        }
    }

    void validateCharacter(final Context context, final DescribableEntity desc, final Object value) {
        if (!(value instanceof Character)) {
            if (value instanceof String) {
                final String v = (String) value;
                if (v.length() > 1) {
                    setResult(context, desc, "Value is not a valid Character : " + value);
                }
            } else {
                setResult(context, desc, "Character value must either be of type Character or String : " + value);
            }
        }
    }

    void validateURL(final Context context, final DescribableEntity desc, final Object value) {
        final String val = value.toString();
        try {
            new URL(val);
        } catch (final MalformedURLException mue) {
            setResult(context, desc, "Value is not a valid URL : " + val);
        }
    }

    void validateEmail(final Context context, final DescribableEntity desc, final Object value) {
        final String val = value.toString();
        // poor man's validation (should probably use InternetAddress)
        if (!val.contains("@")) {
            setResult(context, desc, "Not a valid email address " + val);
        }
    }

    void validatePassword(
            final Context context, final DescribableEntity desc, final Object value, final boolean hasPlaceholder) {
        if (!this.isLiveValues()
                && !hasPlaceholder
                && context.description.getPlaceholderPolicy() != PlaceholderPolicy.DENY) {
            setResult(context, desc, "Value for a password must use a placeholder");
        }
    }

    void validatePath(final Context context, final DescribableEntity desc, final Object value) {
        final String val = value.toString();
        // poor man's validation
        if (!val.startsWith("/")) {
            setResult(context, desc, "Not a valid path " + val);
        }
    }

    void validateRange(final Context context, final DescribableEntity desc, final Number value) {
        if (context.description.getRange() != null) {
            if (context.description.getRange().getMin() != null) {
                if (value instanceof Float || value instanceof Double) {
                    final double min = context.description.getRange().getMin().doubleValue();
                    if (value.doubleValue() < min) {
                        setResult(
                                context,
                                desc,
                                "Value " + value + " is too low; should not be lower than "
                                        + context.description.getRange().getMin());
                    }
                } else {
                    final long min = context.description.getRange().getMin().longValue();
                    if (value.longValue() < min) {
                        setResult(
                                context,
                                desc,
                                "Value " + value + " is too low; should not be lower than "
                                        + context.description.getRange().getMin());
                    }
                }
            }
            if (context.description.getRange().getMax() != null) {
                if (value instanceof Float || value instanceof Double) {
                    final double max = context.description.getRange().getMax().doubleValue();
                    if (value.doubleValue() > max) {
                        setResult(
                                context,
                                desc,
                                "Value " + value + " is too high; should not be higher than "
                                        + context.description.getRange().getMax());
                    }
                } else {
                    final long max = context.description.getRange().getMax().longValue();
                    if (value.longValue() > max) {
                        setResult(
                                context,
                                desc,
                                "Value " + value + " is too high; should not be higher than "
                                        + context.description.getRange().getMax());
                    }
                }
            }
        }
    }

    void validateRegex(
            final Context context, final PropertyDescription desc, final Pattern pattern, final Object value) {
        if (pattern != null) {
            if (!pattern.matcher(value.toString()).matches()) {
                if (desc.getType() == PropertyType.PASSWORD) {
                    setResult(context, desc, "Value does not match regex " + pattern.pattern());
                } else {
                    setResult(context, desc, "Value " + value + " does not match regex " + pattern.pattern());
                }
            }
        }
    }

    void validateOptions(final Context context, final PropertyDescription desc, final Object value) {
        if (context.description.getOptions() != null) {
            boolean found = false;
            for (final Option opt : context.description.getOptions()) {
                if (opt.getValue().equals(value.toString())) {
                    found = true;
                }
            }
            if (!found) {
                if (desc.getType() == PropertyType.PASSWORD) {
                    setResult(context, desc, "Value does not match provided options");
                } else {
                    setResult(context, desc, "Value " + value + " does not match provided options");
                }
            }
        }
    }

    void validatePlaceholderPolicy(
            final Context context, final DescribableEntity desc, final Object value, final boolean hasPlaceholder) {
        // only check policy if no live values
        if (!this.isLiveValues()) {
            // for policy default and allow nothing needs to be validated
            if (context.description.getPlaceholderPolicy() == PlaceholderPolicy.DENY && hasPlaceholder) {
                setResult(context, desc, "Placeholder in value is not allowed");
            } else if (context.description.getPlaceholderPolicy() == PlaceholderPolicy.REQUIRE && !hasPlaceholder) {
                setResult(context, desc, "Value must use a placeholder");
            }
        }
    }

    static final class Context {

        public final PropertyValidationResult result = new PropertyValidationResult();

        public PropertyDescription description;

        public Mode validationMode;
    }
}
