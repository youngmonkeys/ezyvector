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
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointPaginationParameter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointPaginationParameterConverter;
import org.youngmonkeys.ezyvector.pagination.IdDescEzyVectorCollectionPointPaginationParameter;
import org.youngmonkeys.ezyvector.repo.PaginationEzyVectorCollectionPointRepository;
import org.youngmonkeys.ezyplatform.service.CommonPaginationService;

public class PaginationEzyVectorCollectionPointService extends CommonPaginationService<
    EzyVectorCollectionPointModel,
    EzyVectorCollectionPointFilter,
    EzyVectorCollectionPointPaginationParameter,
    Long,
    EzyVectorCollectionPoint> {

    private final EzyVectorEntityToModelConverter entityToModelConverter;

    public PaginationEzyVectorCollectionPointService(
        PaginationEzyVectorCollectionPointRepository repository,
        EzyVectorEntityToModelConverter entityToModelConverter,
        EzyVectorCollectionPointPaginationParameterConverter paginationParameterConverter
    ) {
        super(repository, paginationParameterConverter);
        this.entityToModelConverter = entityToModelConverter;
    }


    @Override
    protected EzyVectorCollectionPointModel convertEntity(EzyVectorCollectionPoint entity) {
        return entityToModelConverter.toModel(entity);
    }

    @Override
    protected EzyVectorCollectionPointPaginationParameter defaultPaginationParameter() {
        return new IdDescEzyVectorCollectionPointPaginationParameter();
    }
}
