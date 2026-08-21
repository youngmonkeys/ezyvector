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
import org.youngmonkeys.ezyvector.admin.service.AdminezyvectorSettingService;
import org.youngmonkeys.ezyvector.admin.validator.AdminRagVectorDatabaseServiceValidator;
import org.youngmonkeys.ezyvector.admin.vd.AdminRagVectorDatabaseServiceManager;
import org.youngmonkeys.ezyvector.constant.RagVectorDatabaseServiceName;
import org.youngmonkeys.ezyvector.model.RagMySqlConnectionPropertiesModel;
import org.youngmonkeys.ezyvector.model.RagQdrantConnectionPropertiesModel;

import static com.tvd12.ezyfox.io.EzyStrings.EMPTY_STRING;
import static com.tvd12.ezyfox.io.EzyStrings.isNotBlank;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.DEFAULT_HIDDEN_PASSWORD;
import static org.youngmonkeys.ezyvector.admin.validator.AdminRagVectorDatabaseServiceValidator.MIN_VECTOR_SIZE;

@Controller
@Authenticated
@EzyFeature("rag")
@AllArgsConstructor
public class AdminVectorDatabaseServiceController {

    private final AdminRagVectorDatabaseServiceManager vectorDatabaseServiceManager;
    private final AdminRagVectorDatabaseServiceValidator vectorDatabaseServiceValidator;
    private final AdminezyvectorSettingService ezyvectorSettingService;

    @DoGet("/vector-database-services")
    public View vectorDatabaseServicesGet() {
        return View.builder()
            .template("ezyvector/vector-database-service/list")
            .addVariable(
                "vectorDatabaseServiceNames",
                vectorDatabaseServiceManager
                    .getSortedVectorDatabaseServiceNames()
            )
            .addVariable(
                "defaultVectorDatabaseServiceName",
                ezyvectorSettingService.getVectorDatabaseService()
            )
            .build();
    }

    @DoGet("/vector-database-services/{serviceName}")
    public View vectorDatabaseServiceDetailsGet(
        @PathVariable String serviceName
    ) {
        vectorDatabaseServiceValidator.validateServiceName(serviceName);
        if (RagVectorDatabaseServiceName.MYSQL.equalsValue(serviceName)) {
            RagMySqlConnectionPropertiesModel mySqlConnectionProperties =
                ezyvectorSettingService.getMySqlConnectionPropertiesInDb();
            return newViewBuilder()
                .template(
                    "ezyvector/vector-database-service/" +
                        serviceName.toLowerCase()
                )
                .addVariable("vectorDatabaseServiceName", serviceName)
                .addVariable(
                    "mySqlConnection",
                    mySqlConnectionProperties
                )
                .addVariable(
                    "mySqlAccessTokenValue",
                    isNotBlank(mySqlConnectionProperties.getAccessToken())
                        ? DEFAULT_HIDDEN_PASSWORD
                        : EMPTY_STRING
                )
                .addVariable(
                    "mySqlCollectionName",
                    ezyvectorSettingService.getMySqlCollectionName()
                )
                .addVariable(
                    "mySqlVectorSize",
                    ezyvectorSettingService.getMySqlVectorSize()
                )
                .addVariable("minVectorSize", MIN_VECTOR_SIZE)
                .build();
        }
        RagQdrantConnectionPropertiesModel qdrantConnectionProperties =
            ezyvectorSettingService.getConnectionPropertiesInDb();
        return newViewBuilder()
            .template(
                "ezyvector/vector-database-service/" +
                    serviceName.toLowerCase()
            )
            .addVariable("vectorDatabaseServiceName", serviceName)
            .addVariable(
                "qdrantConnection",
                qdrantConnectionProperties
            )
            .addVariable(
                "qdrantApiKeyValue",
                isNotBlank(qdrantConnectionProperties.getApiKey())
                    ? DEFAULT_HIDDEN_PASSWORD
                    : EMPTY_STRING
            )
            .addVariable(
                "qdrantVectorSize",
                ezyvectorSettingService.getQdrantVectorSize()
            )
            .addVariable("minVectorSize", MIN_VECTOR_SIZE)
            .build();
    }

    private View.Builder newViewBuilder() {
        return View.builder()
            .addVariable("currentMenu", "ezyvector.vector_database_services")
            .addVariable("currentParentTitle", "vector_database_services")
            .addVariable("currentParentURL", "/ezyvector/vector-database-services");
    }
}
