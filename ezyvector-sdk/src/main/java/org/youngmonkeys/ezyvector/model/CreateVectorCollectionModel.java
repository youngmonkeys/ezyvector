package org.youngmonkeys.ezyvector.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateVectorCollectionModel {
    private long vectorSize;

    @Getter
    @Builder
    public static class Vectors {
        private long size;
        private String distance;
    }
}
