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

package org.youngmonkeys.ezyvector.admin.controller.view;

import com.tvd12.ezyfox.annotation.EzyFeature;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.view.View;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.admin.controller.service.AdminEzyVectorCollectionControllerService;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionStatus;

import static com.tvd12.ezyfox.io.EzyLists.newArrayList;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.VIEW_VARIABLE_ADDITIONAL_MESSAGE_KEYS;

@Controller
@Authenticated
@EzyFeature("vector_database_management")
@AllArgsConstructor
public class AdminVectorCollectionController {

    private final AdminEzyVectorCollectionControllerService
        vectorCollectionControllerService;

    @DoGet("/vector-collections")
    public View vectorCollectionsGet() {
        return View.builder()
            .template("ezyvector/collection/list")
            .addVariable(
                "vectorCollectionStatuses",
                newArrayList(
                    EzyVectorCollectionStatus.values(),
                    Enum::toString
                )
            )
            .appendValuesToVariable(
                VIEW_VARIABLE_ADDITIONAL_MESSAGE_KEYS,
                newArrayList(
                    EzyVectorCollectionStatus.values(),
                    it -> it.toString().toLowerCase()
                )
            )
            .build();
    }

    @DoGet("/vector-collections/{id}")
    public View vectorCollectionsIdsGet(
        @PathVariable long collectionId
    ) {
        return View.builder()
            .template("ezyvector/collection/details")
            .addVariable(
                "vectorCollection",
                vectorCollectionControllerService
                    .getVectorCollectionById(collectionId)
            )
            .build();
    }
}
