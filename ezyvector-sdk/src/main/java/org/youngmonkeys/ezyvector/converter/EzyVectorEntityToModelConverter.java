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
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.storage.EzyVectorFileStorage;

import java.util.Map;

import static com.tvd12.ezyfox.io.EzyStrings.isBlank;

@AllArgsConstructor
public class EzyVectorEntityToModelConverter {

    private final ClockProxy clock;
    private final ObjectMapper objectMapper;

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
