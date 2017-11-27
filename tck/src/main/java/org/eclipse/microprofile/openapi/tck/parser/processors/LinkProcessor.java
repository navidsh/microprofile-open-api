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
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.tck.parser.ResolverCache;
import org.eclipse.microprofile.openapi.tck.parser.models.RefFormat;

public class LinkProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final HeaderProcessor headerProcessor;
    private final ExternalRefProcessor externalRefProcessor;

    public LinkProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.headerProcessor = new HeaderProcessor(cache, openAPI);
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processLink(Link link) {
        if (link.getRef() != null) {
            RefFormat refFormat = computeRefFormat(link.getRef());
            String $ref = link.getRef();
            if (isAnExternalRefFormat(refFormat)) {
                final String newRef = externalRefProcessor.processRefToExternalLink($ref, refFormat);

                if (newRef != null) {
                    link.setRef(newRef);
                }
            }

        } else if (link.getHeaders() != null) {
            Map<String, Header> headers = link.getHeaders();
            for (String headerName : headers.keySet()) {
                Header header = headers.get(headerName);
                headerProcessor.processHeader(header);
            }
        }
    }
}
