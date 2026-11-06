package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.BeanIdentifier
import io.micronaut.jdbc.DataSourceResolver
import oracle.ucp.UniversalConnectionPool
import oracle.ucp.UniversalConnectionPoolAdapter
import oracle.ucp.UniversalConnectionPoolException
import oracle.ucp.UniversalConnectionPoolLifeCycleState
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import oracle.ucp.jdbc.PoolDataSourceImpl
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.SQLException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class ConnectionPoolManagerListenerSpec extends Specification {

    void "test ucp-manager is enabled by default but does not manage any pools"() {
        given:
        var applicationContext = ApplicationContext.run("test")

        expect:
        !applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[0]

        cleanup:
        applicationContext.close()
    }

    void "test ucp-manager is enabled by default and managing default pool"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": ""],
                "test")

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[]{"default"}

        cleanup:
        applicationContext.close()
    }

    void "test ucp-manager is enabled by default and managing default pool with custom name"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                        "datasources.default.connectionPoolName": "JDBC_UCP"],
                "test")

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[]{"JDBC_UCP"}

        cleanup:
        applicationContext.close()
    }

    void "test ucp-manager keeps the pool a connection taken by an earlier listener started (wrapped: #wrapped)"() {
        given:
        var poolName = wrapped ? "EARLY_CONNECTION_WRAPPED" : "EARLY_CONNECTION"
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:${poolName};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE".toString(),
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                        "datasources.default.connectionPoolName": poolName,
                        "test.connection-on-data-source-created": "true",
                        "test.wrap-data-source": wrapped.toString()],
                "test")
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        expect:
        (applicationContext.getBean(DataSource) instanceof TransactionAwareDataSourceProxy) == wrapped
        poolManager.getConnectionPoolNames() == new String[]{poolName}
        poolManager.getConnectionPool(poolName).getLifeCycleState() == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING

        when:
        applicationContext.close()

        then:
        poolManager.getConnectionPoolNames() == new String[0]

        cleanup: "a context that failed to start leaves its pool registered with the manager"
        if (applicationContext?.isRunning()) {
            applicationContext.close()
        }
        destroyPoolIfRegistered(poolName)

        where:
        wrapped << [true, false]
    }

    void "test ucp-manager still rejects two data sources with the same pool name"() {
        when:
        ApplicationContext.run(
                [
                        "datasources.first.url": "jdbc:h2:mem:samePoolFirst;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.first.username": "sa",
                        "datasources.first.password": "",
                        "datasources.first.connectionPoolName": "SAME_POOL",
                        "datasources.second.url": "jdbc:h2:mem:samePoolSecond;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.second.username": "sa",
                        "datasources.second.password": "",
                        "datasources.second.connectionPoolName": "SAME_POOL"],
                "test")

        then:
        var e = thrown(BeanInstantiationException)
        Throwable rootCause = e
        while (rootCause.cause != null) {
            rootCause = rootCause.cause
        }
        rootCause instanceof UniversalConnectionPoolException
        rootCause.message.startsWith("UCP-45350")

        cleanup: "the context failed to start, so the pool of the first data source is still registered"
        destroyPoolIfRegistered("SAME_POOL")
    }

    void "test ucp-manager starts a pool that was created but not started"() {
        given:
        var poolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        var dataSource = poolDataSource("CREATED_NOT_STARTED", "jdbc:h2:mem:createdNotStarted;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
        poolManager.createConnectionPool((UniversalConnectionPoolAdapter) dataSource)

        when:
        new ConnectionPoolManagerListener(poolManager, null).onCreated(createdEvent(dataSource))

        then:
        poolManager.getConnectionPool("CREATED_NOT_STARTED").getLifeCycleState() == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING

        cleanup:
        destroyPoolIfRegistered("CREATED_NOT_STARTED")
    }

    void "test ucp-manager fails when a pool that an earlier connection failed to start still cannot start"() {
        given: "an earlier listener ignored the error of its first connection, which left the pool failed"
        var poolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        var dataSource = poolDataSource("FAILED_TO_START", "jdbc:h2:mem:missingDatabase;IFEXISTS=TRUE")
        dataSource.setInitialPoolSize(1)
        try {
            dataSource.getConnection().close()
        } catch (SQLException ignored) {
        }

        expect:
        poolManager.getConnectionPool("FAILED_TO_START").getLifeCycleState() == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_FAILED

        when:
        new ConnectionPoolManagerListener(poolManager, null).onCreated(createdEvent(dataSource))

        then:
        thrown(ConfigurationException)

        cleanup:
        destroyPoolIfRegistered("FAILED_TO_START")
    }

    void "test ucp-manager creates and starts the pool through the manager for other PoolDataSource implementations"() {
        given: "a PoolDataSource that does not extend PoolDataSourceImpl"
        UniversalConnectionPoolManager poolManager = Mock()
        PoolDataSource dataSource = Mock(PoolDataSource, additionalInterfaces: [UniversalConnectionPoolAdapter])
        dataSource.getConnectionPoolName() >> "OTHER_IMPLEMENTATION"

        when:
        new ConnectionPoolManagerListener(poolManager, null).onCreated(createdEvent(dataSource))

        then:
        1 * poolManager.createConnectionPool(dataSource)

        then:
        1 * poolManager.startConnectionPool("OTHER_IMPLEMENTATION")
    }

    void "test ucp-manager waits while a first connection on another thread starts the pool"() {
        given: "a first connection on another thread has created the pool but not registered or started it yet"
        var poolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        var release = new CountDownLatch(1)
        var dataSource = configure(new PausingPoolDataSource({ release.await(10, TimeUnit.SECONDS) }),
                "STARTED_ON_ANOTHER_THREAD", "jdbc:h2:mem:startedOnAnotherThread;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
        var executor = Executors.newFixedThreadPool(2)
        var firstConnection = executor.submit({ dataSource.getConnection().close() } as Callable)
        assert dataSource.poolCreated.await(10, TimeUnit.SECONDS)
        var listenerRunning = new CountDownLatch(1)
        var listener = executor.submit({
            listenerRunning.countDown()
            new ConnectionPoolManagerListener(poolManager, null).onCreated(createdEvent(dataSource))
        } as Callable)
        // without this, a listener task the executor has not started yet would also pass the timeout below
        assert listenerRunning.await(10, TimeUnit.SECONDS)

        when: "the first connection is still starting the pool"
        listener.get(250, TimeUnit.MILLISECONDS)

        then: "the listener waits for it"
        thrown(TimeoutException)

        when:
        release.countDown()
        firstConnection.get(20, TimeUnit.SECONDS)
        listener.get(20, TimeUnit.SECONDS)

        then: "both succeed and the pool is registered and running"
        poolManager.getConnectionPoolNames().contains("STARTED_ON_ANOTHER_THREAD")
        poolManager.getConnectionPool("STARTED_ON_ANOTHER_THREAD").getLifeCycleState() == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING

        cleanup:
        release.countDown()
        executor?.shutdownNow()
        destroyPoolIfRegistered("STARTED_ON_ANOTHER_THREAD")
    }

    void "test ucp-manager rejects one of two data sources that register the same pool name at the same time"() {
        given: "each data source waits after creating its pool until the other one has created its pool too"
        var poolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        var bothCreated = new CyclicBarrier(2)
        var first = configure(new PausingPoolDataSource({ bothCreated.await(10, TimeUnit.SECONDS) }),
                "COLLISION", "jdbc:h2:mem:collisionFirst;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
        var second = configure(new PausingPoolDataSource({ bothCreated.await(10, TimeUnit.SECONDS) }),
                "COLLISION", "jdbc:h2:mem:collisionSecond;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
        var executor = Executors.newFixedThreadPool(2)

        when:
        var results = [first, second].collect { dataSource ->
            executor.submit({
                try {
                    new ConnectionPoolManagerListener(poolManager, null).onCreated(createdEvent(dataSource))
                    return "started"
                } catch (ConfigurationException ignored) {
                    return "rejected"
                }
            } as Callable)
        }.collect { it.get(20, TimeUnit.SECONDS) }

        then: "both created a pool before either registered it, and only one of them started"
        first.poolCreated.count == 0
        second.poolCreated.count == 0
        results.sort() == ["rejected", "started"]

        cleanup:
        executor?.shutdownNow()
        destroyPoolIfRegistered("COLLISION")
    }

    void "test disabled ucp-manager"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled": false,
                ],
                "test")

        expect:
        !applicationContext.containsBean(ConnectionPoolManagerListener)
        !applicationContext.containsBean(UniversalConnectionPoolManager)

        cleanup:
        applicationContext.close()
    }

    void "starting a pool that is already managed, as development mode does to a retained pool, keeps it running"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": ""],
                "test")
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)
        var dataSource = applicationContext.getBean(DataSource)
        var pool = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT).resolve(dataSource) as PoolDataSource
        var listener = new ConnectionPoolManagerListener(poolManager, null)

        when:
        listener.onCreated(new BeanCreatedEvent<DataSource>(applicationContext, applicationContext.getBeanDefinition(DataSource), BeanIdentifier.of("default"), dataSource))

        then:
        poolManager.getConnectionPoolNames() == new String[]{"default"}
        poolManager.getConnectionPool("default").lifeCycleState == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING
        pool.connection.withCloseable { it.valid }

        cleanup:
        applicationContext.close()
    }

    private static PoolDataSource poolDataSource(String poolName, String url) {
        return configure(PoolDataSourceFactory.getPoolDataSource(), poolName, url)
    }

    private static <T extends PoolDataSource> T configure(T dataSource, String poolName, String url) {
        dataSource.setConnectionFactoryClassName("org.h2.Driver")
        dataSource.setURL(url)
        dataSource.setUser("sa")
        dataSource.setPassword("")
        dataSource.setConnectionPoolName(poolName)
        return dataSource
    }

    private BeanCreatedEvent<DataSource> createdEvent(DataSource dataSource) {
        return new BeanCreatedEvent<DataSource>(Mock(BeanContext), Mock(BeanDefinition), Mock(BeanIdentifier), dataSource)
    }

    // The UCP manager is shared by the whole JVM, so a pool left behind by a failed start would break later tests
    private static void destroyPoolIfRegistered(String poolName) {
        var poolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        if (poolManager.getConnectionPoolNames().contains(poolName)) {
            poolManager.destroyConnectionPool(poolName)
        }
    }

    // createUniversalConnectionPool() checks that no pool with this name is registered and creates the pool, and the
    // manager registers the pool once it returns, so pausing in between holds a data source in the middle of starting
    // its pool
    static class PausingPoolDataSource extends PoolDataSourceImpl {
        final CountDownLatch poolCreated = new CountDownLatch(1)
        private final Closure pause

        PausingPoolDataSource(Closure pause) {
            this.pause = pause
        }

        @Override
        UniversalConnectionPool createUniversalConnectionPool() {
            UniversalConnectionPool pool = super.createUniversalConnectionPool()
            poolCreated.countDown()
            pause.call()
            return pool
        }
    }
}
