package io.micronaut.sql.ojdbc.extensions.test;

import java.util.Map;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class Oracle {
    public static final String IMAGE_NAME = "gvenzl/oracle-free:latest-faststart";
    private static OracleContainer container;
    public static Map<String, String> getProperties() {
        if (container == null) {
            container = new OracleContainer(DockerImageName.parse(IMAGE_NAME));
            container.start();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while (!container.isRunning());
        }
        return getProperties(container);
    }

    private static Map<String, String> getProperties(OracleContainer container) {
        return Map.of(
            "datasources.default.url", container.getJdbcUrl(),
            "datasources.default.username", container.getUsername()
        );
    }

}
