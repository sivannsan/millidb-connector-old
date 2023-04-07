package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

public class MilliDBResultException extends RuntimeException {
    public MilliDBResultException() {
        super();
    }

    public MilliDBResultException(@Nonnull String message) {
        super(message);
    }
}
