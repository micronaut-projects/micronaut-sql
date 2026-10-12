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
package example.hibernate.sync;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the Hibernate {@code SessionFactory} is closed before its pooled data source, so that the
 * {@code create-drop} schema drop runs against a live connection pool when the context stops.
 */
class H2CreateDropShutdownTest {

    private static final String URL = "jdbc:h2:mem:createDropShutdown;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Test
    void schemaIsDroppedBeforeTheDataSourceIsClosed() throws SQLException {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            try (ApplicationContext context = ApplicationContext.run(Map.of(
                "datasources.default.url", URL,
                "jpa.default.properties.hibernate.hbm2ddl.auto", "create-drop"
            ))) {
                assertTrue(context.isRunning());
                assertTrue(tableExists("PET"), "create-drop should create the schema on startup");
            }
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }

        assertFalse(tableExists("PET"), "create-drop should drop the schema on shutdown");
        List<String> closedPoolEvents = appender.list.stream()
            .filter(event -> event.getFormattedMessage().contains("has been closed")
                || (event.getThrowableProxy() != null
                    && ThrowableProxyUtil.asString(event.getThrowableProxy()).contains("has been closed")))
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
        assertEquals(List.of(), closedPoolEvents);
    }

    private static boolean tableExists(String table) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             ResultSet tables = connection.getMetaData().getTables(null, null, table, null)) {
            return tables.next();
        }
    }
}
