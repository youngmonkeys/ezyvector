package org.youngmonkeys.ezyvector.constant;

import com.tvd12.ezyfox.util.EzyEnums;

import java.util.Map;

public enum EzyVectorDistance {
    COSINE;

    private static final Map<String, EzyVectorDistance> MAP_BY_NAME =
        EzyEnums.enumMap(
            EzyVectorDistance.class,
            EzyVectorDistance::toString
        );

    public static EzyVectorDistance of(
        String value
    ) {
        return value == null
            ? null
            : MAP_BY_NAME.get(value.toUpperCase());
    }

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
