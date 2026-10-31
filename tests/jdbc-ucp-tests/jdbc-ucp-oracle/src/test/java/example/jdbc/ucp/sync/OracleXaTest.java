package example.jdbc.ucp.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.data.connection.jdbc.advice.DelegatingDataSource;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import oracle.ucp.jdbc.PoolDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
@Property(name = "datasources.default.db-type", value = "oracle")
@Property(name = "datasources.default.xa", value = "true")
@Property(name = "datasources.default.connection-factory-class-name", value = "oracle.jdbc.xa.client.OracleXADataSource")
@Property(name = "datasources.default.schema-generate", value = "NONE")
@Property(name = "test-resources.containers.oracle.startup-timeout", value = "600s")
@Property(name = "oracle.ucp.destroyOnReload", value = "true")
class OracleXaTest {

    @Inject
    XADataSource xaDataSource;

    DataSource dataSource;

    @Inject
    void setDataSource(DataSource dataSource) {
        // the pool itself, rather than the Micronaut Data proxy that needs a connection open in the context
        this.dataSource = DelegatingDataSource.unwrapDataSource(dataSource);
    }

    @BeforeEach
    void createTable() throws Exception {
        execute(statement -> statement.execute("CREATE TABLE xa_note (id NUMBER PRIMARY KEY, text VARCHAR2(100))"));
    }

    @AfterEach
    void dropTable() throws Exception {
        execute(statement -> statement.execute("DROP TABLE xa_note"));
    }

    @Test
    void commitsATwoPhaseTransactionBranchThroughTheXaPool() throws Exception {
        PoolDataSource pool = (PoolDataSource) dataSource;
        Xid xid = new TestXid(1, "gtrid-commit", "bqual-1");
        XAConnection xaConnection = xaDataSource.getXAConnection();
        try {
            assertEquals(1, pool.getBorrowedConnectionsCount());
            XAResource resource = xaConnection.getXAResource();
            resource.start(xid, XAResource.TMNOFLAGS);
            insert(xaConnection, "two phase");
            resource.end(xid, XAResource.TMSUCCESS);
            assertEquals(XAResource.XA_OK, resource.prepare(xid));
            resource.commit(xid, false);
        } finally {
            xaConnection.close();
        }
        assertEquals(0, pool.getBorrowedConnectionsCount());

        // the DataSource bean opens a local connection, outside the global transaction, that sees the committed row
        assertEquals("two phase", selectText());
    }

    @Test
    void rollsBackAPreparedTransactionBranch() throws Exception {
        Xid xid = new TestXid(1, "gtrid-rollback", "bqual-1");
        XAConnection xaConnection = xaDataSource.getXAConnection();
        try {
            XAResource resource = xaConnection.getXAResource();
            resource.start(xid, XAResource.TMNOFLAGS);
            insert(xaConnection, "rolled back");
            resource.end(xid, XAResource.TMSUCCESS);
            assertEquals(XAResource.XA_OK, resource.prepare(xid));
            resource.rollback(xid);
        } finally {
            xaConnection.close();
        }

        assertEquals(null, selectText());
    }

    @Test
    void recoversAPreparedTransactionBranchFromAnotherPooledConnection() throws Exception {
        Xid xid = new TestXid(1, "gtrid-recover", "bqual-1");
        XAConnection xaConnection = xaDataSource.getXAConnection();
        try {
            XAResource resource = xaConnection.getXAResource();
            resource.start(xid, XAResource.TMNOFLAGS);
            insert(xaConnection, "recovered");
            resource.end(xid, XAResource.TMSUCCESS);
            assertEquals(XAResource.XA_OK, resource.prepare(xid));
        } finally {
            // like a transaction manager that fails after prepare, the branch is left in doubt
            xaConnection.close();
        }

        XAConnection recoveryConnection = xaDataSource.getXAConnection();
        try {
            XAResource resource = recoveryConnection.getXAResource();
            Xid[] inDoubt = resource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
            Xid recovered = Arrays.stream(inDoubt)
                .filter(candidate -> Arrays.equals(candidate.getGlobalTransactionId(), xid.getGlobalTransactionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The prepared branch was not recovered"));
            resource.commit(recovered, false);
            assertFalse(Arrays.stream(resource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN))
                .anyMatch(candidate -> Arrays.equals(candidate.getGlobalTransactionId(), xid.getGlobalTransactionId())));
        } finally {
            recoveryConnection.close();
        }

        assertEquals("recovered", selectText());
    }

    @Test
    void theDataSourceBeanOpensLocalConnectionsFromTheXaPool() throws Exception {
        PoolDataSource pool = (PoolDataSource) dataSource;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO xa_note VALUES (1, 'local')");
            }
            assertEquals(1, pool.getBorrowedConnectionsCount());
            connection.commit();
        }
        assertEquals(0, pool.getBorrowedConnectionsCount());
        assertEquals("local", selectText());
    }

    private static void insert(XAConnection xaConnection, String text) throws Exception {
        try (Statement statement = xaConnection.getConnection().createStatement()) {
            statement.executeUpdate("INSERT INTO xa_note VALUES (1, '" + text + "')");
        }
    }

    private String selectText() throws Exception {
        String[] text = new String[1];
        execute(statement -> {
            try (ResultSet resultSet = statement.executeQuery("SELECT text FROM xa_note WHERE id = 1")) {
                text[0] = resultSet.next() ? resultSet.getString(1) : null;
            }
        });
        return text[0];
    }

    private void execute(SqlWork work) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            work.run(statement);
        }
    }

    private interface SqlWork {
        void run(Statement statement) throws Exception;
    }

    private record TestXid(int formatId, String globalTransactionId, String branchQualifier) implements Xid {

        @Override
        public int getFormatId() {
            return formatId;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return globalTransactionId.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] getBranchQualifier() {
            return branchQualifier.getBytes(StandardCharsets.UTF_8);
        }
    }
}
