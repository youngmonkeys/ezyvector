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
import org.youngmonkeys.ezyvector.web.controller.decorator.WebEzyVectorPointsModelDecorator;
import org.youngmonkeys.ezyvector.web.converter.WebEzyVectorRequestToModelConverter;
import org.youngmonkeys.ezyvector.web.request.WebUpsertVectorPointsRequest;
import org.youngmonkeys.ezyvector.web.response.WebUpsertVectorPointsResponse;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorService;

@Service
@AllArgsConstructor
public class WebEzyVectorPointsControllerService {

    private final WebEzyVectorService vectorService;
    private final WebEzyVectorRequestToModelConverter vectorRequestToModelConverter;
    private final WebEzyVectorPointsModelDecorator vectorPointsModelDecorator;

    public WebUpsertVectorPointsResponse upsertPoints(
        String collectionName,
        WebUpsertVectorPointsRequest request
    ) throws Exception {
        vectorService.upsert(
            collectionName,
            vectorRequestToModelConverter.toSaveVectorPointModels(request)
        );
        return vectorPointsModelDecorator
            .decorateToUpsertVectorPointsResponse();
    }
}
