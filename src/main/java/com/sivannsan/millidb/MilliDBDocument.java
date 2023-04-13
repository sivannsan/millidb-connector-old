package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.millidata.MilliData;

@SuppressWarnings("unused")
public interface MilliDBDocument extends MilliDBFile {
    /**
     * @param path  delimited by dot; provided with number will be used for MilliList index first, then MilliMap key; empty string will fetch the content (the same as fetchContent method)
     */
    @Nonnull
    MilliData fetch(@Nonnull String path) throws MilliDBResultException;

    /**
     * @param path  delimited by dot; provided with number will be used for MilliList index to update first, but if it does not exist, then MilliMap key to put; empty string will set the content (the same as setContent method)
     */
    void set(@Nonnull String path, @Nonnull MilliData value) throws MilliDBResultException;

    @Nonnull
    MilliData fetchContent() throws MilliDBResultException;

    void setContent(@Nonnull MilliData value) throws MilliDBResultException;
}
