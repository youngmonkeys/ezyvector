package org.youngmonkeys.ezyvector.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreateVectorCollectionResultModel {
    private long vectorSize;
}
