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

package org.youngmonkeys.ezyvector.admin.service;

import com.tvd12.ezyhttp.server.core.annotation.Service;
import org.youngmonkeys.ezyvector.admin.converter.AdminEzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionSegmentPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.repo.AdminPaginationEzyVectorCollectionSegmentRepository;
import org.youngmonkeys.ezyvector.service.PaginationEzyVectorCollectionSegmentService;

@Service
public class AdminPaginationEzyVectorCollectionSegmentService
    extends PaginationEzyVectorCollectionSegmentService {

    public AdminPaginationEzyVectorCollectionSegmentService(
        AdminPaginationEzyVectorCollectionSegmentRepository repository,
        AdminEzyVectorEntityToModelConverter entityToModelConverter,
        AdminEzyVectorCollectionSegmentPaginationParameterConverter paginationParameterConverter
    ) {
        super(
            repository,
            entityToModelConverter,
            paginationParameterConverter
        );
    }
}
