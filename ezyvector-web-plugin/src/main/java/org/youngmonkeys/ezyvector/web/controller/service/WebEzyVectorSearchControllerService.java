/*
 * Copyright 2026 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.youngmonkeys.ezyvector.web.controller.service;

import com.tvd12.ezyhttp.server.core.annotation.Service;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.web.controller.decorator.WebEzyVectorSearchModelDecorator;
import org.youngmonkeys.ezyvector.web.request.WebEzyVectorSearchRequest;
import org.youngmonkeys.ezyvector.web.response.WebEzyVectorSearchResponse;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorService;

import java.util.List;

@Service
@AllArgsConstructor
public class WebEzyVectorSearchControllerService {

    private final WebEzyVectorService vectorService;
    private final WebEzyVectorSearchModelDecorator vectorSearchModelDecorator;

    public WebEzyVectorSearchResponse search(
        String collectionName,
        WebEzyVectorSearchRequest request
    ) throws Exception {
        List<EzyVectorSearchResultModel> results = vectorService.search(
            collectionName,
            request.getVector(),
            request.getLimit()
        );
        return vectorSearchModelDecorator.decorateToSearchResponse(results);
    }
}
