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
import org.youngmonkeys.ezyvector.admin.embbeding.AdminRagEmbeddingServiceManager;
import org.youngmonkeys.ezyvector.admin.service.AdminezyvectorSettingService;
import org.youngmonkeys.ezyvector.admin.validator.AdminRagEmbeddingServiceValidator;

import static com.tvd12.ezyfox.io.EzyStrings.EMPTY_STRING;
import static com.tvd12.ezyfox.io.EzyStrings.isNotBlank;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.DEFAULT_HIDDEN_PASSWORD;

@Controller
@Authenticated
@EzyFeature("rag")
@AllArgsConstructor
public class AdminEmbeddingServiceController {

    private final AdminRagEmbeddingServiceManager embeddingServiceManager;
    private final AdminRagEmbeddingServiceValidator embeddingServiceValidator;
    private final AdminezyvectorSettingService ezyvectorSettingService;

    @DoGet("/embedding-services")
    public View embeddingServicesGet() {
        return View.builder()
            .template("ezyvector/embedding-service/list")
            .addVariable(
                "embeddingServiceNames",
                embeddingServiceManager.getSortedEmbeddingServiceNames()
            )
            .addVariable(
                "defaultEmbeddingServiceName",
                ezyvectorSettingService.getEmbeddingService()
            )
            .build();
    }

    @DoGet("/embedding-services/{serviceName}")
    public View embeddingServiceDetailsGet(
        @PathVariable String serviceName
    ) {
        embeddingServiceValidator.validateServiceName(serviceName);
        return newViewBuilder()
            .template("ezyvector/embedding-service/details")
            .addVariable("embeddingServiceName", serviceName)
            .addVariable(
                "openAiApiKeyValue",
                isNotBlank(ezyvectorSettingService.getOpenAiApiKey())
                    ? DEFAULT_HIDDEN_PASSWORD
                    : EMPTY_STRING
            )
            .addVariable(
                "openAiEmbeddingModel",
                ezyvectorSettingService.getOpenAiEmbeddingModel()
            )
            .build();
    }

    private View.Builder newViewBuilder() {
        return View.builder()
            .addVariable("currentMenu", "ezyvector.embedding_services")
            .addVariable("currentParentTitle", "embedding_services")
            .addVariable("currentParentURL", "/ezyvector/embedding-services");
    }
}
