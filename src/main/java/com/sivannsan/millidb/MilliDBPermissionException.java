package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

public class MilliDBPermissionException extends RuntimeException {
    public MilliDBPermissionException() {
        super();
    }

    public MilliDBPermissionException(@Nonnull String message) {
        super(message);
    }
}
