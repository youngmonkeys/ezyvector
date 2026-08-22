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
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionSegment;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentPaginationParameter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentPaginationParameterConverter;
import org.youngmonkeys.ezyvector.pagination.IdDescEzyVectorCollectionSegmentPaginationParameter;
import org.youngmonkeys.ezyvector.repo.PaginationEzyVectorCollectionSegmentRepository;
import org.youngmonkeys.ezyplatform.service.CommonPaginationService;

public class PaginationEzyVectorCollectionSegmentService extends CommonPaginationService<
    EzyVectorCollectionSegmentModel,
    EzyVectorCollectionSegmentFilter,
    EzyVectorCollectionSegmentPaginationParameter,
    Long,
    EzyVectorCollectionSegment> {

    private final EzyVectorEntityToModelConverter entityToModelConverter;

    public PaginationEzyVectorCollectionSegmentService(
        PaginationEzyVectorCollectionSegmentRepository repository,
        EzyVectorEntityToModelConverter entityToModelConverter,
        EzyVectorCollectionSegmentPaginationParameterConverter paginationParameterConverter
    ) {
        super(repository, paginationParameterConverter);
        this.entityToModelConverter = entityToModelConverter;
    }


    @Override
    protected EzyVectorCollectionSegmentModel convertEntity(EzyVectorCollectionSegment entity) {
        return entityToModelConverter.toModel(entity);
    }

    @Override
    protected EzyVectorCollectionSegmentPaginationParameter defaultPaginationParameter() {
        return new IdDescEzyVectorCollectionSegmentPaginationParameter();
    }
}
