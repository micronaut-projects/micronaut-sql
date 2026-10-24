package example.jdbc.ucp.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
@Property(name = "datasources.default.db-type", value = "oracle")
@Property(name = "datasources.default.connection-factory-class-name", value = "oracle.jdbc.xa.client.OracleXADataSource")
@Property(name = "datasources.default.schema-generate", value = "NONE")
@Property(name = "test-resources.containers.oracle.startup-timeout", value = "600s")
@Property(name = "oracle.ucp.destroyOnReload", value = "true")
class OracleXaTest {

    @Inject
    XADataSource xaDataSource;

    @Test
    void commitsATwoPhaseTransactionBranchThroughTheXaPool() throws Exception {
        execute(statement -> statement.execute("CREATE TABLE xa_note (id NUMBER PRIMARY KEY, text VARCHAR2(100))"));
        Xid xid = new TestXid(1, "gtrid-1", "bqual-1");
        XAConnection xaConnection = xaDataSource.getXAConnection();
        try {
            XAResource resource = xaConnection.getXAResource();
            resource.start(xid, XAResource.TMNOFLAGS);
            try (Statement statement = xaConnection.getConnection().createStatement()) {
                statement.executeUpdate("INSERT INTO xa_note VALUES (1, 'two phase')");
            }
            resource.end(xid, XAResource.TMSUCCESS);
            assertEquals(XAResource.XA_OK, resource.prepare(xid));
            resource.commit(xid, false);
        } finally {
            xaConnection.close();
        }

        // a new connection, outside the global transaction, sees the committed row
        execute(statement -> {
            try (ResultSet resultSet = statement.executeQuery("SELECT text FROM xa_note WHERE id = 1")) {
                assertTrue(resultSet.next());
                assertEquals("two phase", resultSet.getString(1));
            }
            statement.execute("DROP TABLE xa_note");
        });
    }

    private void execute(SqlWork work) throws Exception {
        XAConnection xaConnection = xaDataSource.getXAConnection();
        try (Connection connection = xaConnection.getConnection(); Statement statement = connection.createStatement()) {
            work.run(statement);
        } finally {
            xaConnection.close();
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
