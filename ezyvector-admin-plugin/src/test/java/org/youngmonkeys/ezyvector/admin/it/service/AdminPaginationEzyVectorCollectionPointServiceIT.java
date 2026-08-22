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
import org.youngmonkeys.ezyvector.admin.repo.AdminEzyVectorCollectionPointRepository;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionPointPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionPointService;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionPointModel;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionPointFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPointPaginationSortOrder;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyplatform.test.IntegrationTest;

import static org.youngmonkeys.ezyplatform.pagination.PaginationModelFetchers.getPaginationModel;

@EzySingleton
@AllArgsConstructor
public class AdminPaginationEzyVectorCollectionPointServiceIT
    extends EzyLoggable
    implements IntegrationTest {

    private final AdminEzyVectorCollectionPointRepository ezyVectorCollectionPointRepository;
    private final AdminPaginationEzyVectorCollectionPointService paginationEzyVectorCollectionPointService;
    private final AdminEzyVectorCollectionPointPaginationParameterConverter
        paginationParameterConverter;

    @Override
    public void test() {
        getEzyVectorCollectionPointOrderByIdAscTest();
        getEzyVectorCollectionPointOrderByIdDescTest();
    }

    public void getEzyVectorCollectionPointOrderByIdAscTest() {
        // given
        EzyVectorCollectionPointFilter filter = DefaultEzyVectorCollectionPointFilter
            .builder()
            .build();
        String actualNextPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionPointPaginationSortOrder
                    .ID_ASC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollectionPoint entity = new InstanceRandom().randomObject(EzyVectorCollectionPoint.class);
        ezyVectorCollectionPointRepository.save(entity);
        PaginationModel<EzyVectorCollectionPointModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionPointService,
            filter,
            actualNextPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionPointRepository.delete(entity.getId());
    }

    public void getEzyVectorCollectionPointOrderByIdDescTest() {
        // given
        EzyVectorCollectionPointFilter filter = DefaultEzyVectorCollectionPointFilter
            .builder()
            .build();
        String actualPrevPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionPointPaginationSortOrder
                    .ID_DESC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollectionPoint entity = new InstanceRandom().randomObject(EzyVectorCollectionPoint.class);
        ezyVectorCollectionPointRepository.save(entity);
        PaginationModel<EzyVectorCollectionPointModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionPointService,
            filter,
            actualPrevPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionPointRepository.delete(entity.getId());
    }
}
