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

package org.youngmonkeys.ezyvector.admin.controller.service;

import com.tvd12.ezyhttp.server.core.annotation.Service;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.exception.ResourceNotFoundException;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyvector.admin.controller.decorator.AdminEzyVectorCollectionModelDecorator;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionDetailsResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.admin.service.AdminEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionFilter;

import static org.youngmonkeys.ezyplatform.pagination.PaginationModelFetchers.getPaginationModelBySortOrder;

@Service
@AllArgsConstructor
public class AdminEzyVectorCollectionControllerService {

    private final AdminEzyVectorCollectionService vectorCollectionService;
    private final AdminEzyVectorCollectionModelDecorator vectorCollectionModelDecorator;
    private final AdminPaginationEzyVectorCollectionService paginationVectorCollectionService;
    private final AdminEzyVectorCollectionPaginationParameterConverter
        paginationParameterConverter;

    public AdminEzyVectorCollectionDetailsResponse getVectorCollectionById(
        long collectionId
    ) {
        EzyVectorCollectionModel collection = vectorCollectionService
            .getCollectionById(collectionId);
        if (collection == null) {
            throw new ResourceNotFoundException("vectorCollection");
        }
        return vectorCollectionModelDecorator
            .decorateToVectorCollectionDetailsResponse(collection);
    }

    public PaginationModel<AdminEzyVectorCollectionResponse> getVectorCollections(
        EzyVectorCollectionFilter filter,
        String sortOrder,
        String nextPageToken,
        String prevPageToken,
        boolean lastPage,
        int limit
    ) {
        PaginationModel<EzyVectorCollectionModel> pagination =
            getPaginationModelBySortOrder(
                paginationVectorCollectionService,
                paginationParameterConverter,
                filter,
                sortOrder,
                nextPageToken,
                prevPageToken,
                lastPage,
                limit
            );
        return vectorCollectionModelDecorator
            .decorateToVectorCollectionPaginationResponse(pagination);
    }
}
