package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ActiveEnum {
    ACTIVE(1),
    INACTIVE(0);

    public final int value;

    ActiveEnum(int value) {
        this.value = value;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
}
