package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnegative;
import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.common.Validate;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliMap;
import com.sivannsan.millidata.MilliNull;
import com.sivannsan.millidata.MilliValue;

@SuppressWarnings("unused")
public final class MilliDBQuery {
    private final long id;
    @Nonnull
    private final Function function;
    @Nonnull
    private final MilliData metadata;
    public int executes = 0;

    public MilliDBQuery(@Nonnegative long id, @Nonnull Function function, @Nonnull MilliData metadata) {
        this.id = Validate.nonnegative(id);
        this.function = Validate.nonnull(function);
        this.metadata = Validate.nonnull(metadata);
    }

    public MilliDBQuery(@Nonnull Function function, @Nonnull MilliData metadata) {
        this(IDGenerator.generateNewID(), function, metadata);
    }

    public long getID() {
        return id;
    }

    @Nonnull
    public Function getFunction() {
        return function;
    }

    @Nonnull
    public MilliData getMetadata() {
        return metadata;
    }

    @Nonnull
    public MilliMap asMilliMap() {
        MilliMap map = new MilliMap().append("id", new MilliValue(id)).append("f", new MilliValue(function.toString()));
        if (metadata != MilliNull.INSTANCE) map.put("m", metadata);
        return map;
    }

    public static final class Parser {
        @Nonnull
        public static MilliDBQuery parse(@Nonnull String query, @Nonnull MilliDBQuery defaultValue) {
            try {
                return parse(Validate.nonnull(query));
            } catch (MilliDBQueryParsedException e) {
                return Validate.nonnull(defaultValue);
            }
        }

        @Nonnull
        public static MilliDBQuery parse(String query) throws MilliDBQueryParsedException {
            if (query == null) throw new MilliDBQueryParsedException("The provided argument is null!");
            MilliMap map = MilliData.Parser.parse(Validate.nonnull(query), new MilliMap()).asMilliMap(new MilliMap());
            long parsedID = map.get("id").asMilliValue(new MilliValue(-1)).asInteger64();
            if (parsedID < 0) throw new MilliDBQueryParsedException("The parsed ID is invalid!");
            Function parsedFunction = Function.fromString(map.get("f").asMilliValue(new MilliValue()).asString());
            if (parsedFunction == null) throw new MilliDBQueryParsedException("The parsed function is invalid!");
            MilliData parsedMetadata = map.get("m");
            return new MilliDBQuery(parsedID, parsedFunction, parsedMetadata);
        }
    }

    private static final class IDGenerator {
        private static long LAST_ID = 0;

        public static long generateNewID() {
            return ++LAST_ID;
        }
    }

    public enum Function {
        /**
         * <p> query metadata: user, password, database
         * <p> result metadata: succeed, reason
         */
        VERIFY,
        /**
         * <p> query metadata: path, filter
         * <p> result metadata: documents, collections
         */
        LIST,
        /**
         * <p> query metadata: path
         * <p> result metadata: type
         */
        GET,
        /**
         * <p> query metadata: path, type, force
         */
        CREATE,
        /**
         * <p> query metadata: path
         */
        DELETE,
        /**
         * <p> query metadata: path, data_path
         * <p> result metadata: data
         */
        FETCH,
        /**
         * <p> query metadata: path, data_path, data_value
         */
        SET,
        /**
         * <p> close the socket at the server
         */
        CLOSE;

        @Override
        public String toString() {
            switch (this) {
                case VERIFY: return "v";
                case LIST: return "l";
                case GET: return "g";
                case CREATE: return "c";
                case DELETE: return "d";
                case FETCH: return "f";
                case SET: return "s";
                case CLOSE: return "close";
                default: throw new IllegalStateException("Unexpected value: " + this);
            }
        }

        public static Function fromString(@Nonnull String string) {
            Validate.nonnull(string);
            for (Function f : values()) if (f.toString().equalsIgnoreCase(string)) return f;
            return null;
        }

        @Nonnull
        public static Function fromString(@Nonnull String string, @Nonnull Function defaultValue) {
            Function f = fromString(string);
            return f == null ? Validate.nonnull(defaultValue) : f;
        }
    }
}
