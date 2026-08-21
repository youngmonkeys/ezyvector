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

package org.youngmonkeys.ezyvector.web.client;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyvector.client.ezyvectorClient;
import org.youngmonkeys.ezyvector.web.builder.WebRagKnowledgeDataBuilderManager;
import org.youngmonkeys.ezyvector.web.chunker.WebRagDataChunkerManager;
import org.youngmonkeys.ezyvector.web.cleaner.WebRagTextCleanerManager;
import org.youngmonkeys.ezyvector.web.embbeding.WebRagEmbeddingServiceManager;
import org.youngmonkeys.ezyvector.web.loader.WebRagDataLoaderManager;
import org.youngmonkeys.ezyvector.web.processor.WebRagQueryProcessorManager;
import org.youngmonkeys.ezyvector.web.retriever.WebRagDataRetrieverManager;
import org.youngmonkeys.ezyvector.web.service.WebezyvectorSettingService;
import org.youngmonkeys.ezyvector.web.service.WebRagDataChunkMetaService;
import org.youngmonkeys.ezyvector.web.service.WebRagDataChunkService;
import org.youngmonkeys.ezyvector.web.vd.WebRagVectorDatabaseServiceManager;

@EzySingleton
public class WebezyvectorClient extends ezyvectorClient {

    public WebezyvectorClient(
        WebRagDataChunkerManager dataChunkerManager,
        WebRagDataLoaderManager dataLoaderManager,
        WebRagDataRetrieverManager dataRetrieverManager,
        WebRagEmbeddingServiceManager embeddingServiceManager,
        WebRagKnowledgeDataBuilderManager knowledgeDataBuilderManager,
        WebRagQueryProcessorManager queryProcessorManager,
        WebRagTextCleanerManager textCleanerManager,
        WebRagVectorDatabaseServiceManager vectorDatabaseServiceManager,
        WebRagDataChunkService dataChunkService,
        WebRagDataChunkMetaService dataChunkMetaService,
        WebezyvectorSettingService settingService
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
