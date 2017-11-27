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

package org.eclipse.microprofile.openapi.tck.parser.processors;

import static org.eclipse.microprofile.openapi.tck.parser.util.RefUtils.computeRefFormat;
import static org.eclipse.microprofile.openapi.tck.parser.util.RefUtils.isAnExternalRefFormat;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.ArraySchema;
import org.eclipse.microprofile.openapi.models.media.ComposedSchema;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

public class SchemaProcessor {
    private final ResolverCache cache;
    private final ExternalRefProcessor externalRefProcessor;

    public SchemaProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processSchema(Schema schema) {
        if (schema != null) {
            if (schema.getRef() != null) {
                processReferenceSchema(schema);
            } else {
                processSchemaType(schema);
            }
        }
    }

    public void processSchemaType(Schema schema) {

        if (schema instanceof ArraySchema) {
            processArraySchema((ArraySchema) schema);
        } else if (schema instanceof ComposedSchema) {
            processComposedSchema((ComposedSchema) schema);
        }

        if (schema.getProperties() != null) {
            processPropertySchema(schema);
        }
        if (schema.getNot() != null) {
            processNotSchema(schema);
        }
        if (schema.getAdditionalProperties() != null) {
            processAdditionalProperties(schema);

        }

    }

    private void processAdditionalProperties(Schema schema) {

        if (schema.getAdditionalProperties() != null) {
            if (schema.getAdditionalProperties().getRef() != null) {
                processReferenceSchema(schema.getAdditionalProperties());
            } else {
                processSchemaType(schema.getAdditionalProperties());
            }
        }
    }

    private void processNotSchema(Schema schema) {

        if (schema.getNot() != null) {
            if (schema.getNot().getRef() != null) {
                processReferenceSchema(schema.getNot());
            } else {
                processSchemaType(schema.getNot());
            }
        }
    }

    public void processPropertySchema(Schema schema) {
        if (schema.getRef() != null) {
            processReferenceSchema(schema);
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                Schema property = propertyEntry.getValue();
                if (property instanceof ArraySchema) {
                    processArraySchema((ArraySchema) property);
                }
                if (property.getRef() != null) {
                    processReferenceSchema(property);
                }
            }
        }
    }

    public void processComposedSchema(ComposedSchema composedSchema) {
        if (composedSchema.getAllOf() != null) {
            final List<Schema> schemas = composedSchema.getAllOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }
        if (composedSchema.getOneOf() != null) {
            final List<Schema> schemas = composedSchema.getOneOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }
        if (composedSchema.getAnyOf() != null) {
            final List<Schema> schemas = composedSchema.getAnyOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }

    }

    public void processArraySchema(ArraySchema arraySchema) {

        final Schema items = arraySchema.getItems();
        if (items.getRef() != null) {
            processReferenceSchema(items);
        } else {
            processSchemaType(items);
        }
    }

    private void processReferenceSchema(Schema schema) {
        /*
         * if this is a URL or relative ref: 1) we need to load it into memory.
         * 2) shove it into the #/definitions 3) update the RefModel to point to
         * its location in #/definitions
         */
        RefFormat refFormat = computeRefFormat(schema.getRef());
        String $ref = schema.getRef();

        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalSchema($ref, refFormat);

            if (newRef != null) {
                schema.setRef(newRef);
            }
        }
    }

}