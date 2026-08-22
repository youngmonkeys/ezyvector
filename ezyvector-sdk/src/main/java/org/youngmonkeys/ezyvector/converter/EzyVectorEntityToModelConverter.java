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

package org.youngmonkeys.ezyvector.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.time.ClockProxy;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollection;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionSegment;
import org.youngmonkeys.ezyvector.hnsw.HnswIndex;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.storage.EzyVectorFileStorage;

import java.util.Map;

import static com.tvd12.ezyfox.io.EzyStrings.isBlank;

@AllArgsConstructor
public class EzyVectorEntityToModelConverter {

    private final ClockProxy clock;
    private final ObjectMapper objectMapper;

    public EzyVectorCollectionModel toModel(
        EzyVectorCollection entity
    ) {
        if (entity == null) {
            return null;
        }
        return EzyVectorCollectionModel.builder()
            .id(entity.getId())
            .name(entity.getName())
            .vectorSize(entity.getVectorSize())
            .distance(entity.getDistance())
            .indexType(entity.getIndexType())
            .status(entity.getStatus())
            .pointsCount(entity.getPointsCount())
            .config(entity.getConfig())
            .createdAt(clock.toTimestamp(entity.getCreatedAt()))
            .updatedAt(clock.toTimestamp(entity.getUpdatedAt()))
            .build();
    }

    public EzyVectorCollectionPointModel toModel(
        EzyVectorCollectionPoint entity
    ) {
        return EzyVectorCollectionPointModel.builder()
            .id(entity.getId())
            .collectionId(entity.getCollectionId())
            .pointId(entity.getPointId())
            .vector(entity.getVector())
            .payload(entity.getPayload())
            .status(entity.getStatus())
            .version(entity.getVersion())
            .createdAt(clock.toTimestamp(entity.getCreatedAt()))
            .updatedAt(clock.toTimestamp(entity.getUpdatedAt()))
            .build();
    }

    public EzyVectorCollectionSegmentModel toModel(
        EzyVectorCollectionSegment entity
    ) {
        return EzyVectorCollectionSegmentModel.builder()
            .id(entity.getId())
            .collectionId(entity.getCollectionId())
            .segmentNo(entity.getSegmentNo())
            .segmentType(entity.getSegmentType())
            .status(entity.getStatus())
            .pointsCount(entity.getPointsCount())
            .minPointId(entity.getMinPointId())
            .maxPointId(entity.getMaxPointId())
            .indexVersion(entity.getIndexVersion())
            .createdAt(clock.toTimestamp(entity.getCreatedAt()))
            .updatedAt(clock.toTimestamp(entity.getUpdatedAt()))
            .build();
    }

    public EzyVectorSearchResultModel toSearchResultModel(
        HnswIndex.SearchResult hit,
        EzyVectorCollectionPoint entity
    ) throws Exception {
        return EzyVectorSearchResultModel.builder()
            .chunkId(hit.getId())
            .score(hit.getScore())
            .payload(
                entity == null
                    ? null
                    : toPayloadMap(entity.getPayload())
            )
            .build();
    }

    public EzyVectorSearchResultModel toSearchResultModel(
        EzyVectorFileStorage.SearchResult hit,
        EzyVectorCollectionPoint entity
    ) throws Exception {
        return EzyVectorSearchResultModel.builder()
            .chunkId(hit.getId())
            .score(hit.getScore())
            .payload(
                entity == null
                    ? null
                    : toPayloadMap(entity.getPayload())
            )
            .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toPayloadMap(
        String json
    ) throws Exception {
        return isBlank(json)
            ? null
            : objectMapper.readValue(json, Map.class);
    }
}
