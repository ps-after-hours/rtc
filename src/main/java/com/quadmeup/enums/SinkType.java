package com.quadmeup.enums;

public enum SinkType {
    JOINED("joined"),
    ORPHANED("orphaned");

    private String type;

    SinkType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
