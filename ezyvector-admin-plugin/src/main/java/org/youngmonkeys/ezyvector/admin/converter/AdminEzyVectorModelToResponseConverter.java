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

package org.youngmonkeys.ezyvector.admin.converter;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionDetailsResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionPointResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionSegmentResponse;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;

@EzySingleton
public class AdminEzyVectorModelToResponseConverter {

    public AdminEzyVectorCollectionResponse toVectorCollectionResponse(
        EzyVectorCollectionModel model
    ) {
        return AdminEzyVectorCollectionResponse.builder()
            .id(model.getId())
            .name(model.getName())
            .vectorSize(model.getVectorSize())
            .distance(model.getDistance())
            .indexType(model.getIndexType())
            .status(model.getStatus())
            .pointsCount(model.getPointsCount())
            .config(model.getConfig())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }

    public AdminEzyVectorCollectionDetailsResponse toVectorCollectionDetailsResponse(
        EzyVectorCollectionModel model
    ) {
        return AdminEzyVectorCollectionDetailsResponse.builder()
            .id(model.getId())
            .name(model.getName())
            .vectorSize(model.getVectorSize())
            .distance(model.getDistance())
            .indexType(model.getIndexType())
            .status(model.getStatus())
            .pointsCount(model.getPointsCount())
            .config(model.getConfig())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }

    public AdminEzyVectorCollectionPointResponse toVectorCollectionPointResponse(
        EzyVectorCollectionPointModel model
    ) {
        return AdminEzyVectorCollectionPointResponse.builder()
            .id(model.getId())
            .collectionId(model.getCollectionId())
            .pointId(model.getPointId())
            .vectorSize(
                model.getVector() != null
                    ? model.getVector().length
                    : 0
            )
            .payload(model.getPayload())
            .status(model.getStatus())
            .version(model.getVersion())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }

    public AdminEzyVectorCollectionSegmentResponse toVectorCollectionSegmentResponse(
        EzyVectorCollectionSegmentModel model
    ) {
        return AdminEzyVectorCollectionSegmentResponse.builder()
            .id(model.getId())
            .collectionId(model.getCollectionId())
            .segmentNo(model.getSegmentNo())
            .segmentType(model.getSegmentType())
            .status(model.getStatus())
            .pointsCount(model.getPointsCount())
            .minPointId(model.getMinPointId())
            .maxPointId(model.getMaxPointId())
            .indexVersion(model.getIndexVersion())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }
}
