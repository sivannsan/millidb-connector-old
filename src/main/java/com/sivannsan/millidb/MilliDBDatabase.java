package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;

@SuppressWarnings("unused")
public interface MilliDBDatabase {
    void close();

    @Nonnull
    String getName();

    /**
     * @param path  delimited by slash; empty string will get the root (the same as getRoot method)
     * @return      MilliDBNone if the searched MilliDBFile does not exist or if the provided path is not empty yet the root is not a MilliDBCollection
     */
    @Nonnull
    MilliDBFile get(@Nonnull String path) throws MilliDBResultException;

    /**
     * @param path  the conventional name for MilliDBDocument ends with the .mll extension; empty string will create the root (the same as createRoot method)
     * @param type  MilliDBDocument.class or MilliDBCollection.class; MilliDBNone will be ignored the creation
     * @param force if false, it won't create if the provided name already exists; if true, it will delete the old and create a new if the type is different
     */
    void create(@Nonnull String path, @Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException;

    @Nonnull
    MilliDBFile getRoot() throws MilliDBResultException;

    /**
     * @param type  MilliDBDocument.class or MilliDBCollection.class; MilliDBNone will be ignored the creation
     * @param force if false, it won't create if the root already exists; if true, it will delete the old root and create a new root if the type is different
     */
    void createRoot(@Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException;
}
