/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.sql.dev;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.ApplicationContext;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.jdbc.DataSourceResolver;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a Hibernate JPA application through the development runtime, which restarts it as an entity changes. The
 * next generation builds a session factory of its own, on the same retained pool, whose schema follows the entity
 * as the schema generation configured in {@code application-dev.properties} allows, and the first generation, with
 * its entity classes and the session factory built from them, is collected.
 */
class HibernateRestartReloadTest {

    private static final String BOOK = "example.Book";
    private static final String LIBRARY = "example.Library";
    private static final String SHELF = "example.Shelf";
    private static final String SUPPLIER = "io.micronaut.configuration.hibernate.jpa.dev.DevelopmentSchemaSettingsSupplier";

    @TempDir
    Path project;

    @BeforeAll
    static void initializeH2() throws ClassNotFoundException {
        // H2 preallocates an exception as it initializes TraceObject, whose stack trace would otherwise hold the
        // frames of the first generation that opens a connection, and with them its classes
        Class.forName("org.h2.message.TraceObject", true, HibernateRestartReloadTest.class.getClassLoader());
    }

    @Test
    void createDropFollowsAnAddedFieldOnTheRetainedPool() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "create-drop");
            book(harness, false);
            library(harness, false);
            harness.start();
            HikariDataSource pool = pool(harness.context());
            SessionFactory first = harness.context().getBean(SessionFactory.class);
            invoke(harness.context(), "save", "Dune");
            assertEquals(List.of("Dune"), invoke(harness.context(), "titles"));
            // a lazy reference: Hibernate generates a proxy class of the entity of this generation
            assertEquals(List.of("proxy:Dune"), invoke(harness.context(), "references"));

            book(harness, true);
            library(harness, true);
            harness.reload();
            assertEquals(2, harness.generation());

            assertSame(pool, pool(harness.context()), "the next session factory is built on the retained pool");
            assertFalse(pool.isClosed());
            SessionFactory second = harness.context().getBean(SessionFactory.class);
            assertNotSame(first, second);
            assertTrue(first.isClosed(), "the session factory of the first generation is closed");
            first = null;
            assertTrue(columns(pool, "BOOK").contains("PAGES"), "create-drop created the table with the added column");
            // create-drop dropped the rows with the table as the first generation stopped
            assertEquals(List.of(), invoke(harness.context(), "titles"));
            invoke(harness.context(), "save", "Emma", 474);
            assertEquals(List.of("Emma:474"), invoke(harness.context(), "titles"));
            assertEquals(List.of("proxy:Emma"), invoke(harness.context(), "references"));
            second = null;
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void updateAddsTheColumnAndKeepsTheRows() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "update");
            book(harness, false);
            library(harness, false);
            shelf(harness, "");
            harness.start();
            HikariDataSource pool = pool(harness.context());
            invoke(harness.context(), "save", "Dune");
            // the transactional entity manager, and a criteria query over the metamodel of this generation
            assertEquals(1L, invokeOn(harness.context(), SHELF, "count"));

            book(harness, true);
            library(harness, true);
            shelf(harness, "the second edition");
            harness.reload();
            assertEquals(2, harness.generation());

            assertSame(pool, pool(harness.context()));
            assertTrue(columns(pool, "BOOK").contains("PAGES"), "update added the column");
            invoke(harness.context(), "save", "Emma", 474);
            assertEquals(List.of("Dune:null", "Emma:474"), invoke(harness.context(), "titles"));
            assertEquals(2L, invokeOn(harness.context(), SHELF, "count"));
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void createUpdatesTheSchemaOfTheRetainedPoolOnARestartAndWarns() throws Exception {
        ListAppender<ILoggingEvent> warnings = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(SUPPLIER);
        warnings.start();
        logger.addAppender(warnings);
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, "create");
            book(harness, false);
            library(harness, false);
            harness.start();
            HikariDataSource pool = pool(harness.context());
            invoke(harness.context(), "save", "Dune");
            assertEquals(List.of(), warnings.list, "the first session factory generates the schema as configured");

            book(harness, true);
            library(harness, true);
            harness.reload();
            assertEquals(2, harness.generation());

            assertSame(pool, pool(harness.context()));
            assertEquals(List.of("Dune:null"), invoke(harness.context(), "titles"), "the restart dropped no table");
            assertTrue(columns(pool, "BOOK").contains("PAGES"), "the schema was updated with the added column");
            assertEquals(1, warnings.list.size());
            ILoggingEvent warning = warnings.list.get(0);
            assertEquals(Level.WARN, warning.getLevel());
            assertTrue(warning.getFormattedMessage().contains("jpa.default.properties.hibernate.hbm2ddl.auto=create-drop"), warning.getFormattedMessage());
            ReloadTck.assertRetiredGenerationsCollected(harness);
        } finally {
            logger.detachAppender(warnings);
        }
    }

    private static void database(ReloadHarness harness, String schemaGeneration) {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";LOCK_TIMEOUT=10000";
        harness.property("datasources.default.url", url);
        harness.property("datasources.default.username", "sa");
        harness.property("datasources.default.password", "");
        harness.property("datasources.default.driver-class-name", "org.h2.Driver");
        // as an application sets it for development: the dev environment is active under the development runtime
        harness.resource("application-dev.properties", "jpa.default.properties.hibernate.hbm2ddl.auto=" + schemaGeneration + "\n");
    }

    private static void book(ReloadHarness harness, boolean withPages) {
        harness.source(BOOK, """
            package example;

            import jakarta.persistence.Entity;
            import jakarta.persistence.GeneratedValue;
            import jakarta.persistence.Id;

            @Entity
            public class Book {
                @Id
                @GeneratedValue
                private Long id;
                private String title;
                %s

                public Long getId() {
                    return id;
                }

                public void setId(Long id) {
                    this.id = id;
                }

                public String getTitle() {
                    return title;
                }

                public void setTitle(String title) {
                    this.title = title;
                }
                %s
            }
            """.formatted(
            withPages ? "private Integer pages;" : "",
            withPages ? """
                public Integer getPages() {
                    return pages;
                }

                public void setPages(Integer pages) {
                    this.pages = pages;
                }
                """ : ""));
    }

    private static void library(ReloadHarness harness, boolean withPages) {
        harness.source(LIBRARY, """
            package example;

            import java.util.List;
            import org.hibernate.SessionFactory;

            @jakarta.inject.Singleton
            public class Library {
                private final SessionFactory sessionFactory;

                public Library(SessionFactory sessionFactory) {
                    this.sessionFactory = sessionFactory;
                }

                public void save(String title) {
                    sessionFactory.inTransaction(session -> {
                        Book book = new Book();
                        book.setTitle(title);
                        session.persist(book);
                    });
                }

                %s

                public List<String> references() {
                    return sessionFactory.fromTransaction(session -> session
                        .createSelectionQuery("select id from Book order by title", Long.class)
                        .getResultList()
                        .stream()
                        .map(id -> {
                            Book book = session.getReference(Book.class, id);
                            return (book.getClass() == Book.class ? "entity:" : "proxy:") + book.getTitle();
                        })
                        .toList());
                }

                public List<String> titles() {
                    return sessionFactory.fromTransaction(session -> session
                        .createSelectionQuery("from Book order by title", Book.class)
                        .getResultList()
                        .stream()
                        .map(book -> %s)
                        .toList());
                }
            }
            """.formatted(
            withPages ? """
                public void save(String title, int pages) {
                    sessionFactory.inTransaction(session -> {
                        Book book = new Book();
                        book.setTitle(title);
                        book.setPages(pages);
                        session.persist(book);
                    });
                }
                """ : "",
            withPages ? "book.getTitle() + \":\" + book.getPages()" : "book.getTitle()"));
    }

    private static void shelf(ReloadHarness harness, String comment) {
        harness.source(SHELF, """
            package example;

            import io.micronaut.transaction.annotation.Transactional;
            import jakarta.persistence.EntityManager;
            import jakarta.persistence.criteria.CriteriaBuilder;
            import jakarta.persistence.criteria.CriteriaQuery;

            @jakarta.inject.Singleton
            public class Shelf {
                private final EntityManager entityManager;

                public Shelf(EntityManager entityManager) {
                    this.entityManager = entityManager;
                }

                @Transactional
                public long count() {
                    // %s
                    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
                    CriteriaQuery<Long> query = builder.createQuery(Long.class);
                    query.select(builder.count(query.from(Book.class)));
                    return entityManager.createQuery(query).getSingleResult();
                }
            }
            """.formatted(comment));
    }

    private static Object invoke(ApplicationContext context, String method, Object... args) throws Exception {
        return invokeOn(context, LIBRARY, method, args);
    }

    /**
     * Invokes a method of a bean of the current generation, whose classes the test does not see.
     */
    private static Object invokeOn(ApplicationContext context, String className, String method, Object... args) throws Exception {
        Class<?> type = context.getClassLoader().loadClass(className);
        Object bean = context.getBean(type);
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] instanceof Integer ? int.class : args[i].getClass();
        }
        try {
            return type.getMethod(method, types).invoke(bean, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private static HikariDataSource pool(ApplicationContext context) {
        DataSourceResolver resolver = context.findBean(DataSourceResolver.class).orElse(DataSourceResolver.DEFAULT);
        return (HikariDataSource) resolver.resolve(context.getBean(DataSource.class));
    }

    private static List<String> columns(DataSource dataSource, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.getMetaData().getColumns(null, null, table, null)) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
            }
        }
        return columns;
    }

}
