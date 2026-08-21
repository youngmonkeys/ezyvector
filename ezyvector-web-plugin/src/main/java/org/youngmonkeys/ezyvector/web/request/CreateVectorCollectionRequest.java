package org.youngmonkeys.ezyvector.web.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVectorCollectionRequest {
    private long vectorSize;

    @Getter
    @Setter
    public static class Vectors {
        private long size;
        private String distance;
    }
}
