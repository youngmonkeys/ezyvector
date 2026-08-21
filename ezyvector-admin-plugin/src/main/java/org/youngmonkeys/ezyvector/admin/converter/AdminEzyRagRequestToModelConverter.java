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
import org.youngmonkeys.ezyvector.admin.request.AdminChunkDataRequest;
import org.youngmonkeys.ezyvector.admin.request.AdminSaveMySqlConnectionPropertiesRequest;
import org.youngmonkeys.ezyvector.admin.request.AdminSaveQdrantConnectionPropertiesRequest;
import org.youngmonkeys.ezyvector.model.RagDataSourceModel;
import org.youngmonkeys.ezyvector.model.RagMySqlConnectionPropertiesModel;
import org.youngmonkeys.ezyvector.model.RagQdrantConnectionPropertiesModel;

@EzySingleton
public class AdminezyvectorRequestToModelConverter {

    public RagQdrantConnectionPropertiesModel toModel(
        AdminSaveQdrantConnectionPropertiesRequest request
    ) {
        return RagQdrantConnectionPropertiesModel
            .builder()
            .baseUrl(request.getBaseUrl())
            .apiKey(request.getApiKey())
            .collectionName(request.getCollectionName())
            .build();
    }

    public RagMySqlConnectionPropertiesModel toModel(
        AdminSaveMySqlConnectionPropertiesRequest request
    ) {
        return RagMySqlConnectionPropertiesModel
            .builder()
            .baseUrl(request.getBaseUrl())
            .accessToken(request.getAccessToken())
            .build();
    }

    public RagDataSourceModel toDataSourceModel(
        AdminChunkDataRequest request
    ) {
        return RagDataSourceModel
            .builder()
            .sourceType(request.getSourceType())
            .sourceId(request.getSourceId())
            .data(request.getData())
            .build();
    }
}
