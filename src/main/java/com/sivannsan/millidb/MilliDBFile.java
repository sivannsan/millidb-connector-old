package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

@SuppressWarnings("unused")
public interface MilliDBFile {
    /**
     * @return  null if this MilliDBFile is root or unknown
     */
    MilliDBCollection getParent();

    /**
     * @return  path from the root MilliDBFile, including the root name; empty string if this MilliDBFile is unknown
     */
    @Nonnull
    String getPath();

    /**
     * @return  empty string if this MilliDBFile is unknown
     */
    @Nonnull
    String getName();

    boolean isMilliDBNone();

    boolean isMilliDBDocument();

    boolean isMilliDBCollection();

    @Nonnull
    MilliDBDocument asMilliDBDocument() throws ClassCastException;

    @Nonnull
    MilliDBCollection asMilliDBCollection() throws ClassCastException;

    /**
     * Deletion is not available (ignored) for MilliDBNone
     */
    void delete() throws MilliDBResultException;
}
