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

package org.youngmonkeys.ezyvector.admin.controller.decorator;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyvector.admin.converter.AdminEzyVectorModelToResponseConverter;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionDetailsResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionPointResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionSegmentResponse;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;

@EzySingleton
@AllArgsConstructor
public class AdminEzyVectorCollectionModelDecorator {

    private final AdminEzyVectorModelToResponseConverter modelToResponseConverter;

    @SuppressWarnings("LineLength")
    public PaginationModel<AdminEzyVectorCollectionResponse> decorateToVectorCollectionPaginationResponse(
        PaginationModel<EzyVectorCollectionModel> pagination
    ) {
        return pagination.map(
            modelToResponseConverter::toVectorCollectionResponse
        );
    }

    @SuppressWarnings("LineLength")
    public PaginationModel<AdminEzyVectorCollectionPointResponse> decorateToVectorCollectionPointPaginationResponse(
        PaginationModel<EzyVectorCollectionPointModel> pagination
    ) {
        return pagination.map(
            modelToResponseConverter::toVectorCollectionPointResponse
        );
    }

    @SuppressWarnings("LineLength")
    public PaginationModel<AdminEzyVectorCollectionSegmentResponse> decorateToVectorCollectionSegmentPaginationResponse(
        PaginationModel<EzyVectorCollectionSegmentModel> pagination
    ) {
        return pagination.map(
            modelToResponseConverter::toVectorCollectionSegmentResponse
        );
    }

    @SuppressWarnings("LineLength")
    public AdminEzyVectorCollectionDetailsResponse decorateToVectorCollectionDetailsResponse(
        EzyVectorCollectionModel model
    ) {
        return modelToResponseConverter
            .toVectorCollectionDetailsResponse(model);
    }
}
