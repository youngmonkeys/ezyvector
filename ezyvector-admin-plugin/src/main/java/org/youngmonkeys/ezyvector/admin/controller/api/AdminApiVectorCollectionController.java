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

package org.youngmonkeys.ezyvector.admin.controller.api;

import com.tvd12.ezyfox.annotation.EzyFeature;
import com.tvd12.ezyhttp.core.annotation.Description;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.annotation.RequestParam;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.admin.validator.AdminCommonValidator;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyvector.admin.controller.service.AdminEzyVectorCollectionControllerService;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionPointResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionSegmentResponse;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionFilter;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionPointFilter;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionSegmentFilter;

import static org.youngmonkeys.ezyplatform.util.StringConverters.trimOrNull;

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("rag")
@AllArgsConstructor
public class AdminApiVectorCollectionController {

    private final AdminEzyVectorCollectionControllerService
        vectorCollectionControllerService;
    private final AdminCommonValidator commonValidator;

    @Description("Get the data chunks with pagination")
    @DoGet("/vector-collections")
    public PaginationModel<AdminEzyVectorCollectionResponse> vectorCollectionsGet(
        @RequestParam(value = "keyword") String keyword,
        @RequestParam(value = "status") String status,
        @RequestParam(value = "sortOrder") String sortOrder,
        @RequestParam(value = "nextPageToken") String nextPageToken,
        @RequestParam(value = "prevPageToken") String prevPageToken,
        @RequestParam(value = "lastPage") boolean lastPage,
        @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        commonValidator.validatePageSize(limit);
        return vectorCollectionControllerService.getVectorCollections(
            DefaultEzyVectorCollectionFilter.builder()
                .likeKeyword(trimOrNull(keyword))
                .status(trimOrNull(status))
                .build(),
            sortOrder,
            nextPageToken,
            prevPageToken,
            lastPage,
            limit
        );
    }

    @SuppressWarnings("LineLength")
    @Description("Get the vector collection points with pagination")
    @DoGet("/vector-collections/{collectionId}/points")
    public PaginationModel<AdminEzyVectorCollectionPointResponse> vectorCollectionPointsGet(
        @PathVariable long collectionId,
        @RequestParam(value = "status") String status,
        @RequestParam(value = "sortOrder") String sortOrder,
        @RequestParam(value = "nextPageToken") String nextPageToken,
        @RequestParam(value = "prevPageToken") String prevPageToken,
        @RequestParam(value = "lastPage") boolean lastPage,
        @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        commonValidator.validatePageSize(limit);
        return vectorCollectionControllerService.getVectorCollectionPoints(
            DefaultEzyVectorCollectionPointFilter.builder()
                .collectionId(collectionId)
                .status(trimOrNull(status))
                .build(),
            sortOrder,
            nextPageToken,
            prevPageToken,
            lastPage,
            limit
        );
    }

    @SuppressWarnings("LineLength")
    @Description("Get the vector collection segments with pagination")
    @DoGet("/vector-collections/{collectionId}/segments")
    public PaginationModel<AdminEzyVectorCollectionSegmentResponse> vectorCollectionSegmentsGet(
        @PathVariable long collectionId,
        @RequestParam(value = "status") String status,
        @RequestParam(value = "sortOrder") String sortOrder,
        @RequestParam(value = "nextPageToken") String nextPageToken,
        @RequestParam(value = "prevPageToken") String prevPageToken,
        @RequestParam(value = "lastPage") boolean lastPage,
        @RequestParam(value = "limit", defaultValue = "30") int limit
    ) {
        commonValidator.validatePageSize(limit);
        return vectorCollectionControllerService.getVectorCollectionSegments(
            DefaultEzyVectorCollectionSegmentFilter.builder()
                .collectionId(collectionId)
                .status(trimOrNull(status))
                .build(),
            sortOrder,
            nextPageToken,
            prevPageToken,
            lastPage,
            limit
        );
    }
}
