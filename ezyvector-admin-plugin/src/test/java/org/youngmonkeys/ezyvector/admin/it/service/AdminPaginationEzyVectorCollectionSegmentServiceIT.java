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

package org.youngmonkeys.ezyvector.admin.it.service;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.test.assertion.Asserts;
import com.tvd12.test.util.RandomUtil;
import lombok.AllArgsConstructor;
import org.youngmonkeys.devtools.InstanceRandom;
import org.youngmonkeys.ezyvector.admin.repo.AdminEzyVectorCollectionSegmentRepository;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionSegmentPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionSegmentService;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionSegment;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionSegmentModel;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionSegmentFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionSegmentPaginationSortOrder;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyplatform.test.IntegrationTest;

import static org.youngmonkeys.ezyplatform.pagination.PaginationModelFetchers.getPaginationModel;

@EzySingleton
@AllArgsConstructor
public class AdminPaginationEzyVectorCollectionSegmentServiceIT
    extends EzyLoggable
    implements IntegrationTest {

    private final AdminEzyVectorCollectionSegmentRepository ezyVectorCollectionSegmentRepository;
    private final AdminPaginationEzyVectorCollectionSegmentService paginationEzyVectorCollectionSegmentService;
    private final AdminEzyVectorCollectionSegmentPaginationParameterConverter
        paginationParameterConverter;

    @Override
    public void test() {
        getEzyVectorCollectionSegmentOrderByIdAscTest();
        getEzyVectorCollectionSegmentOrderByIdDescTest();
    }

    public void getEzyVectorCollectionSegmentOrderByIdAscTest() {
        // given
        EzyVectorCollectionSegmentFilter filter = DefaultEzyVectorCollectionSegmentFilter
            .builder()
            .build();
        String actualNextPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionSegmentPaginationSortOrder
                    .ID_ASC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollectionSegment entity = new InstanceRandom().randomObject(EzyVectorCollectionSegment.class);
        ezyVectorCollectionSegmentRepository.save(entity);
        PaginationModel<EzyVectorCollectionSegmentModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionSegmentService,
            filter,
            actualNextPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionSegmentRepository.delete(entity.getId());
    }

    public void getEzyVectorCollectionSegmentOrderByIdDescTest() {
        // given
        EzyVectorCollectionSegmentFilter filter = DefaultEzyVectorCollectionSegmentFilter
            .builder()
            .build();
        String actualPrevPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionSegmentPaginationSortOrder
                    .ID_DESC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollectionSegment entity = new InstanceRandom().randomObject(EzyVectorCollectionSegment.class);
        ezyVectorCollectionSegmentRepository.save(entity);
        PaginationModel<EzyVectorCollectionSegmentModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionSegmentService,
            filter,
            actualPrevPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionSegmentRepository.delete(entity.getId());
    }
}
