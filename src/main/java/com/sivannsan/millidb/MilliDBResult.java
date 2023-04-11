package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnegative;
import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.common.Require;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliNull;
import com.sivannsan.millidata.MilliValue;

public final class MilliDBResult {
    private final long id;
    private final boolean succeed;
    @Nonnull
    private final MilliData metadata;

    /**
     * @param id    0 for unknown id
     * @param succeed   true only if there is no obstacle with the operation
     */
    public MilliDBResult(@Nonnegative long id, boolean succeed, @Nonnull MilliData metadata) {
        this.id = Require.nonnegative(id);
        this.succeed = succeed;
        this.metadata = Require.nonnull(metadata);
    }

    public long getID() {
        return id;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSucceed() {
        return succeed;
    }

    @Nonnull
    public MilliData getMetadata() {
        return metadata;
    }

    @Nonnull
    public MilliMap toMilliMap() {
        MilliMap map = new MilliMap("id", new MilliValue(id));
        if (succeed) map.put("s", new MilliValue(true));
        if (metadata != MilliNull.INSTANCE) map.put("m", metadata);
        return map;
    }

    public static final class Parser {
        @Nonnull
        public static MilliDBResult parse(@Nonnull String result, @Nonnull MilliDBResult defaultValue) {
            try {
                return parse(result);
            } catch (MilliDBResultException e) {
                return Require.nonnull(defaultValue);
            }
        }

        @Nonnull
        public static MilliDBResult parse(@Nonnull String result) throws MilliDBResultException {
            MilliMap map = MilliData.Parser.parse(Require.nonnull(result), new MilliMap()).asMilliMap(new MilliMap());
            long parsedID = map.get("id").asMilliValue(new MilliValue(-1)).asInteger64();
            if (parsedID < 0) throw new MilliDBResultException("Invalid ID for parsing!");
            boolean parsedSucceed = map.get("s").asMilliValue(new MilliValue(false)).asBoolean();
            MilliData parsedMetadata = map.get("m");
            return new MilliDBResult(parsedID, parsedSucceed, parsedMetadata);
        }
    }
}
