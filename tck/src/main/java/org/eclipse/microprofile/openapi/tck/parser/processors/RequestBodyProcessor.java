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

import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

public class RequestBodyProcessor {
    private final SchemaProcessor schemaProcessor;
    private final ExternalRefProcessor externalRefProcessor;
    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public RequestBodyProcessor(ResolverCache cache, OpenAPI openAPI) {
        schemaProcessor = new SchemaProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public void processRequestBody(RequestBody requestBody) {

        if (requestBody.getRef() != null) {
            processReferenceRequestBody(requestBody);
        }
        Schema schema = null;
        if (requestBody.getContent() != null) {
            Map<String, MediaType> content = requestBody.getContent();
            for (String mediaName : content.keySet()) {
                MediaType mediaType = content.get(mediaName);
                if (mediaType.getSchema() != null) {
                    schema = mediaType.getSchema();
                    if (schema != null) {
                        schemaProcessor.processSchema(schema);
                    }
                }
            }
        }
    }

    public void processReferenceRequestBody(RequestBody requestBody) {
        RefFormat refFormat = computeRefFormat(requestBody.getRef());
        String $ref = requestBody.getRef();
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalRequestBody($ref, refFormat);

            if (newRef != null) {
                requestBody.setRef(newRef);
            }
        }
    }

}
