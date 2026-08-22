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

import org.youngmonkeys.ezyvector.converter.EzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollection;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPaginationParameter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPaginationParameterConverter;
import org.youngmonkeys.ezyvector.pagination.IdDescEzyVectorCollectionPaginationParameter;
import org.youngmonkeys.ezyvector.repo.PaginationEzyVectorCollectionRepository;
import org.youngmonkeys.ezyplatform.service.CommonPaginationService;

public class PaginationEzyVectorCollectionService extends CommonPaginationService<
    EzyVectorCollectionModel,
    EzyVectorCollectionFilter,
    EzyVectorCollectionPaginationParameter,
    Long,
    EzyVectorCollection> {

    private final EzyVectorEntityToModelConverter entityToModelConverter;

    public PaginationEzyVectorCollectionService(
        PaginationEzyVectorCollectionRepository repository,
        EzyVectorEntityToModelConverter entityToModelConverter,
        EzyVectorCollectionPaginationParameterConverter paginationParameterConverter
    ) {
        super(repository, paginationParameterConverter);
        this.entityToModelConverter = entityToModelConverter;
    }


    @Override
    protected EzyVectorCollectionModel convertEntity(EzyVectorCollection entity) {
        return entityToModelConverter.toModel(entity);
    }

    @Override
    protected EzyVectorCollectionPaginationParameter defaultPaginationParameter() {
        return new IdDescEzyVectorCollectionPaginationParameter();
    }
}
