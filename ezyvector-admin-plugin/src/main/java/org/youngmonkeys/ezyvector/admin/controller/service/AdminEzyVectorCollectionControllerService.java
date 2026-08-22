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
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionPointPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionSegmentPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionDetailsResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionPointResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionSegmentResponse;
import org.youngmonkeys.ezyvector.admin.service.AdminEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionPointService;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionSegmentService;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentFilter;

import static org.youngmonkeys.ezyplatform.pagination.PaginationModelFetchers.getPaginationModelBySortOrder;

@Service
@AllArgsConstructor
public class AdminEzyVectorCollectionControllerService {

    private final AdminEzyVectorCollectionService vectorCollectionService;
    private final AdminEzyVectorCollectionModelDecorator vectorCollectionModelDecorator;
    private final AdminPaginationEzyVectorCollectionService paginationVectorCollectionService;
    private final AdminPaginationEzyVectorCollectionPointService
        paginationVectorCollectionPointService;
    private final AdminPaginationEzyVectorCollectionSegmentService
        paginationVectorCollectionSegmentService;
    private final AdminEzyVectorCollectionPaginationParameterConverter
        paginationParameterConverter;
    private final AdminEzyVectorCollectionPointPaginationParameterConverter
        pointPaginationParameterConverter;
    private final AdminEzyVectorCollectionSegmentPaginationParameterConverter
        segmentPaginationParameterConverter;

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

    @SuppressWarnings("LineLength")
    public PaginationModel<AdminEzyVectorCollectionPointResponse> getVectorCollectionPoints(
        EzyVectorCollectionPointFilter filter,
        String sortOrder,
        String nextPageToken,
        String prevPageToken,
        boolean lastPage,
        int limit
    ) {
        PaginationModel<EzyVectorCollectionPointModel> pagination =
            getPaginationModelBySortOrder(
                paginationVectorCollectionPointService,
                pointPaginationParameterConverter,
                filter,
                sortOrder,
                nextPageToken,
                prevPageToken,
                lastPage,
                limit
            );
        return vectorCollectionModelDecorator
            .decorateToVectorCollectionPointPaginationResponse(pagination);
    }

    @SuppressWarnings("LineLength")
    public PaginationModel<AdminEzyVectorCollectionSegmentResponse> getVectorCollectionSegments(
        EzyVectorCollectionSegmentFilter filter,
        String sortOrder,
        String nextPageToken,
        String prevPageToken,
        boolean lastPage,
        int limit
    ) {
        PaginationModel<EzyVectorCollectionSegmentModel> pagination =
            getPaginationModelBySortOrder(
                paginationVectorCollectionSegmentService,
                segmentPaginationParameterConverter,
                filter,
                sortOrder,
                nextPageToken,
                prevPageToken,
                lastPage,
                limit
            );
        return vectorCollectionModelDecorator
            .decorateToVectorCollectionSegmentPaginationResponse(pagination);
    }
}
