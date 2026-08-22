package org.youngmonkeys.ezyvector.entity;

public enum EzyVectorCollectionPointStatus {
    LIVE,
    DELETED;

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
