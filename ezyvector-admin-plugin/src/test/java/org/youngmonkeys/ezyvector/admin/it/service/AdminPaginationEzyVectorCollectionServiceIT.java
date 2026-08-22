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
import org.youngmonkeys.ezyvector.admin.repo.AdminEzyVectorCollectionRepository;
import org.youngmonkeys.ezyvector.admin.pagination.AdminEzyVectorCollectionPaginationParameterConverter;
import org.youngmonkeys.ezyvector.admin.service.AdminPaginationEzyVectorCollectionService;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollection;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.pagination.DefaultEzyVectorCollectionFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionFilter;
import org.youngmonkeys.ezyvector.pagination.EzyVectorCollectionPaginationSortOrder;
import org.youngmonkeys.ezyplatform.model.PaginationModel;
import org.youngmonkeys.ezyplatform.test.IntegrationTest;

import static org.youngmonkeys.ezyplatform.pagination.PaginationModelFetchers.getPaginationModel;

@EzySingleton
@AllArgsConstructor
public class AdminPaginationEzyVectorCollectionServiceIT
    extends EzyLoggable
    implements IntegrationTest {

    private final AdminEzyVectorCollectionRepository ezyVectorCollectionRepository;
    private final AdminPaginationEzyVectorCollectionService paginationEzyVectorCollectionService;
    private final AdminEzyVectorCollectionPaginationParameterConverter
        paginationParameterConverter;

    @Override
    public void test() {
        getEzyVectorCollectionOrderByIdAscTest();
        getEzyVectorCollectionOrderByIdDescTest();
    }

    public void getEzyVectorCollectionOrderByIdAscTest() {
        // given
        EzyVectorCollectionFilter filter = DefaultEzyVectorCollectionFilter
            .builder()
            .build();
        String actualNextPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionPaginationSortOrder
                    .ID_ASC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollection entity = new InstanceRandom().randomObject(EzyVectorCollection.class);
        ezyVectorCollectionRepository.save(entity);
        PaginationModel<EzyVectorCollectionModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionService,
            filter,
            actualNextPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionRepository.delete(entity.getId());
    }

    public void getEzyVectorCollectionOrderByIdDescTest() {
        // given
        EzyVectorCollectionFilter filter = DefaultEzyVectorCollectionFilter
            .builder()
            .build();
        String actualPrevPageToken = paginationParameterConverter
            .getDefaultPageToken(
                EzyVectorCollectionPaginationSortOrder
                    .ID_DESC
                    .toString()
            );
        int limit = RandomUtil.randomSmallInt() + 1;

        // when
        EzyVectorCollection entity = new InstanceRandom().randomObject(EzyVectorCollection.class);
        ezyVectorCollectionRepository.save(entity);
        PaginationModel<EzyVectorCollectionModel> pagination = getPaginationModel(
            paginationEzyVectorCollectionService,
            filter,
            actualPrevPageToken,
            null,
            false,
            limit
        );

        // then
        Asserts.assertTrue(pagination.getCount() > 0);
        ezyVectorCollectionRepository.delete(entity.getId());
    }
}
