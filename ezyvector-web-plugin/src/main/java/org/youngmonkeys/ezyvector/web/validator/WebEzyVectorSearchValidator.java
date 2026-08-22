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

import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import org.youngmonkeys.ezyvector.web.request.WebEzyVectorSearchRequest;

import java.util.HashMap;
import java.util.Map;

import static org.youngmonkeys.ezyplatform.constant.CommonConstants.ZERO;

public class WebEzyVectorSearchValidator {

    public void validate(
        WebEzyVectorSearchRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        float[] vector = request.getVector();
        if (vector == null || vector.length == ZERO) {
            errors.put("vector", "required");
        }
        if (request.getLimit() <= ZERO) {
            errors.put("limit", "invalid");
        }
        if (!errors.isEmpty()) {
            throw new HttpBadRequestException(errors);
        }
    }
}
