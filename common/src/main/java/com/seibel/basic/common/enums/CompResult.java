package com.seibel.basic.common.enums;

public enum CompResult {

    LESSER(-1),
    EQUAL(0),
    GREATER(1);

    public final int value;

    CompResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


}
