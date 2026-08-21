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

package org.youngmonkeys.ezyvector.admin.client;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyvector.admin.builder.AdminRagKnowledgeDataBuilderManager;
import org.youngmonkeys.ezyvector.admin.chunker.AdminRagDataChunkerManager;
import org.youngmonkeys.ezyvector.admin.cleaner.AdminRagTextCleanerManager;
import org.youngmonkeys.ezyvector.admin.embbeding.AdminRagEmbeddingServiceManager;
import org.youngmonkeys.ezyvector.admin.loader.AdminRagDataLoaderManager;
import org.youngmonkeys.ezyvector.admin.processor.AdminRagQueryProcessorManager;
import org.youngmonkeys.ezyvector.admin.retriever.AdminRagDataRetrieverManager;
import org.youngmonkeys.ezyvector.admin.service.AdminezyvectorSettingService;
import org.youngmonkeys.ezyvector.admin.service.AdminRagDataChunkMetaService;
import org.youngmonkeys.ezyvector.admin.service.AdminRagDataChunkService;
import org.youngmonkeys.ezyvector.admin.vd.AdminRagVectorDatabaseServiceManager;
import org.youngmonkeys.ezyvector.client.ezyvectorClient;

@EzySingleton
public class AdminezyvectorClient extends ezyvectorClient {

    public AdminezyvectorClient(
        AdminRagDataChunkerManager dataChunkerManager,
        AdminRagDataLoaderManager dataLoaderManager,
        AdminRagDataRetrieverManager dataRetrieverManager,
        AdminRagEmbeddingServiceManager embeddingServiceManager,
        AdminRagKnowledgeDataBuilderManager knowledgeDataBuilderManager,
        AdminRagQueryProcessorManager queryProcessorManager,
        AdminRagTextCleanerManager textCleanerManager,
        AdminRagVectorDatabaseServiceManager vectorDatabaseServiceManager,
        AdminRagDataChunkService dataChunkService,
        AdminRagDataChunkMetaService dataChunkMetaService,
        AdminezyvectorSettingService settingService
    ) {
        super(
            dataChunkerManager,
            dataLoaderManager,
            dataRetrieverManager,
            embeddingServiceManager,
            knowledgeDataBuilderManager,
            queryProcessorManager,
            textCleanerManager,
            vectorDatabaseServiceManager,
            dataChunkService,
            dataChunkMetaService,
            settingService
        );
    }
}
