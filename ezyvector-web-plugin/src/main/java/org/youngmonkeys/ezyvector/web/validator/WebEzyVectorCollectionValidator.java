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
import org.youngmonkeys.ezyvector.constant.EzyVectorDistance;
import org.youngmonkeys.ezyvector.web.request.WebCreateVectorCollectionRequest;

import java.util.HashMap;
import java.util.Map;

import static com.tvd12.ezyfox.io.EzyStrings.isBlank;
import static org.youngmonkeys.ezyplatform.constant.CommonConstants.ZERO_LONG;

public class WebEzyVectorCollectionValidator {

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
