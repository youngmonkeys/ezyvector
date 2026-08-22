package org.youngmonkeys.ezyvector.web.converter;

import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;

public class WebEzyVectorRequestToModelConverter {

    public SaveVectorCollectionModel toSaveVectorCollectionModel(
        WebCreateVectorCollectionRequest request
    ) {
        WebCreateVectorCollectionRequest.Vectors vectors =
            request.getVectors();
        return SaveVectorCollectionModel.builder()
            .vectors(
                SaveVectorCollectionModel.Vectors
                    .builder()
                    .size(vectors.getSize())
                    .distance(vectors.getDistance())
                    .build()
            )
            .build();
    }
}
