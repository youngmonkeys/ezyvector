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

package org.youngmonkeys.ezyvector.web.validator;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.exception.HttpUnauthorizedException;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyvector.constant.EzyVectorDistance;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorSettingService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.tvd12.ezyfox.io.EzyStrings.isBlank;
import static java.util.Collections.singletonMap;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.HEADER_NAME_AUTHORIZATION;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.ZERO_LONG;
import static org.youngmonkeys.ezyplatform.util.AccessTokens.extractBearerToken;

@EzySingleton
@AllArgsConstructor
public class WebEzyVectorCollectionValidator {

    private static final String HEADER_NAME_API_KEY = "x-api-key";

    private final WebEzyVectorSettingService ezyVectorSettingService;

    public void validateAuthentication(
        RequestArguments arguments
    ) {
        String apiKey = arguments.getRequestValueAnyway(
            "api-key",
            "api_key",
            "apikey"
        );
        if (isBlank(apiKey)) {
            apiKey = extractBearerToken(
                arguments.getHeader(HEADER_NAME_AUTHORIZATION)
            );
        }
        if (isBlank(apiKey)) {
            apiKey = arguments.getHeader(HEADER_NAME_API_KEY);
        }
        String apiKeyInDb = ezyVectorSettingService
            .getVectorCollectionsApiKey();
        if (!Objects.equals(apiKey, apiKeyInDb)) {
            throw new HttpUnauthorizedException(
                singletonMap("apiKey", "invalid")
            );
        }
    }

    public void validate(
        WebCreateVectorCollectionRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        WebCreateVectorCollectionRequest.Vectors vectors =
            request.getVectors();
        if (vectors == null) {
            errors.put("vectors", "required");
        } else {
            long size = vectors.getSize();
            if (size == ZERO_LONG) {
                errors.put("vectors.size", "required");
            } else if (size < ZERO_LONG) {
                errors.put("vectors.size", "invalid");
            }
            String distance = vectors.getDistance();
            if (isBlank(distance)) {
                errors.put("vectors.distance", "required");
            } else if (EzyVectorDistance.of(distance) == null) {
                errors.put("vectors.distance", "required");
            }
            if (!errors.isEmpty()) {
                throw new HttpBadRequestException(errors);
            }
        }
    }
}
