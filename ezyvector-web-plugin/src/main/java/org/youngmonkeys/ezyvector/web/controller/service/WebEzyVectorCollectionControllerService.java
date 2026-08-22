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
import org.youngmonkeys.ezyplatform.exception.ResourceNotFoundException;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.web.controller.decorator.WebEzyVectorCollectionModelDecorator;
import org.youngmonkeys.ezyvector.web.response.WebGetVectorCollectionResponse;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorService;

@Service
@AllArgsConstructor
public class WebEzyVectorCollectionControllerService {

    private final WebEzyVectorService vectorDatabaseService;
    private final WebEzyVectorCollectionService vectorCollectionService;
    private final WebEzyVectorCollectionModelDecorator vectorCollectionModelDecorator;

    public void createCollectionIfAbsent(
        String collectionName,
        SaveVectorCollectionModel model
    ) throws Exception {
        vectorDatabaseService.createCollectionIfAbsent(
            collectionName,
            model
        );
    }

    public WebGetVectorCollectionResponse getVectorCollectionByName(
        String collectionName
    ) {
        EzyVectorCollectionModel collection = vectorCollectionService
            .getCollectionByName(collectionName);
        if (collection == null) {
            throw new ResourceNotFoundException("vectorCollection");
        }
        return vectorCollectionModelDecorator
            .decorateToGetVectorCollectionResponse(collection);
    }
}
