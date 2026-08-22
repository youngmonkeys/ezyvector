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
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPointStatus;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionStatus;
import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorPointModel;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
public class EzyVectorModelToEntityConverter {

    private final ClockProxy clock;
    private final ObjectMapper objectMapper;


    public EzyVectorCollection toVectorCollectionEntity(
        String collectionName,
        SaveVectorCollectionModel model
    ) {
        SaveVectorCollectionModel.Vectors vectors
            = model.getVectors();
        EzyVectorCollection entity = new EzyVectorCollection();
        entity.setName(collectionName);
        entity.setVectorSize(vectors.getSize());
        entity.setDistance(vectors.getDistance());
        entity.setIndexType("HNSW");
        entity.setStatus(
            EzyVectorCollectionStatus.ACTIVATED.toString()
        );
        LocalDateTime now = clock.nowDateTime();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    };

    public EzyVectorCollectionPoint toVectorCollectionPointEntity(
        long collectionId,
        SaveVectorPointModel point
    ) throws Exception {
        EzyVectorCollectionPoint entity =
            new EzyVectorCollectionPoint();
        entity.setCollectionId(collectionId);
        entity.setPointId(point.getId());
        entity.setStatus(
            EzyVectorCollectionPointStatus.LIVE.toString()
        );
        mergeToCollectionPointEntity(point, entity);
        entity.setCreatedAt(entity.getUpdatedAt());
        return entity;
    }

    public void mergeToCollectionPointEntity(
        SaveVectorPointModel point,
        EzyVectorCollectionPoint entity
    ) throws Exception {
        entity.setVersion(entity.getVersion() + 1L);
        entity.setVector(point.getVector());
        entity.setPayload(toPayloadJson(point.getPayload()));
        entity.setUpdatedAt(clock.nowDateTime());
    }

    public String toPayloadJson(
        Map<String, Object> payload
    ) throws Exception {
        return payload == null
            ? null
            : objectMapper.writeValueAsString(payload);
    }
}
