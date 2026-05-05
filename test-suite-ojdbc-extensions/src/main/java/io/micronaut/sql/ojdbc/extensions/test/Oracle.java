package io.micronaut.sql.ojdbc.extensions.test;

import java.util.Map;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class Oracle {
    public static final String IMAGE_NAME = "gvenzl/oracle-free:latest-faststart";
    private static volatile OracleContainer container;
    public static Map<String, String> getProperties() {
        OracleContainer current = container;
        if (current == null) {
            synchronized (Oracle.class) {
                current = container;
                if (current == null) {
                    current = new OracleContainer(DockerImageName.parse(IMAGE_NAME));
                    current.start();
                    container = current;
                }
            }
        }
        return getProperties(current);
    }

    private static Map<String, String> getProperties(OracleContainer container) {
        return Map.of(
            "datasources.default.url", container.getJdbcUrl(),
            "datasources.default.username", container.getUsername()
        );
    }

}
