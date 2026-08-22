package org.youngmonkeys.ezyvector.entity;

public enum EzyVectorCollectionStatus {
    ACTIVATED,
    INACTIVATED;

    public boolean equalsValue(String value) {
        return toString().equals(value);
    }
}
