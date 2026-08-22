package org.youngmonkeys.ezyvector.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebEzyVectorSearchRequest {
    private float[] vector;
    private int limit;
    @JsonProperty(value = "with_payload")
    private boolean withPayload;
}
