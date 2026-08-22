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
import org.youngmonkeys.ezyvector.web.request.WebUpsertVectorPointsRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EzySingleton
public class WebEzyVectorPointsValidator {

    public void validate(
        WebUpsertVectorPointsRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        List<WebUpsertVectorPointsRequest.Point> points = request.getPoints();
        if (points == null || points.isEmpty()) {
            errors.put("points", "required");
        } else {
            for (int i = 0; i < points.size(); ++i) {
                List<Float> vector = points.get(i).getVector();
                if (vector == null || vector.isEmpty()) {
                    errors.put("points[" + i + "].vector", "required");
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new HttpBadRequestException(errors);
        }
    }
}
