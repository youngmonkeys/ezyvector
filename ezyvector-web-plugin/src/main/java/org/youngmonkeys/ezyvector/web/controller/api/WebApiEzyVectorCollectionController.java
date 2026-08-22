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

package org.youngmonkeys.ezyvector.web.controller.api;

import com.tvd12.ezyfox.util.EzyMapBuilder;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.DoPut;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.model.SaveVectorPointModel;
import org.youngmonkeys.ezyvector.web.controller.service.WebEzyVectorCollectionControllerService;
import org.youngmonkeys.ezyvector.web.converter.WebEzyVectorRequestToModelConverter;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;
import org.youngmonkeys.ezyvector.web.request.WebEzyVectorSearchRequest;
import org.youngmonkeys.ezyvector.web.response.WebCreateVectorCollectionResponse;
import org.youngmonkeys.ezyvector.web.response.WebGetVectorCollectionResponse;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorService;
import org.youngmonkeys.ezyvector.web.validator.WebEzyVectorCollectionValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.youngmonkeys.ezyplatform.util.Numbers.toLongOrZeroFromObject;

@Controller("/collections")
@AllArgsConstructor
public class WebApiEzyVectorCollectionController {

    private final WebEzyVectorService vectorDatabaseService;
    private final WebEzyVectorCollectionControllerService vectorCollectionControllerService;
    private final WebEzyVectorCollectionValidator vectorCollectionValidator;
    private final WebEzyVectorRequestToModelConverter vectorRequestToModelConverter;

    @DoPut("/{collectionName}")
    public WebCreateVectorCollectionResponse collectionNamePut(
        @PathVariable String collectionName,
        @RequestBody WebCreateVectorCollectionRequest request
    ) throws Exception {
        vectorCollectionValidator.validate(request);
        vectorDatabaseService.createCollectionIfAbsent(
            collectionName,
            vectorRequestToModelConverter.toSaveVectorCollectionModel(
                request
            )
        );
        return WebCreateVectorCollectionResponse.builder()
            .result(Boolean.TRUE)
            .status("ok")
            .build();
    }

    @DoGet("/{collectionName}")
    public WebGetVectorCollectionResponse collectionNameGet(
        @PathVariable String collectionName
    ) {
        return vectorCollectionControllerService
            .getVectorCollectionByName(collectionName);
    }

    @SuppressWarnings("unchecked")
    @DoPut("/{collectionName}/points")
    public Map<String, Object> collectionNamePointsPut(
        @PathVariable String collectionName,
        @RequestBody Map<String, Object> request
    ) throws Exception {
        List<Map<String, Object>> points =
            (List<Map<String, Object>>) request.get("points");
        vectorDatabaseService.upsert(
            collectionName,
            toPointModels(points)
        );
        return
    }

    @SuppressWarnings("unchecked")
    @DoPost("/{collectionName}/points/search")
    public Map<String, Object> collectionNamePointsSearchPost(
        @PathVariable String collectionName,
        @RequestBody WebEzyVectorSearchRequest request
    ) throws Exception {
        List<EzyVectorSearchResultModel> results = vectorDatabaseService.search(
            collectionName,
            request.getVector(),
            request.getLimit()
        );
        return okResult(toResultMaps(results));
    }

    @SuppressWarnings("unchecked")
    private List<SaveVectorPointModel> toPointModels(
        List<Map<String, Object>> points
    ) {
        List<SaveVectorPointModel> models = new ArrayList<>(points.size());
        for (Map<String, Object> point : points) {
            models.add(
                SaveVectorPointModel.builder()
                    .id(toLongOrZeroFromObject(point.get("id")))
                    .vector(toVector((List<Object>) point.get("vector")))
                    .payload((Map<String, Object>) point.get("payload"))
                    .build()
            );
        }
        return models;
    }

    private List<Map<String, Object>> toResultMaps(
        List<EzyVectorSearchResultModel> results
    ) {
        List<Map<String, Object>> resultMaps = new ArrayList<>(results.size());
        for (EzyVectorSearchResultModel result : results) {
            resultMaps.add(
                EzyMapBuilder.mapBuilder()
                    .put("id", result.getChunkId())
                    .put("score", result.getScore())
                    .put("payload", result.getPayload())
                    .toMap()
            );
        }
        return resultMaps;
    }

    private float[] toVector(List<Object> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            vector[i] = ((Number) values.get(i)).floatValue();
        }
        return vector;
    }
}
