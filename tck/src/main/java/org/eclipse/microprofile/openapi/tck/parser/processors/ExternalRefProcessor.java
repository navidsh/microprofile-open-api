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

import static org.eclipse.microprofile.openapi.tck.parser.util.RefUtils.computeDefinitionName;
import static org.eclipse.microprofile.openapi.tck.parser.util.RefUtils.computeRefFormat;
import static org.eclipse.microprofile.openapi.tck.parser.util.RefUtils.isAnExternalRefFormat;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.ArraySchema;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

public final class ExternalRefProcessor {

    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public ExternalRefProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public String processRefToExternalSchema(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Schema schema = cache.loadRef($ref, refFormat, Schema.class);

        if (schema == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        if (schemas == null) {
            schemas = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, schemas.keySet());

        Schema existingModel = schemas.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            if (existingModel.getRef() != null) {
                // use the new model
                existingModel = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingModel == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addSchemas(newRef, schema);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (schema.getRef() != null) {
                RefFormat format = computeRefFormat(schema.getRef());
                if (isAnExternalRefFormat(format)) {
                    schema.setRef(processRefToExternalSchema(schema.getRef(), format));
                } else {
                    processRefToExternalSchema(file + schema.getRef(), RefFormat.RELATIVE);
                }
            }
            // Loop the properties and recursively call this method;
            Map<String, Schema> subProps = schema.getProperties();
            if (subProps != null) {
                for (Map.Entry<String, Schema> prop : subProps.entrySet()) {
                    if (prop.getValue().getRef() != null) {
                        processRefProperty(prop.getValue(), file);
                    } else if (prop.getValue() instanceof ArraySchema) {
                        ArraySchema arrayProp = (ArraySchema) prop.getValue();
                        if (arrayProp.getItems().getRef() != null) {
                            processRefProperty(arrayProp.getItems(), file);
                        }
                    } else if (prop.getValue().getAdditionalProperties() != null) {
                        Schema mapProp = prop.getValue();
                        if (mapProp.getAdditionalProperties().getRef() != null) {
                            processRefProperty(mapProp.getAdditionalProperties(), file);
                        } else if (mapProp.getAdditionalProperties() instanceof ArraySchema
                                && ((ArraySchema) mapProp.getAdditionalProperties()).getItems().getRef() != null) {
                            processRefProperty(((ArraySchema) mapProp.getAdditionalProperties()).getItems(), file);
                        }
                    }
                }
            }
            if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems().getRef() != null) {
                processRefProperty(((ArraySchema) schema).getItems(), file);
            }
        }

        return newRef;
    }

