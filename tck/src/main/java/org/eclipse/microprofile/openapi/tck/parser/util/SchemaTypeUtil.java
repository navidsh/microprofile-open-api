/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.tck.parser.util;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.media.BinarySchema;
import org.eclipse.microprofile.openapi.models.media.BooleanSchema;
import org.eclipse.microprofile.openapi.models.media.ByteArraySchema;
import org.eclipse.microprofile.openapi.models.media.DateSchema;
import org.eclipse.microprofile.openapi.models.media.EmailSchema;
import org.eclipse.microprofile.openapi.models.media.IntegerSchema;
import org.eclipse.microprofile.openapi.models.media.NumberSchema;
import org.eclipse.microprofile.openapi.models.media.ObjectSchema;
import org.eclipse.microprofile.openapi.models.media.PasswordSchema;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.StringSchema;
import org.eclipse.microprofile.openapi.models.media.UUIDSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaTypeUtil {

    private static final String TYPE = "type";
    private static final String FORMAT = "format";

    public static final String INTEGER_TYPE = "integer";
    public static final String NUMBER_TYPE = "number";
    public static final String STRING_TYPE = "string";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String OBJECT_TYPE = "object";

    public static final String INTEGER32_FORMAT = "int32";
    public static final String INTEGER64_FORMAT = "int64";
    public static final String FLOAT_FORMAT = "float";
    public static final String DOUBLE_FORMAT = "double";
    public static final String BYTE_FORMAT = "byte";
    public static final String BINARY_FORMAT = "binary";
    public static final String DATE_FORMAT = "date";
    public static final String DATE_TIME_FORMAT = "date-time";
    public static final String PASSWORD_FORMAT = "password";
    public static final String EMAIL_FORMAT = "email";
    public static final String UUID_FORMAT = "uuid";

    public static Schema createSchemaByType(ObjectNode node) {
        if (node == null) {
            return OASFactory.createObject(Schema.class);
        }
        final String type = getNodeValue(node, TYPE);
        if (StringUtils.isBlank(type)) {
            return OASFactory.createObject(Schema.class);
        }
        final String format = getNodeValue(node, FORMAT);

        return createSchema(type, format);
    }

    public static Schema createSchema(String type, String format) {

        if (INTEGER_TYPE.equals(type)) {
            if (INTEGER64_FORMAT.equals(format)) {
                return OASFactory.createObject(IntegerSchema.class).format(INTEGER64_FORMAT);
            } else {
                return OASFactory.createObject(IntegerSchema.class);
            }
        } else if (NUMBER_TYPE.equals(type)) {
            if (FLOAT_FORMAT.equals(format)) {
                return OASFactory.createObject(NumberSchema.class).format(FLOAT_FORMAT);
            } else if (DOUBLE_FORMAT.equals(format)) {
                return OASFactory.createObject(NumberSchema.class).format(DOUBLE_FORMAT);
            } else {
                return OASFactory.createObject(NumberSchema.class);
            }
        } else if (BOOLEAN_TYPE.equals(type)) {
            return OASFactory.createObject(BooleanSchema.class);
        } else if (STRING_TYPE.equals(type)) {
            if (BYTE_FORMAT.equals(format)) {
                return OASFactory.createObject(ByteArraySchema.class);
            } else if (BINARY_FORMAT.equals(format)) {
                return OASFactory.createObject(BinarySchema.class);
            } else if (DATE_FORMAT.equals(format)) {
                return OASFactory.createObject(DateSchema.class);
            } else if (DATE_TIME_FORMAT.equals(format)) {
                return OASFactory.createObject(DateTimeSchema.class);
            } else if (PASSWORD_FORMAT.equals(format)) {
                return OASFactory.createObject(PasswordSchema.class);
            } else if (EMAIL_FORMAT.equals(format)) {
                return OASFactory.createObject(EmailSchema.class);
            } else if (UUID_FORMAT.equals(format)) {
                return OASFactory.createObject(UUIDSchema.class);
            } else {
                return OASFactory.createObject(StringSchema.class);
            }
        } else if (OBJECT_TYPE.equals(type)) {
            return OASFactory.createObject(ObjectSchema.class);
        } else {
            return OASFactory.createObject(Schema.class);
        }
    }

    private static String getNodeValue(ObjectNode node, String field) {
        final JsonNode jsonNode = node.get(field);
        if (jsonNode == null) {
            return null;
        }
        return jsonNode.textValue();
    }

}
