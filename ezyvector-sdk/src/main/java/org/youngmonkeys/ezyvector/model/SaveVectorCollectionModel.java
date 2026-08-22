package org.youngmonkeys.ezyvector.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveVectorCollectionModel {
    private Vectors vectors;

    @Getter
    @Builder
    public static class Vectors {
        private long size;
        private String distance;
    }
}
