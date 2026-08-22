package org.youngmonkeys.ezyvector.admin.converter;

import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionDetailsResponse;
import org.youngmonkeys.ezyvector.admin.response.AdminEzyVectorCollectionResponse;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;

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
}
