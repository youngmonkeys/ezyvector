package org.youngmonkeys.ezyvector.entity;

public enum EzyVectorCollectionSegmentStatus {
    BUILDING,
    ACTIVE,
    COMPACTING,
    OBSOLETE,
    CORRUPTED;

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
