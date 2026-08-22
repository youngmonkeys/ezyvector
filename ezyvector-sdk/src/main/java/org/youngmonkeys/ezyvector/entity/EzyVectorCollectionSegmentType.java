package org.youngmonkeys.ezyvector.entity;

public enum EzyVectorCollectionSegmentType {
    MUTABLE,
    IMMUTABLE;

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
