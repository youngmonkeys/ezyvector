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
import org.youngmonkeys.ezyplatform.manager.EnvironmentManager;
import org.youngmonkeys.ezyplatform.util.ServletRequests;
import org.youngmonkeys.ezyvector.constant.EzyVectorDistance;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;
import org.youngmonkeys.ezyvector.web.service.WebEzyVectorSettingService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.tvd12.ezyfox.io.EzyStrings.isBlank;
import static java.util.Collections.singletonMap;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.HEADER_NAME_AUTHORIZATION;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.ZERO_LONG;
import static org.youngmonkeys.ezyplatform.util.AccessTokens.extractBearerToken;

@EzySingleton
@AllArgsConstructor
public class WebEzyVectorCollectionValidator {

    private static final String HEADER_NAME_API_KEY = "x-api-key";

    private final EnvironmentManager environmentManager;
    private final WebEzyVectorSettingService ezyVectorSettingService;

    public void validateAuthentication(
        RequestArguments arguments
    ) {
        validateClientIp(arguments);
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

    private void validateClientIp(
        RequestArguments arguments
    ) {
        Set<String> allowedIps = ezyVectorSettingService
            .getVectorCollectionsAllowedIps();
        if (allowedIps.isEmpty()) {
            return;
        }
        String clientIp = ServletRequests.getClientIp(
            arguments.getRequest(),
            environmentManager.isWithoutProxy()
        );
        if (!isAllowedIp(getFirstIp(clientIp), allowedIps)) {
            throw new HttpUnauthorizedException(
                singletonMap("clientIp", "notAllowed")
            );
        }
    }

    private String getFirstIp(String ip) {
        int commaIndex = ip != null ? ip.indexOf(',') : -1;
        return commaIndex >= 0
            ? ip.substring(0, commaIndex).trim()
            : ip;
    }

    private boolean isAllowedIp(
        String clientIp,
        Set<String> allowedIps
    ) {
        if (isBlank(clientIp)) {
            return false;
        }
        for (String ruleGroup : allowedIps) {
            if (isBlank(ruleGroup)) {
                continue;
            }
            String[] rules = ruleGroup.split("[,\\s]+");
            for (String rule : rules) {
                if (isBlank(rule)) {
                    continue;
                }
                rule = rule.trim();
                if (clientIp.equals(rule)
                    || isIpInCidr(clientIp, rule)
                    || isIpInRange(clientIp, rule)
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isIpInCidr(
        String clientIp,
        String rule
    ) {
        int slashIndex = rule.indexOf('/');
        if (slashIndex <= 0) {
            return false;
        }
        long ip = parseIpv4(clientIp);
        long network = parseIpv4(rule.substring(0, slashIndex));
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(
                rule.substring(slashIndex + 1)
            );
        } catch (Exception e) {
            return false;
        }
        if (ip < 0
            || network < 0
            || prefixLength < 0
            || prefixLength > 32
        ) {
            return false;
        }
        long mask = prefixLength == 0
            ? 0L
            : 0xFFFFFFFFL << (32 - prefixLength) & 0xFFFFFFFFL;
        return (ip & mask) == (network & mask);
    }

    private boolean isIpInRange(
        String clientIp,
        String rule
    ) {
        int hyphenIndex = rule.indexOf('-');
        if (hyphenIndex <= 0) {
            return false;
        }
        long ip = parseIpv4(clientIp);
        long start = parseIpv4(rule.substring(0, hyphenIndex));
        long end = parseIpv4(rule.substring(hyphenIndex + 1));
        return start >= 0
            && end >= 0
            && ip >= start
            && ip <= end;
    }

    private long parseIpv4(String ip) {
        if (isBlank(ip)) {
            return -1L;
        }
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            return -1L;
        }
        long answer = 0L;
        for (String part : parts) {
            int value;
            try {
                value = Integer.parseInt(part);
            } catch (Exception e) {
                return -1L;
            }
            if (value < 0 || value > 255) {
                return -1L;
            }
            answer = answer << 8 | value;
        }
        return answer;
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
