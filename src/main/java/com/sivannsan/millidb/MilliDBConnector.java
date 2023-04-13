package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnegative;
import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.common.Validate;
import com.sivannsan.millidata.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class MilliDBConnector {
    private MilliDBConnector() {
    }

    /**
     * @return  null if the connection is fail
     */
    public static MilliDBDatabase connect(@Nonnull String host, int port, @Nonnull String database, @Nonnull String user, @Nonnull String password) {
        try {
            MilliDBLogger.info("Connecting to a MilliDBServer...");
            MilliDBLogger.info("- host: " + host);
            MilliDBLogger.info("- port: " + port);
            long time = System.currentTimeMillis();
            Socket socket = new Socket(host, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            socket.setSoTimeout(5000);
            IMilliDBDatabase db = new IMilliDBDatabase(socket, reader, writer, database, 5);
            MilliDBResult result =  db.execute(new MilliDBQuery(MilliDBQuery.Function.VERIFY, new MilliMap("user", new MilliValue(user)).append("password", new MilliValue(password)).append("database", new MilliValue(database))));
            if (!result.isSucceed()) {
                MilliDBLogger.warning("Couldn't verify with the MilliDBServer!");
                return null;
            }
            MilliMap map = result.getMetadata().asMilliMap(new MilliMap());
            if (!map.get("succeed").asMilliValue(new MilliValue(false)).asBoolean()) {
                MilliDBLogger.warning(map.get("reason").asMilliValue(new MilliValue()).asString());
                db.execute(new MilliDBQuery(MilliDBQuery.Function.CLOSE, MilliNull.INSTANCE));
                return null;
            }
            MilliDBLogger.info("The MilliDBServer has been successfully connected in " + (System.currentTimeMillis() - time) + "ms!");
            return db;
        } catch (UnknownHostException e) {
            MilliDBLogger.warning("UnknownHostException occurs while connecting to a database");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            MilliDBLogger.warning("IOException occurs while connecting to a database");
            e.printStackTrace();
            return null;
        }
    }

    private static final class IMilliDBDatabase implements MilliDBDatabase {
        @Nonnull
        private final Socket socket;
        @Nonnull
        private final BufferedReader reader;
        @Nonnull
        private final PrintWriter writer;
        @Nonnull
        private final String name;
        @Nonnegative
        private final int maxFailures;

        private IMilliDBDatabase(@Nonnull Socket socket, @Nonnull BufferedReader reader, @Nonnull PrintWriter writer, @Nonnull String name, @Nonnegative int maxFailures) {
            this.socket = Validate.nonnull(socket);
            this.reader = Validate.nonnull(reader);
            this.writer = Validate.nonnull(writer);
            this.name = Validate.nonnull(name);
            this.maxFailures = Validate.nonnegative(maxFailures);
        }

        @Override
        public void close() {
            execute(new MilliDBQuery(MilliDBQuery.Function.CLOSE, MilliNull.INSTANCE));
            try {
                writer.close();
                reader.close();
                socket.close();
            } catch (IOException e) {
                MilliDBLogger.warning("IOException occurs while closing a database");
                e.printStackTrace();
            }
        }

        @Override
        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public synchronized MilliDBResult execute(@Nonnull MilliDBQuery query) {
            query.executes += 1;
            if (query.executes > maxFailures) {
                return new MilliDBResult(query.getID(), false, MilliNull.INSTANCE);
            }
            if (query.executes > 1) MilliDBLogger.warning("Query '" + query.getID() + "' has failed to execute " + (query.executes - 1) + " time" + (query.executes > 2 ? "s" : ""));
            writer.println(query.asMilliMap());
            try {
                MilliDBResult result = MilliDBResult.Parser.parse(reader.readLine());
                if (result.getID() == query.getID()) return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return execute(query);
        }

        @Override
        @Nonnull
        public MilliDBFile get(@Nonnull String path) throws MilliDBResultException {
            MilliDBResult result = execute(new MilliDBQuery(MilliDBQuery.Function.GET, new MilliValue(name + (Validate.nonnull(path).equals("") ? "" : "/" + path))));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute GET from the root with a file path of '" + path + "'");
            String type = result.getMetadata().asMilliValue(new MilliValue()).asString();
            switch (type) {
                case "none":
                    return new IMilliDBNone(this, null, "");
                case "document":
                    if (path.equals("")) {
                        return new IMilliDBDocument(this, null, name);
                    } else {
                        IMilliDBCollection parent = new IMilliDBCollection(this, null, name);
                        String[] filenames = path.split("/");
                        for (int i = 0; i < filenames.length - 1; i++) {
                            parent = new IMilliDBCollection(this, parent, filenames[i]);
                        }
                        return new IMilliDBDocument(this, parent, filenames[filenames.length - 1]);
                    }
                case "collection":
                    IMilliDBCollection collection = new IMilliDBCollection(this, null, name);
                    if (!path.equals("")) {
                        for (String filename : path.split("/")) {
                            collection = new IMilliDBCollection(this, collection, filename);
                        }
                    }
                    return collection;
                default:
                    throw new MilliDBResultException("Invalid result from the execution of GET from the root with a file path of '" + path + "'");
            }
        }

        @Override
        public void create(@Nonnull String path, @Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException {
            String t;
            if (type == MilliDBDocument.class) t = "document";
            else if (type == MilliDBCollection.class) t = "collection";
            else return;
            MilliMap metadata = new MilliMap().append("path", new MilliValue(name + (path.equals("") ? "" : "/" + path))).append("type", new MilliValue(t));
            if (force) metadata.put("force", new MilliValue(true));
            MilliDBResult result = execute(new MilliDBQuery(MilliDBQuery.Function.CREATE, metadata));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute CREATE from the root with a file path of '" + path + "', a file type of '" + t + "', and a force of '" + force + "'");
        }

        @Override
        @Nonnull
        public MilliDBFile getRoot() throws MilliDBResultException {
            return get("");
        }

        @Override
        public void createRoot(@Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException {
            create("", type, force);
        }
    }

    private static abstract class IMilliDBFile implements MilliDBFile {
        @Nonnull
        protected final IMilliDBDatabase database;
        private final IMilliDBCollection parent;
        @Nonnull
        private final String name;

        private IMilliDBFile(@Nonnull IMilliDBDatabase database, IMilliDBCollection parent, @Nonnull String name) {
            this.database = Validate.nonnull(database);
            this.parent = parent;
            this.name = Validate.nonnull(name);
        }

        @Override
        public final MilliDBCollection getParent() {
            return parent;
        }

        @Override
        @Nonnull
        public final String getPath() {
            return parent == null ? name : parent.getPath() + "/" + name;
        }

        @Override
        @Nonnull
        public final String getName() {
            return name;
        }

        @Override
        public final boolean isMilliDBNone() {
            return this instanceof MilliDBNone;
        }

        @Override
        public final boolean isMilliDBDocument() {
            return this instanceof MilliDBDocument;
        }

        @Override
        public final boolean isMilliDBCollection() {
            return this instanceof MilliDBCollection;
        }

        @Override
        @Nonnull
        public final MilliDBDocument asMilliDBDocument() throws ClassCastException {
            if (isMilliDBDocument()) return (MilliDBDocument) this;
            throw new ClassCastException("Not a MilliDBDocument");
        }

        @Override
        @Nonnull
        public final MilliDBCollection asMilliDBCollection() throws ClassCastException {
            if (isMilliDBCollection()) return (MilliDBCollection) this;
            throw new ClassCastException("Not a MilliDBCollection");
        }

        @Override
        public final void delete() throws MilliDBResultException {
            if (isMilliDBNone()) return;
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.DELETE, new MilliValue(getPath())));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute DELETE from the file path of '" + getPath() + "'");
        }
    }

    private static final class IMilliDBNone extends IMilliDBFile implements MilliDBNone {
        private IMilliDBNone(@Nonnull IMilliDBDatabase database, IMilliDBCollection parent, @Nonnull String name) {
            super(database, parent, name);
        }
    }

    private static final class IMilliDBDocument extends IMilliDBFile implements MilliDBDocument {
        private IMilliDBDocument(@Nonnull IMilliDBDatabase database, IMilliDBCollection parent, @Nonnull String name) {
            super(database, parent, name);
        }

        @Override
        @Nonnull
        public MilliData fetch(@Nonnull String path) throws MilliDBResultException {
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.FETCH, new MilliMap().append("path", new MilliValue(getPath())).append("data_path", new MilliValue(path))));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute FETCH from the file path of '" + getPath() + "' with the data path of '" + path + "'");
            return result.getMetadata();
        }

        @Override
        public void set(@Nonnull String path, @Nonnull MilliData value) throws MilliDBResultException {
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.SET, new MilliMap().append("path", new MilliValue(getPath())).append("data_path", new MilliValue(path)).append("data_value", value)));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute SET from the file path of '" + getPath() + "' with the data path of '" + path + "'");
        }

        @Override
        @Nonnull
        public MilliData fetchContent() throws MilliDBResultException {
            return fetch("");
        }

        @Override
        public void setContent(@Nonnull MilliData value) throws MilliDBResultException {
            set("", value);
        }
    }

    private static final class IMilliDBCollection extends IMilliDBFile implements MilliDBCollection {
        private IMilliDBCollection(@Nonnull IMilliDBDatabase database, IMilliDBCollection parent, @Nonnull String name) {
            super(database, parent, name);
        }

        @Override
        @Nonnull
        public List<MilliDBFile> list() throws MilliDBResultException {
            return list(null);
        }

        @Override
        @Nonnull
        public List<MilliDBFile> list(MilliDBFilter filter) throws MilliDBResultException {
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.LIST, filter == null ? new MilliMap("path", new MilliValue(getPath())) : new MilliMap().append("path", new MilliValue(getPath())).append("filter", filter.toMilliMap())));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute LIST from the file path of '" + getPath() + "'" + (filter == null ? "" : " with a filter of '" + filter.toMilliMap().toString() + "'"));
            List<MilliDBFile> files = new ArrayList<>();
            for (MilliData document : result.getMetadata().asMilliMap(new MilliMap()).get("documents").asMilliList(new MilliList())) {
                if (document.isMilliValue()) files.add(new IMilliDBDocument(database, this, document.asMilliValue().asString()));
            }
            for (MilliData collection : result.getMetadata().asMilliMap(new MilliMap()).get("collections").asMilliList(new MilliList())) {
                if (collection.isMilliValue()) files.add(new IMilliDBCollection(database, this, collection.asMilliValue().asString()));
            }
            return files;
        }

        @Override
        @Nonnull
        public MilliDBFile get(@Nonnull String name) throws MilliDBResultException {
            if (Validate.nonnull(name).equals("")) return new IMilliDBNone(database, this, name);
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.GET, new MilliValue(getPath() + "/" + name)));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute GET from the file path of '" + getPath() + "' with a file name of '" + name + "'");
            String type = result.getMetadata().asMilliValue(new MilliValue()).asString();
            switch (type) {
                case "none":
                    return new IMilliDBNone(database, this, name);
                case "document":
                    return new IMilliDBDocument(database, this, name);
                case "collection":
                    return new IMilliDBCollection(database, this, name);
                default:
                    throw new MilliDBResultException("Invalid result from the execution of GET from the file path of '" + getPath() + "' with a file name of '" + name + "'");
            }
        }

        @Override
        public void create(@Nonnull String name, @Nonnull Class<? extends MilliDBFile> type, boolean force) throws MilliDBResultException {
            if (Validate.nonnull(name).equals("")) return;
            String t;
            if (Validate.nonnull(type) == MilliDBDocument.class) t = "document";
            else if (type == MilliDBCollection.class) t = "collection";
            else return;
            MilliMap metadata = new MilliMap().append("path", new MilliValue(getPath() + "/" + name)).append("type", new MilliValue(t));
            if (force) metadata.put("force", new MilliValue(true));
            MilliDBResult result = database.execute(new MilliDBQuery(MilliDBQuery.Function.CREATE, metadata));
            if (!result.isSucceed()) throw new MilliDBResultException("Failed to execute CREATE from the file path of '" + getPath() + "' with a file name of '" + name + "', a file type of '" + t + "', and a force of '" + force + "'");
        }
    }
}
