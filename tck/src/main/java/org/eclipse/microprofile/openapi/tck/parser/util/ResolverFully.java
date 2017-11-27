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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.ArraySchema;
import org.eclipse.microprofile.openapi.models.media.ComposedSchema;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.ObjectSchema;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

public class ResolverFully {

    private boolean aggregateCombinators;

    public ResolverFully() {
        this(true);
    }

    public ResolverFully(boolean aggregateCombinators) {
        this.aggregateCombinators = aggregateCombinators;
    }

    private Map<String, Schema> schemas;
    private Map<String, Schema> resolvedModels = new HashMap<>();
    private Map<String, Example> examples;

    public void resolveFully(OpenAPI openAPI) {
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            schemas = openAPI.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new HashMap<>();
            }
        }

        if (openAPI.getComponents() != null && openAPI.getComponents().getExamples() != null) {
            examples = openAPI.getComponents().getExamples();
            if (examples == null) {
                examples = new HashMap<>();
            }
        }

        if (openAPI.getPaths() != null) {
            for (String pathname : openAPI.getPaths().keySet()) {
                PathItem pathItem = openAPI.getPaths().get(pathname);
                resolvePath(pathItem);
            }
        }
    }

    public void resolvePath(PathItem pathItem) {
        for (Operation op : pathItem.readOperations()) {
            // inputs
            if (op.getParameters() != null) {
                for (Parameter parameter : op.getParameters()) {
                    if (parameter.getSchema() != null) {
                        Schema resolved = resolveSchema(parameter.getSchema());
                        if (resolved != null) {
                            parameter.setSchema(resolved);
                        }
                    }
                    if (parameter.getContent() != null) {
                        Map<String, MediaType> content = parameter.getContent();
                        for (String key : content.keySet()) {
                            if (content.get(key) != null && content.get(key).getSchema() != null) {
                                Schema resolvedSchema = resolveSchema(content.get(key).getSchema());
                                if (resolvedSchema != null) {
                                    content.get(key).setSchema(resolvedSchema);
                                }
                            }
                        }
                    }
                }
            }

            if (op.getCallbacks() != null) {
                Map<String, Callback> callbacks = op.getCallbacks();
                for (String name : callbacks.keySet()) {
                    Callback callback = callbacks.get(name);
                    if (callback != null) {
                        for (String callbackName : callback.keySet()) {
                            PathItem path = callback.get(callbackName);
                            if (path != null) {
                                resolvePath(path);
                            }

                        }
                    }
                }
            }

            if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
                Map<String, MediaType> content = op.getRequestBody().getContent();
                for (String key : content.keySet()) {
                    if (content.get(key) != null && content.get(key).getSchema() != null) {
                        Schema resolved = resolveSchema(content.get(key).getSchema());
                        if (resolved != null) {
                            content.get(key).setSchema(resolved);
                        }
                    }
                }
            }
            // responses
            if (op.getResponses() != null) {
                for (String code : op.getResponses().keySet()) {
                    APIResponse response = op.getResponses().get(code);
                    if (response.getContent() != null) {
                        Map<String, MediaType> content = response.getContent();
                        for (String mediaType : content.keySet()) {
                            if (content.get(mediaType).getSchema() != null) {
                                Schema resolved = resolveSchema(content.get(mediaType).getSchema());
                                response.getContent().get(mediaType).setSchema(resolved);
                            }
                            if (content.get(mediaType).getExamples() != null) {
                                Map<String, Example> resolved = resolveExample(content.get(mediaType).getExamples());
                                response.getContent().get(mediaType).setExamples(resolved);

                            }
                        }
                    }
                }
            }
        }
    }

    public Schema resolveSchema(Schema schema) {
        if (schema.getRef() != null) {
            String ref = schema.getRef();
            ref = ref.substring(ref.lastIndexOf("/") + 1);
            Schema resolved = schemas.get(ref);
            if (resolved == null) {
                return schema;
            }
            if (this.resolvedModels.containsKey(ref)) {
                return this.resolvedModels.get(ref);
            }
            this.resolvedModels.put(ref, schema);

            Schema model = resolveSchema(resolved);

            // if we make it without a resolution loop, we can update the
            // reference
            this.resolvedModels.put(ref, model);
            return model;
        }

        if (schema instanceof ArraySchema) {
            ArraySchema arrayModel = (ArraySchema) schema;
            if (arrayModel.getItems().getRef() != null) {
                arrayModel.setItems(resolveSchema(arrayModel.getItems()));
            } else {
                arrayModel.setItems(arrayModel.getItems());
            }

            return arrayModel;
        }
        if (schema instanceof ObjectSchema) {
            ObjectSchema obj = (ObjectSchema) schema;
            if (obj.getProperties() != null) {
                Map<String, Schema> updated = new LinkedHashMap<>();
                for (String propertyName : obj.getProperties().keySet()) {
                    Schema innerProperty = obj.getProperties().get(propertyName);
                    // reference check
                    if (schema != innerProperty) {
                        Schema resolved = resolveSchema(innerProperty);
                        updated.put(propertyName, resolved);
                    }
                }
                obj.setProperties(updated);
            }
            return obj;
        }

        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            if (aggregateCombinators) {
                Schema model = SchemaTypeUtil.createSchema(composedSchema.getType(), composedSchema.getFormat());
                Set<String> requiredProperties = new HashSet<>();
                if (composedSchema.getAllOf() != null) {
                    for (Schema innerModel : composedSchema.getAllOf()) {
                        Schema resolved = resolveSchema(innerModel);
                        Map<String, Schema> properties = resolved.getProperties();
                        if (resolved.getProperties() != null) {
                            for (String key : properties.keySet()) {
                                Schema prop = (Schema) resolved.getProperties().get(key);
                                model.addProperties(key, resolveSchema(prop));
                            }
                            if (resolved.getRequired() != null) {
                                for (int i = 0; i < resolved.getRequired().size(); i++) {
                                    if (resolved.getRequired().get(i) != null) {
                                        requiredProperties.add(resolved.getRequired().get(i).toString());
                                    }
                                }
                            }
                        }
                        if (requiredProperties.size() > 0) {
                            model.setRequired(new ArrayList<>(requiredProperties));
                        }
                        if (composedSchema.getExtensions() != null) {
                            Map<String, Object> extensions = composedSchema.getExtensions();
                            for (String key : extensions.keySet()) {
                                model.addExtension(key, composedSchema.getExtensions().get(key));
                            }
                        }
                        return model;
                    }

                } else if (composedSchema.getOneOf() != null) {
                    Schema resolved;
                    List<Schema> list = new ArrayList<>();
                    for (Schema innerModel : composedSchema.getOneOf()) {
                        resolved = resolveSchema(innerModel);
                        list.add(resolved);
                    }
                    composedSchema.setOneOf(list);

                } else if (composedSchema.getAnyOf() != null) {
                    Schema resolved;
                    List<Schema> list = new ArrayList<>();
                    for (Schema innerModel : composedSchema.getAnyOf()) {
                        resolved = resolveSchema(innerModel);
                        list.add(resolved);
                    }
                    composedSchema.setAnyOf(list);
                }

                return composedSchema;
            } else {
                // User don't want to aggregate composed schema, we only solve
                // refs
                if (composedSchema.getAllOf() != null)
                    composedSchema.allOf(composedSchema.getAllOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                else if (composedSchema.getOneOf() != null)
                    composedSchema.oneOf(composedSchema.getOneOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                else if (composedSchema.getAnyOf() != null)
                    composedSchema.anyOf(composedSchema.getAnyOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                return composedSchema;
            }
        }

        if (schema.getProperties() != null) {
            Schema model = schema;
            Map<String, Schema> updated = new LinkedHashMap<>();
            Map<String, Schema> properties = model.getProperties();
            for (String propertyName : properties.keySet()) {
                Schema property = (Schema) model.getProperties().get(propertyName);
                Schema resolved = resolveSchema(property);
                updated.put(propertyName, resolved);
            }

            for (String key : updated.keySet()) {
                Schema property = updated.get(key);

                if (property instanceof ObjectSchema) {
                    ObjectSchema op = (ObjectSchema) property;
                    if (op.getProperties() != model.getProperties()) {
                        if (property.getType() == null) {
                            property.setType("object");
                        }
                        model.addProperties(key, property);
                    } else {
                        ObjectSchema newSchema = OASFactory.createObject(ObjectSchema.class);
                        model.addProperties(key, newSchema);
                    }
                }

            }
            return model;
        }

        return schema;
    }

    public Map<String, Example> resolveExample(Map<String, Example> examples) {

        Map<String, Example> resolveExamples = examples;

        if (examples != null) {

            for (String name : examples.keySet()) {
                if (examples.get(name).getRef() != null) {
                    String ref = examples.get(name).getRef();
                    ref = ref.substring(ref.lastIndexOf("/") + 1);
                    Example sample = this.examples.get(ref);
                    resolveExamples.replace(name, sample);
                }
            }
        }

        return resolveExamples;

    }
}
