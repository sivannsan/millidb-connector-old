package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.common.Validate;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliValue;

@SuppressWarnings("unused")
public abstract class MilliDBFilter {
    /**
     * <p>Used to check</p>
     * <p>1. If the searched files are documents</p>
     * <p>2. If the content of the searched documents are super-data of the provided subMilliData</p>
     * @param level deep level to check with superOf method, so that the next level will be checked with equals method; the value of 0 is used for checking only on the current MilliData; negative value is used for infinite level
     */
    public static SuperOf superOf(@Nonnull MilliData subMilliData, int level) {
        return new SuperOf(subMilliData, level);
    }

    /**
     * <p>Used to check</p>
     * <p>1. If the searched files are documents</p>
     * <p>2. If the content of the searched documents are super-data of the provided subMilliData</p>
     */
    public static SuperOf superOf(@Nonnull MilliData subMilliData) {
        return superOf(subMilliData, 0);
    }

    @Nonnull
    public abstract String getType();

    @Nonnull
    public abstract MilliMap toMilliMap();

    public static final class Parser {
        public static MilliDBFilter parse(@Nonnull String filter) {
            return parse(MilliData.Parser.parse(filter, new MilliMap()).asMilliMap(new MilliMap()));
        }

        public static MilliDBFilter parse(@Nonnull MilliMap filter) {
            String type = filter.get("_t").asMilliValue(new MilliValue()).asString();
            if (type.equals("")) return null;
            if (type.equals("so")) {
                return superOf(filter.get("s"), filter.get("l").asMilliValue(new MilliValue(0)).asInteger32());
            }
            return null;
        }
    }

    public static final class SuperOf extends MilliDBFilter {
        private final MilliData subMilliData;
        private final int level;

        private SuperOf(@Nonnull MilliData subMilliData, int level) {
            this.subMilliData = Validate.nonnull(subMilliData);
            this.level = level;
        }

        @Override
        @Nonnull
        public String getType() {
            return "so";
        }

        @Nonnull
        public MilliData getSubMilliData() {
            return subMilliData;
        }

        public int getLevel() {
            return level;
        }

        @Override
        @Nonnull
        public MilliMap toMilliMap() {
            return new MilliMap("_t", new MilliValue(getType())).append("s", subMilliData).append("l", new MilliValue(level));
        }
    }
}
