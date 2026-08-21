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

package org.youngmonkeys.ezyvector.service;

import org.youngmonkeys.ezyvector.converter.ezyvectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.entity.RagDataChunk;
import org.youngmonkeys.ezyvector.model.RagDataChunkModel;
import org.youngmonkeys.ezyvector.pagination.RagDataChunkFilter;
import org.youngmonkeys.ezyvector.pagination.RagDataChunkPaginationParameter;
import org.youngmonkeys.ezyvector.pagination.RagDataChunkPaginationParameterConverter;
import org.youngmonkeys.ezyvector.pagination.IdDescRagDataChunkPaginationParameter;
import org.youngmonkeys.ezyvector.repo.PaginationRagDataChunkRepository;
import org.youngmonkeys.ezyplatform.service.CommonPaginationService;

public class PaginationRagDataChunkService extends CommonPaginationService<
    RagDataChunkModel,
    RagDataChunkFilter,
    RagDataChunkPaginationParameter,
    Long,
    RagDataChunk> {

    private final ezyvectorEntityToModelConverter entityToModelConverter;

    public PaginationRagDataChunkService(
        PaginationRagDataChunkRepository repository,
        ezyvectorEntityToModelConverter entityToModelConverter,
        RagDataChunkPaginationParameterConverter paginationParameterConverter
    ) {
        super(repository, paginationParameterConverter);
        this.entityToModelConverter = entityToModelConverter;
    }


    @Override
    protected RagDataChunkModel convertEntity(RagDataChunk entity) {
        return entityToModelConverter.toModel(entity);
    }

    @Override
    protected RagDataChunkPaginationParameter defaultPaginationParameter() {
        return new IdDescRagDataChunkPaginationParameter();
    }
}