    public String processRefToExternalResponse(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final APIResponse response = cache.loadRef($ref, refFormat, APIResponse.class);

        if (response == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, APIResponse> responses = openAPI.getComponents().getResponses();

        if (responses == null) {
            responses = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, responses.keySet());

        APIResponse existingResponse = responses.get(possiblyConflictingDefinitionName);

        if (existingResponse != null) {
            if (existingResponse.getRef() != null) {
                // use the new model
                existingResponse = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingResponse == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addResponses(newRef, response);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (response.getRef() != null) {
                RefFormat format = computeRefFormat(response.getRef());
                if (isAnExternalRefFormat(format)) {
                    response.setRef(processRefToExternalResponse(response.getRef(), format));
                } else {
                    processRefToExternalResponse(file + response.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalRequestBody(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final RequestBody body = cache.loadRef($ref, refFormat, RequestBody.class);

        if (body == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, RequestBody> bodies = openAPI.getComponents().getRequestBodies();

        if (bodies == null) {
            bodies = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, bodies.keySet());

        RequestBody existingBody = bodies.get(possiblyConflictingDefinitionName);

        if (existingBody != null) {
            if (existingBody.getRef() != null) {
                // use the new model
                existingBody = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingBody == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addRequestBodies(newRef, body);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (body.getRef() != null) {
                RefFormat format = computeRefFormat(body.getRef());
                if (isAnExternalRefFormat(format)) {
                    body.setRef(processRefToExternalRequestBody(body.getRef(), format));
                } else {
                    processRefToExternalRequestBody(file + body.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalHeader(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Header header = cache.loadRef($ref, refFormat, Header.class);

        if (header == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Header> headers = openAPI.getComponents().getHeaders();

        if (headers == null) {
            headers = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, headers.keySet());

        Header existingHeader = headers.get(possiblyConflictingDefinitionName);

        if (existingHeader != null) {
            if (existingHeader.getRef() != null) {
                // use the new model
                existingHeader = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingHeader == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addHeaders(newRef, header);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (header.getRef() != null) {
                RefFormat format = computeRefFormat(header.getRef());
                if (isAnExternalRefFormat(format)) {
                    header.setRef(processRefToExternalHeader(header.getRef(), format));
                } else {
                    processRefToExternalHeader(file + header.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalSecurityScheme(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final SecurityScheme securityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);

        if (securityScheme == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents().getSecuritySchemes();

        if (securitySchemeMap == null) {
            securitySchemeMap = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, securitySchemeMap.keySet());

        SecurityScheme existingSecurityScheme = securitySchemeMap.get(possiblyConflictingDefinitionName);

        if (existingSecurityScheme != null) {
            if (existingSecurityScheme.getRef() != null) {
                // use the new model
                existingSecurityScheme = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingSecurityScheme == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addSecuritySchemes(newRef, securityScheme);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (securityScheme.getRef() != null) {
                RefFormat format = computeRefFormat(securityScheme.getRef());
                if (isAnExternalRefFormat(format)) {
                    securityScheme.setRef(processRefToExternalSecurityScheme(securityScheme.getRef(), format));
                } else {
                    processRefToExternalSecurityScheme(file + securityScheme.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalLink(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Link link = cache.loadRef($ref, refFormat, Link.class);

        if (link == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Link> links = openAPI.getComponents().getLinks();

        if (links == null) {
            links = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, links.keySet());

        Link existingLink = links.get(possiblyConflictingDefinitionName);

        if (existingLink != null) {
            if (existingLink.getRef() != null) {
                // use the new model
                existingLink = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingLink == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addLinks(newRef, link);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (link.getRef() != null) {
                RefFormat format = computeRefFormat(link.getRef());
                if (isAnExternalRefFormat(format)) {
                    link.setRef(processRefToExternalLink(link.getRef(), format));
                } else {
                    processRefToExternalLink(file + link.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalExample(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Example example = cache.loadRef($ref, refFormat, Example.class);

        if (example == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Example> examples = openAPI.getComponents().getExamples();

        if (examples == null) {
            examples = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, examples.keySet());

        Example existingExample = examples.get(possiblyConflictingDefinitionName);

        if (existingExample != null) {
            if (existingExample.getRef() != null) {
                // use the new model
                existingExample = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingExample == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addExamples(newRef, example);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (example.getRef() != null) {
                RefFormat format = computeRefFormat(example.getRef());
                if (isAnExternalRefFormat(format)) {
                    example.setRef(processRefToExternalExample(example.getRef(), format));
                } else {
                    processRefToExternalExample(file + example.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalParameter(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Parameter parameter = cache.loadRef($ref, refFormat, Parameter.class);

        if (parameter == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();

        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, parameters.keySet());

        Parameter existingParameters = parameters.get(possiblyConflictingDefinitionName);

        if (existingParameters != null) {
            if (existingParameters.getRef() != null) {
                // use the new model
                existingParameters = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingParameters == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addParameters(newRef, parameter);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (parameter.getRef() != null) {
                RefFormat format = computeRefFormat(parameter.getRef());
                if (isAnExternalRefFormat(format)) {
                    parameter.setRef(processRefToExternalParameter(parameter.getRef(), format));
                } else {
                    processRefToExternalParameter(file + parameter.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalCallback(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Callback callback = cache.loadRef($ref, refFormat, Callback.class);

        if (callback == null) {
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createObject(Components.class));
        }
        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();

        if (callbacks == null) {
            callbacks = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, callback.keySet());

        Callback existingCallback = callbacks.get(possiblyConflictingDefinitionName);

        if (existingCallback != null) {
            if (existingCallback.get("$ref").getRef() != null) {
                // use the new model
                existingCallback = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingCallback == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addCallbacks(newRef, callback);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (callback.get("$ref") != null) {
                if (callback.get("$ref").getRef() != null) {
                    RefFormat format = computeRefFormat(callback.get("$ref").getRef());
                    if (isAnExternalRefFormat(format)) {
                        callback.get("$ref").setRef(processRefToExternalCallback(callback.get("$ref").getRef(), format));
                    } else {
                        processRefToExternalCallback(file + callback.get("$ref").getRef(), RefFormat.RELATIVE);
                    }
                }
            }
        }

        return newRef;
    }

    private void processRefProperty(Schema subRef, String externalFile) {
        RefFormat format = computeRefFormat(subRef.getRef());
        if (isAnExternalRefFormat(format)) {
            String $ref = constructRef(subRef, externalFile);
            subRef.setRef($ref);
            if ($ref.startsWith("."))
                processRefToExternalSchema($ref, RefFormat.RELATIVE);
            else {
                processRefToExternalSchema($ref, RefFormat.URL);
            }

        } else {
            processRefToExternalSchema(externalFile + subRef.getRef(), RefFormat.RELATIVE);
        }
    }

    protected String constructRef(Schema refProperty, String rootLocation) {
        String ref = refProperty.getRef();
        return join(rootLocation, ref);
    }

    public static String join(String source, String fragment) {
        try {
            boolean isRelative = false;
            if (source.startsWith("/") || source.startsWith(".")) {
                isRelative = true;
            }
            URI uri = new URI(source);

            if (!source.endsWith("/") && (fragment.startsWith("./") && "".equals(uri.getPath()))) {
                uri = new URI(source + "/");
            } else if ("".equals(uri.getPath()) && !fragment.startsWith("/")) {
                uri = new URI(source + "/");
            }
            URI f = new URI(fragment);

            URI resolved = uri.resolve(f);

            URI normalized = resolved.normalize();
            if (Character.isAlphabetic(normalized.toString().charAt(0)) && isRelative) {
                return "./" + normalized.toString();
            }
            return normalized.toString();
        } catch (Exception e) {
            return source;
        }
    }

}