package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

public class MilliDBQueryParsedException extends RuntimeException {
    public MilliDBQueryParsedException() {
        super();
    }

    public MilliDBQueryParsedException(@Nonnull String message) {
        super(message);
    }
}
