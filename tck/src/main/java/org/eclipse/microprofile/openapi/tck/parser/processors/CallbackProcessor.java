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
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class CallbackProcessor {
    private final ResolverCache cache;
    private final OperationProcessor operationProcessor;
    private final ParameterProcessor parameterProcessor;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public CallbackProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.operationProcessor = new OperationProcessor(cache, openAPI);
        this.parameterProcessor = new ParameterProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
        this.openAPI = openAPI;
    }

    public void processCallback(Callback callback) {
        if (callback.get("$ref") != null) {
            processReferenceCallback(callback);
        }
        // Resolver PathItem
        for (String name : callback.keySet()) {
            PathItem pathItem = callback.get(name);
            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }

            List<Parameter> parameters = pathItem.getParameters();
            if (parameters != null) {
                for (Parameter parameter : parameters) {
                    parameterProcessor.processParameter(parameter);
                }
            }
        }
    }

    public void processReferenceCallback(Callback callback) {
        String $ref = callback.get("$ref").getRef();
        RefFormat refFormat = computeRefFormat($ref);
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalCallback($ref, refFormat);
            if (newRef != null) {
                callback.get("$ref").setRef("#/components/callbacks/" + newRef);
            }
        }
    }
}
