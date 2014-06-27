/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.node.NodeBuilder;
import sirius.kernel.async.Async;
import sirius.kernel.async.CallContext;
import sirius.kernel.async.Promise;
import sirius.kernel.cache.Cache;
import sirius.kernel.cache.CacheManager;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.*;
import sirius.web.health.MetricProvider;
import sirius.web.health.MetricState;
import sirius.web.health.MetricsCollector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Central access class to the persistence layer.
 * <p>
 * Provides CRUD access to the underlying ElasticSearch cluster.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class Index {

    /**
     * Contains the default ID used by new entities
     */
    public static final String NEW = "new";

    /**
     * Contains the name of the ID field
     */
    public static final String ID_FIELD = "_id";

    /**
     * Async executor category for integrity check tasks
     */
    public static final String ASYNC_CATEGORY_INDEX_INTEGRITY = "index-ref-integrity";

    /**
     * Contains the database schema as expected by the java model
     */
    protected static Schema schema;

    /**
     * Logger used by this framework
     */
    public static Log LOG = Log.get("index");

    /**
     * Contains the ElasticSearch client
     */
    private static Client client;

    /**
     * To support multiple installations in parallel, an indexPrefix can be supplied, which is added to each index
     */
    private static String indexPrefix;

    /**
     * Determines if the framework is already completely initialized
     */
    private static volatile boolean ready;

    /**
     * Contains a promise which can be waited for
     */
    private static Promise<Boolean> readyPromise = new Promise<>();

    /**
     * Internal timer which is used to delay some actions. This is necessary, as ES takes up to one second to make a
     * write visible to the next read
     */
    private static Timer delayLineTimer;

    /**
     * Can be used to cache frequently used entities.
     */
    private static Cache<String, Object> globalCache = CacheManager.createCache("entity-cache");

    /*
     * Average query duration for statistical measures
     */
    private static Average queryDuration = new Average();
    /*
     * Counts how many threads used blockThreadForUpdate
     */
    private static Counter blocks = new Counter();
    /*
     * Counts how many delays where used
     */
    private static Counter delays = new Counter();
    /*
     * Counts how many optimistic lock errors occurred
     */
    private static Counter optimisticLockErrors = new Counter();

    /**
     * Fetches the entity of given type with the given id.
     * <p>
     * May use a given cache to load the entity.
     * </p>
     *
     * @param type  the type of the desired entity
     * @param id    the id of the desired entity
     * @param cache the cache to resolve the entity.
     * @return a tuple containing the resolved entity (or <tt>null</tt> if not found) and a flag which indicates if the
     * value was loaded from cache (<tt>true</tt>) or not.
     */
    @Nonnull
    public static <E extends Entity> Tuple<E, Boolean> fetch(@Nonnull Class<E> type,
                                                             @Nullable String id,
                                                             @Nonnull com.google.common.cache.Cache<String, Object> cache) {
        if (Strings.isEmpty(id)) {
            return Tuple.create(null, false);
        }

        @SuppressWarnings("unchecked") E value = (E) cache.getIfPresent(getDescriptor(type).getType() + "-" + id);
        if (value != null) {
            return Tuple.create(value, true);
        }
        value = find(type, id);
        cache.put(Index.getDescriptor(type).getType() + "-" + id, value);
        return Tuple.create(value, false);
    }

    /**
     * Fetches the entity of given type with the given id.
     * <p>
     * May use a given cache to load the entity.
     * </p>
     *
     * @param type  the type of the desired entity
     * @param id    the id of the desired entity
     * @param cache the cache to resolve the entity.
     * @return the resolved entity (or <tt>null</tt> if not found)
     */
    @Nullable
    public static <E extends Entity> E fetchFromCache(@Nonnull Class<E> type,
                                                      @Nullable String id,
                                                      @Nonnull com.google.common.cache.Cache<String, Object> cache) {
        return fetch(type, id, cache).getFirst();
    }

    /**
     * Fetches the entity of given type with the given id.
     * <p>
     * May use a global cache to load the entity.
     * </p>
     *
     * @param type the type of the desired entity
     * @param id   the id of the desired entity
     * @return a tuple containing the resolved entity (or <tt>null</tt> if not found) and a flag which indicates if the
     * value was loaded from cache (<tt>true</tt>) or not.
     */
    @Nonnull
    public static <E extends Entity> Tuple<E, Boolean> fetch(@Nonnull Class<E> type, @Nullable String id) {
        if (Strings.isEmpty(id)) {
            return Tuple.create(null, false);
        }

        @SuppressWarnings("unchecked") E value = (E) globalCache.get(getDescriptor(type).getType() + "-" + id);
        if (value != null) {
            return Tuple.create(value, true);
        }
        value = find(type, id);
        globalCache.put(Index.getDescriptor(type).getType() + "-" + id, value);
        return Tuple.create(value, false);
    }

    /**
     * Fetches the entity of given type with the given id.
     * <p>
     * May use a global cache to load the entity.
     * </p>
     *
     * @param type the type of the desired entity
     * @param id   the id of the desired entity
     * @return the resolved entity (or <tt>null</tt> if not found)
     */
    @Nullable
    public static <E extends Entity> E fetchFromCache(@Nonnull Class<E> type, @Nullable String id) {
        return fetch(type, id).getFirst();
    }

    /**
     * Provides access to the expected schema / mappings.
     *
     * @return the expected schema of the object model
     */
    public static Schema getSchema() {
        return schema;
    }

    /**
     * Used to delay an action for at least one second
     */
    private static class Wait {
        private long waitline;
        private Runnable cmd;
        private CallContext context;

        public Wait(Runnable cmd) {
            this.cmd = cmd;
            this.waitline = System.currentTimeMillis() + 1000;
            this.context = CallContext.getCurrent();
        }

        /**
         * Determines if the action was delayed long enough
         *
         * @return <tt>true</tt> if the action was delayed long enough, <tt>false</tt> otherwise
         */
        public boolean isRunnable() {
            return System.currentTimeMillis() > waitline;
        }

        /**
         * Executes the action in its own executor
         */
        public void execute() {
            CallContext.setCurrent(context);
            Async.executor("index-delay").fork(cmd).execute();
        }
    }

    /**
     * Queue of actions which need to be delayed one second
     */
    private static List<Wait> oneSecondDelayLine = Lists.newArrayList();

    /**
     * Adds an action to the delay line, which ensures that it is at least delayed for one second
     *
     * @param cmd to command to be delayed
     */
    public static void callAfterUpdate(final Runnable cmd) {
        synchronized (oneSecondDelayLine) {
            if (oneSecondDelayLine.size() < 100) {
                delays.inc();
                oneSecondDelayLine.add(new Wait(cmd));
                return;
            }
        }
        blockThreadForUpdate();
        cmd.run();
    }

    @Register
    public static class IndexReport implements MetricProvider {

        @Override
        public void gather(MetricsCollector collector) {
            if (!isReady()) {
                return;
            }
            ClusterHealthResponse res = getClient().admin().cluster().prepareHealth().execute().actionGet();
            collector.metric("ES-Nodes", res.getNumberOfNodes(), null, asMetricState(res.getStatus()));
            collector.metric("ES-InitializingShards",
                             res.getInitializingShards(),
                             null,
                             res.getInitializingShards() > 0 ? MetricState.YELLOW : MetricState.GRAY);
            collector.metric("ES-RelocatingShards",
                             res.getRelocatingShards(),
                             null,
                             res.getRelocatingShards() > 0 ? MetricState.YELLOW : MetricState.GRAY);
            collector.metric("ES-UnassignedShards",
                             res.getUnassignedShards(),
                             null,
                             res.getUnassignedShards() > 0 ? MetricState.RED : MetricState.GRAY);
            collector.metric("index-delay-line", "ES-DelayLine", oneSecondDelayLine.size(), null);
            collector.differentialMetric("index-blocks", "index-blocks", "ES-DelayBlocks", blocks.getCount(), "/min");
            collector.differentialMetric("index-delays", "index-delays", "ES-Delays", delays.getCount(), "/min");
            collector.differentialMetric("index-locking-errors",
                                         "index-locking-errors",
                                         "ES-OptimisticLock-Errors",
                                         optimisticLockErrors.getCount(),
                                         "/min");
            collector.metric("index-queryDuration", "ES-QueryDuration", queryDuration.getAvg(), "ms");
            collector.differentialMetric("index-queries",
                                         "index-queries",
                                         "ES-Queries",
                                         queryDuration.getCount(),
                                         "/min");
        }

        private MetricState asMetricState(ClusterHealthStatus status) {
            if (status == ClusterHealthStatus.GREEN) {
                return MetricState.GRAY;
            } else if (status == ClusterHealthStatus.YELLOW) {
                return MetricState.YELLOW;
            } else {
                return MetricState.RED;
            }
        }
    }

    /**
     * Implementation of the timer which handles delayed actions.
     */
    protected static class DelayLineHandler extends TimerTask {

        @Override
        public void run() {
            try {
                synchronized (oneSecondDelayLine) {
                    Iterator<Wait> iter = oneSecondDelayLine.iterator();
                    while (iter.hasNext()) {
                        Wait next = iter.next();
                        if (next.isRunnable()) {
                            next.execute();
                            iter.remove();
                        } else {
                            return;
                        }
                    }
                }
            } catch (Throwable e) {
                Exceptions.handle(LOG, e);
            }
        }
    }

    /**
     * Manually blocks the current thread for one second, to make a write visible in ES.
     * <p>
     * Consider using {@link #callAfterUpdate(Runnable)} which does not block system resources. Only use this method
     * is absolutely necessary.
     * </p>
     */
    public static void blockThreadForUpdate() {
        try {
            blocks.inc();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Exceptions.ignore(e);
        }
    }

    /**
     * Determines if the framework is completely initialized.
     *
     * @return <tt>true</tt> if the framework is completely initialized, <tt>false</tt> otherwise
     */
    public static boolean isReady() {
        return ready;
    }

    public static Promise<Boolean> ready() {
        return readyPromise;
    }

    @Register(classes = Lifecycle.class)
    public static class IndexLifecycle implements Lifecycle {

        @Part
        private static Config config;

        @Override
        public void started() {
            if (Strings.isEmpty(config.getString("index.type"))) {
                LOG.INFO("ElasticSearch is disabled! (index.type is not set)");
                return;
            }

            if ("embedded".equalsIgnoreCase(config.getString("index.type"))) {
                client = NodeBuilder.nodeBuilder().client(true).data(true).local(true).build().client();
            } else {
                Settings settings = ImmutableSettings.settingsBuilder()
                                                     .put("cluster.name", config.getString("index.cluster"))
                                                     .build();
                client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(config.getString(
                        "index.host"), config.getInt("index.port")));
            }

            // Setup index
            indexPrefix = config.getString("index.prefix");
            if (!indexPrefix.endsWith("-")) {
                indexPrefix = indexPrefix + "-";
            }

            schema = new Schema();
            schema.load();

            // Send mappings to ES
            if (config.getBoolean("index.updateSchema")) {
                for (String msg : schema.createMappings()) {
                    LOG.INFO(msg);
                }
            }
            ready = true;
            readyPromise.success(true);

            delayLineTimer = new Timer("index-delay");
            delayLineTimer.schedule(new DelayLineHandler(), 1000, 1000);
        }

        @Override
        public void stopped() {
            if (delayLineTimer != null) {
                delayLineTimer.cancel();
            }
        }

        @Override
        public void awaitTermination() {
            // We wait until this last call before we cut the connection to the database (elasticsearch) to permit
            // other stopping lifecycles access until the very end...
            ready = false;
            client.close();
        }

        @Override
        public String getName() {
            return "index (ElasticSearch)";
        }
    }

    /**
     * Handles the given unit of work while restarting it if an optimistic lock error occurs.
     *
     * @param uow the unit of work to handle.
     * @throws HandledException if either any other exception occurs, or if all three attempts fail with an optimistic lock error.
     */
    public static void retry(UnitOfWork uow) {
        int retries = 3;
        while (retries-- > 0) {
            try {
                uow.execute();
                return;
            } catch (OptimisticLockException e) {
                if (retries <= 0) {
                    throw Exceptions.handle()
                                    .withSystemErrorMessage(
                                            "Failed to update an entity after re-trying a unit of work several times: %s (%s)")
                                    .error(e)
                                    .to(LOG)
                                    .handle();
                }
                continue;
            } catch (HandledException e) {
                throw e;
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .withSystemErrorMessage(
                                        "An unexpected exception occurred while executing a unit of work: %s (%s)")
                                .error(e)
                                .to(LOG)
                                .handle();
            }
        }
    }

    /**
     * Ensures that the given manually created index exists.
     *
     * @param name the name of the index. The index prefix of the current system will be added automatically
     */
    public static void ensureIndexExists(String name) {
        try {
            IndicesExistsResponse res = Index.getClient()
                                             .admin()
                                             .indices()
                                             .prepareExists(getIndexName(name))
                                             .execute()
                                             .get(10, TimeUnit.SECONDS);
            if (!res.isExists()) {
                CreateIndexResponse createResponse = Index.getClient()
                                                          .admin()
                                                          .indices()
                                                          .prepareCreate(getIndexName(name))
                                                          .execute()
                                                          .get(10, TimeUnit.SECONDS);
                if (!createResponse.isAcknowledged()) {
                    throw Exceptions.handle()
                                    .to(LOG)
                                    .withSystemErrorMessage("Cannot create index: %s", getIndexName(name))
                                    .handle();
                } else {
                    Index.blockThreadForUpdate();
                }
            }
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot create index: %s - %s (%s)", getIndexName(name))
                            .handle();
        }
    }

    /**
     * Completely wipes the given index.
     *
     * @param name the name of the index. The index prefix of the current system will be added automatically
     */
    public static void deleteIndex(String name) {
        try {
            DeleteIndexResponse res = Index.getClient()
                                           .admin()
                                           .indices()
                                           .prepareDelete(getIndexName(name))
                                           .execute()
                                           .get(10, TimeUnit.SECONDS);
            if (!res.isAcknowledged()) {
                throw Exceptions.handle()
                                .to(LOG)
                                .withSystemErrorMessage("Cannot delete index: %s", getIndexName(name))
                                .handle();
            }
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot delete index: %s - %s (%s)", getIndexName(name))
                            .handle();
        }
    }

    /**
     * Writes the given mapping to the given index.
     *
     * @param index the name of the index. The index prefix of the current system will be added automatically
     * @param type  the entity class which mapping should be written to the given index.
     */
    public static <E extends Entity> void addMapping(String index, Class<E> type) {
        try {
            EntityDescriptor desc = schema.getDescriptor(type);
            PutMappingResponse putRes = Index.getClient()
                                             .admin()
                                             .indices()
                                             .preparePutMapping(getIndexName(index))
                                             .setType(desc.getType())
                                             .setSource(desc.createMapping())
                                             .execute()
                                             .get(10, TimeUnit.SECONDS);
            if (!putRes.isAcknowledged()) {
                throw Exceptions.handle()
                                .to(LOG)
                                .withSystemErrorMessage("Cannot create mapping %s in index: %s",
                                                        type.getSimpleName(),
                                                        getIndexName(index))
                                .handle();
            }
        } catch (Throwable ex) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(ex)
                            .withSystemErrorMessage("Cannot create mapping %s in index: %s - %s (%s)",
                                                    type.getSimpleName(),
                                                    getIndexName(index))
                            .handle();
        }
    }

    /**
     * Creates this entity by storing an initial copy in the database.
     *
     * @param entity the entity to be stored in the database
     * @return the stored entity (with a filled ID etc.)
     */
    public static <E extends Entity> E create(E entity) {
        try {
            return update(entity, false, true);
        } catch (OptimisticLockException e) {
            // Should never happen - but who knows...
            throw Exceptions.handle(LOG, e);
        }
    }

    /**
     * Tries to update the given entity in the database.
     * <p>
     * If the same entity was modified in the database already, an
     * <tt>OptimisticLockException</tt> will be thrown
     * </p>
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws OptimisticLockException if the entity was modified in the database and those changes where not reflected
     *                                 by the entity to be saved
     */
    public static <E extends Entity> E tryUpdate(E entity) throws OptimisticLockException {
        return update(entity, true, false);
    }

    /**
     * Updates the entity in the database.
     * <p>
     * If the entity was modified in the database and those changes where not reflected
     * by the entity to be saved, this operation will fail.
     * </p>
     *
     * @param entity the entity to be written into the DB
     * @return the updated entity
     */
    public static <E extends Entity> E update(E entity) {
        try {
            return update(entity, true, false);
        } catch (OptimisticLockException e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to update '%s' (%s): %s (%s)",
                                                    entity.toString(),
                                                    entity.getId())
                            .handle();
        }
    }

    /**
     * Updates the entity in the database.
     * <p>
     * As change tracking is disabled, this operation will override all previous changes which are not reflected
     * by the entity to be saved
     * </p>
     *
     * @param entity the entity to be written into the DB
     * @return the updated entity
     */
    public static <E extends Entity> E override(E entity) {
        try {
            return update(entity, false, false);
        } catch (OptimisticLockException e) {
            // Should never happen as version checking is disabled....
            throw Exceptions.handle(LOG, e);
        }
    }

    /**
     * Internal save method used by {@link #create(Entity)}, {@link #tryUpdate(Entity)}, {@link #update(Entity)}
     * and {@link #override(Entity)}
     *
     * @param entity              the entity to save
     * @param performVersionCheck determines if change tracking will be enabled
     * @param forceCreate         determines if a new entity should be created
     * @return the saved entity
     * @throws OptimisticLockException if change tracking is enabled and an intermediary change took place
     */
    protected static <E extends Entity> E update(final E entity,
                                                 final boolean performVersionCheck,
                                                 final boolean forceCreate) throws OptimisticLockException {
        try {
            entity.beforeSaveChecks();

            final Map<String, Object> source = new TreeMap<String, Object>();

            entity.performSaveChecks();
            entity.beforeSave();
            getDescriptor(entity.getClass()).writeTo(entity, source);

            if (LOG.isFINE()) {
                LOG.FINE("SAVE[CREATE: %b, LOCK: %b]: %s.%s: %s",
                         forceCreate,
                         performVersionCheck,
                         getIndex(entity),
                         getDescriptor(entity.getClass()).getType(),
                         Strings.join(source));
            }

            String id = entity.getId();
            if (NEW.equals(id)) {
                id = null;
            }
            if (id == null || "".equals(id)) {
                id = entity.computePossibleId();
            }

            IndexRequestBuilder irb = getClient().prepareIndex(getIndex(entity),
                                                               getDescriptor(entity.getClass()).getType(),
                                                               id).setCreate(forceCreate).setSource(source);
            if (!entity.isNew() && performVersionCheck) {
                irb.setVersion(entity.getVersion());
            }
            Watch w = Watch.start();
            IndexResponse indexResponse = irb.execute().actionGet();
            if (LOG.isFINE()) {
                LOG.FINE("SAVE: %s.%s: %s (%d) SUCCEEDED",
                         getIndex(entity),
                         getDescriptor(entity.getClass()).getType(),
                         indexResponse.getId(),
                         indexResponse.getVersion());
            }
            entity.id = indexResponse.getId();
            entity.version = indexResponse.getVersion();
            entity.afterSave();
            queryDuration.addValue(w.elapsedMillis());
            w.submitMicroTiming("ES", "UPDATE " + entity.getClass().getName());
            return entity;
        } catch (VersionConflictEngineException e) {
            if (LOG.isFINE()) {
                LOG.FINE("Version conflict on updating: %s", entity);
            }
            optimisticLockErrors.inc();
            throw new OptimisticLockException(e, entity);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to update '%s' (%s): %s (%s)",
                                                    entity.toString(),
                                                    entity.getId())
                            .handle();
        }
    }

    /**
     * Returns the descriptor for the given class.
     *
     * @param clazz the class which descriptor is request
     * @return the descriptor for the given class
     */
    public static EntityDescriptor getDescriptor(Class<? extends Entity> clazz) {
        return schema.getDescriptor(clazz);
    }

    /**
     * Returns the class for the given type name.
     *
     * @param name the name of the type which class is requested
     * @return the class which is associated with the given type
     */
    public static Class<? extends Entity> getType(String name) {
        return schema.getType(name);
    }

    /**
     * Tries to find the entity of the given type with the given id.
     *
     * @param clazz the type of the entity
     * @param id    the id of the entity
     * @return the entity of the given class with the given id or <tt>null</tt> if no such entity exists
     */
    public static <E extends Entity> E find(final Class<E> clazz, String id) {
        return find(null, null, clazz, id);
    }

    /**
     * Tries to find the entity of the given type with the given id and routing.
     *
     * @param routing the value used to compute the routing hash
     * @param clazz   the type of the entity
     * @param id      the id of the entity
     * @return the entity of the given class with the given id or <tt>null</tt> if no such entity exists
     */
    public static <E extends Entity> E find(String routing, final Class<E> clazz, String id) {
        return find(null, routing, clazz, id);
    }

    /**
     * Tries to find the entity of the given type with the given id.
     *
     * @param index the index to use. The current index prefix will be automatically added. Can be left <tt>null</tt>
     *              to use the default index
     * @param clazz the type of the entity
     * @param id    the id of the entity
     * @return the entity of the given class with the given id or <tt>null</tt> if no such entity exists
     */
    public static <E extends Entity> E find(@Nullable String index,
                                            @Nullable String routing,
                                            @Nonnull final Class<E> clazz,
                                            String id) {
        try {
            if (Strings.isEmpty(id)) {
                return null;
            }
            if (NEW.equals(id)) {
                E e = clazz.newInstance();
                e.setId(NEW);
                return e;
            }
            if (index == null) {
                index = getIndex(clazz);
            } else {
                index = getIndexName(index);
            }
            EntityDescriptor descriptor = getDescriptor(clazz);
            if (LOG.isFINE()) {
                LOG.FINE("FIND: %s.%s: %s", index, descriptor.getType(), id);
            }
            Watch w = Watch.start();
            try {
                if (descriptor.hasRouting() && routing == null) {
                    LOG.WARN(
                            "Trying to FIND an entity of type %s (with id %s) without providing a routing! This will must probably FAIL!",
                            clazz.getName(),
                            id);
                }
                GetResponse res = getClient().prepareGet(index, descriptor.getType(), id)
                                             .setPreference("_primary")
                                             .setRouting(routing)
                                             .execute()
                                             .actionGet();
                if (!res.isExists()) {
                    if (LOG.isFINE()) {
                        LOG.FINE("FIND: %s.%s: NOT FOUND", index, descriptor.getType());
                    }
                    return null;
                } else {
                    E entity = clazz.newInstance();
                    entity.initSourceTracing();
                    entity.setId(res.getId());
                    entity.setVersion(res.getVersion());
                    descriptor.readSource(entity, res.getSource());
                    if (LOG.isFINE()) {
                        LOG.FINE("FIND: %s.%s: FOUND: %s", index, descriptor.getType(), Strings.join(res.getSource()));
                    }
                    return entity;
                }
            } finally {
                queryDuration.addValue(w.elapsedMillis());
                w.submitMicroTiming("ES", "UPDATE " + clazz.getName());
            }
        } catch (Throwable t) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(t)
                            .withSystemErrorMessage("Failed to find '%s' (%s): %s (%s)", id, clazz.getName())
                            .handle();
        }
    }

    /**
     * Returns the name of the index associated with the given entity.
     *
     * @param entity the entity which index is requested
     * @return the index name associated with the given entity
     */
    protected static <E extends Entity> String getIndex(E entity) {
        if (entity != null) {
            String result = entity.getIndex();
            if (result != null) {
                return getIndexName(result);
            }
        }
        return getIndexName(getDescriptor(entity.getClass()).getIndex());
    }

    /**
     * Returns the name of the index associated with the given class.
     *
     * @param clazz the entity type which index is requested
     * @return the index name associated with the given class
     */
    protected static <E extends Entity> String getIndex(Class<E> clazz) {
        return getIndexName(getDescriptor(clazz).getIndex());
    }

    /**
     * Qualifies the given index name with the prefix.
     *
     * @param name the index name to qualify
     */
    protected static <E extends Entity> String getIndexName(String name) {
        return indexPrefix + name;
    }

    /**
     * Tries to delete the given entity unless it was modified since the last read.
     *
     * @param entity the entity to delete
     * @throws OptimisticLockException if the entity was modified since the last read
     */
    public static <E extends Entity> void tryDelete(E entity) throws OptimisticLockException {
        delete(entity, false);
    }

    /**
     * Deletes the given entity
     * <p>
     * If the entity was modified since the last read, this operation will fail.
     * </p>
     *
     * @param entity the entity to delete
     */
    public static <E extends Entity> void delete(E entity) {
        try {
            delete(entity, false);
        } catch (OptimisticLockException e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to delete '%s' (%s): %s (%s)",
                                                    entity.toString(),
                                                    entity.getId())
                            .handle();
        }
    }

    /**
     * Deletes the given entity without any change tracking. Therefore the entity will also be deleted, if it was
     * modified since the last read.
     *
     * @param entity the entity to delete
     */
    public static <E extends Entity> void forceDelete(E entity) {
        try {
            delete(entity, true);
        } catch (OptimisticLockException e) {
            // Should never happen as version checking is disabled....
            throw Exceptions.handle(LOG, e);
        }
    }

    /**
     * Handles all kinds of deletes
     *
     * @param entity the entity to delete
     * @param force  determines whether optimistic locking is suppressed (<tt>true</tt>) or not
     * @throws OptimisticLockException if the entity was changed since the last read
     */
    protected static <E extends Entity> void delete(final E entity,
                                                    final boolean force) throws OptimisticLockException {
        try {
            if (entity.isNew()) {
                return;
            }
            if (LOG.isFINE()) {
                LOG.FINE("DELETE[FORCE: %b]: %s.%s: %s",
                         force,
                         getIndex(entity.getClass()),
                         getDescriptor(entity.getClass()).getType(),
                         entity.getId());
            }
            entity.performDeleteChecks();
            Watch w = Watch.start();
            DeleteRequestBuilder drb = getClient().prepareDelete(getIndex(entity),
                                                                 getDescriptor(entity.getClass()).getType(),
                                                                 entity.getId());
            if (!force) {
                drb.setVersion(entity.getVersion());
            }
            drb.execute().actionGet();
            entity.deleted = true;
            queryDuration.addValue(w.elapsedMillis());
            w.submitMicroTiming("ES", "DELETE " + entity.getClass().getName());
            entity.cascadeDelete();
            if (LOG.isFINE()) {
                LOG.FINE("DELETE: %s.%s: %s SUCCESS",
                         getIndex(entity.getClass()),
                         getDescriptor(entity.getClass()).getType(),
                         entity.getId());
            }
        } catch (VersionConflictEngineException e) {
            if (LOG.isFINE()) {
                LOG.FINE("Version conflict on updating: %s", entity);
            }
            optimisticLockErrors.inc();
            throw new OptimisticLockException(e, entity);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to delete '%s' (%s): %s (%s)",
                                                    entity.toString(),
                                                    entity.getId())
                            .handle();
        }
    }

    /**
     * Creates a new query for objects of the given class.
     *
     * @param clazz the class of objects to query
     * @return a new query against the database
     */
    public static <E extends Entity> Query<E> select(Class<E> clazz) {
        return new Query<>(clazz);
    }

    /**
     * Installs the given ElasticSearch client
     *
     * @param client the client to use
     */
    public static void setClient(Client client) {
        Index.client = client;
    }

    /**
     * Returns the underlying ElasticSearch client
     *
     * @return the client used by this class
     */
    public static Client getClient() {
        return client;
    }

    /**
     * Returns the selected index prefix
     *
     * @return the index prefix added to each index name
     */
    public static String getIndexPrefix() {
        return indexPrefix;
    }

    /**
     * Sets the index prefix to use
     *
     * @param indexPrefix the index prefix to use
     */
    public static void setIndexPrefix(String indexPrefix) {
        Index.indexPrefix = indexPrefix;
    }

}
