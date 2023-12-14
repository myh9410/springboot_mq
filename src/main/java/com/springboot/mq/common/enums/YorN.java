package com.springboot.mq.common.enums;

public enum YorN {
    Y(true),
    N(false);

    private boolean value;

    YorN(boolean value) {
        this.value = value;
    }
}
