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
