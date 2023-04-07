package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

import java.util.List;

public interface MilliDBCollection extends MilliDBFile {
    @Nonnull
    List<MilliDBFile> list() throws MilliDBResultException;

    @Nonnull
    List<MilliDBFile> list(MilliDBFilter filter) throws MilliDBResultException;

    /**
     * @param name  empty string will return a MilliDBNone
     */
    @Nonnull
    MilliDBFile get(@Nonnull String name) throws MilliDBResultException;

    /**
     * @param name  the conventional name for MilliDBDocument ends with the .mll extension; empty string will be ignored the creation
     * @param type  MilliDBDocument.class or MilliDBCollection.class; MilliDBNone will be ignored the creation
     * @param force if false, it won't create if the provided name already exists; if true, it will delete the old and create a new if the type is different
     */
    void create(@Nonnull String name, @Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException;
}
