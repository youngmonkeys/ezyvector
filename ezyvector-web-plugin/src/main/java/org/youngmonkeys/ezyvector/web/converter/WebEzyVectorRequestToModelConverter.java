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

package org.youngmonkeys.ezyvector.web.converter;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorPointModel;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;
import org.youngmonkeys.ezyvector.web.request.WebUpsertVectorPointsRequest;

import java.util.ArrayList;
import java.util.List;

@EzySingleton
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

    public List<SaveVectorPointModel> toSaveVectorPointModels(
        WebUpsertVectorPointsRequest request
    ) {
        List<WebUpsertVectorPointsRequest.Point> points = request.getPoints();
        List<SaveVectorPointModel> models = new ArrayList<>(points.size());
        for (WebUpsertVectorPointsRequest.Point point : points) {
            models.add(
                SaveVectorPointModel.builder()
                    .id(point.getId())
                    .vector(toVector(point.getVector()))
                    .payload(point.getPayload())
                    .build()
            );
        }
        return models;
    }

    private float[] toVector(List<Float> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            vector[i] = values.get(i);
        }
        return vector;
    }
}
