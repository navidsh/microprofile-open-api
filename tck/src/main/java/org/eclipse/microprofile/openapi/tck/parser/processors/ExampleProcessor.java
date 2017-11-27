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

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

public class ExampleProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public ExampleProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processExample(Example example) {
        if (example.getRef() != null) {
            processReferenceExample(example);
        }
    }

    public void processExample(List<Example> examples) {
        for (Example example : examples) {
            if (example.getRef() != null) {
                processReferenceExample(example);
            }
        }
    }

    private void processReferenceExample(Example example) {
        RefFormat refFormat = computeRefFormat(example.getRef());
        String $ref = example.getRef();
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalExample($ref, refFormat);
            if (newRef != null) {
                example.setRef(newRef);
            }
        }
    }
}
