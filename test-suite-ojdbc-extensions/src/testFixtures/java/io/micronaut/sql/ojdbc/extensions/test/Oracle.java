package io.micronaut.sql.ojdbc.extensions.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class Oracle {
    public static final String IMAGE_NAME = "gvenzl/oracle-free:23.26.1-faststart";
    private static volatile OracleContainer container;

    public static Map<String, String> getProperties() {
        return getProperties(container());
    }

    /**
     * Writes an Oracle JDBC configuration file for the {@code config-file} provider and returns
     * properties whose URL points at it. The file supplies the connect descriptor, user and password,
     * so the data source configuration carries neither.
     *
     * @return the data source properties
     */
    public static Map<String, String> getConfigFileProperties() {
        OracleContainer current = container();
        String connectDescriptor = current.getHost() + ":" + current.getOraclePort() + "/" + current.getDatabaseName();
        String password = Base64.getEncoder().encodeToString(current.getPassword().getBytes(StandardCharsets.UTF_8));
        String json = """
            {
              "connect_descriptor": "%s",
              "user": "%s",
              "password": {"type": "base64", "value": "%s"}
            }
            """.formatted(connectDescriptor, current.getUsername(), password);
        try {
            Path file = Files.createTempFile("ojdbc-config", ".json");
            file.toFile().deleteOnExit();
            Files.writeString(file, json);
            return Map.of("datasources.default.url", "jdbc:oracle:thin:@config-file://" + file.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static OracleContainer container() {
        OracleContainer current = container;
        if (current == null) {
            synchronized (Oracle.class) {
                current = container;
                if (current == null) {
                    current = new OracleContainer(DockerImageName.parse(IMAGE_NAME));
                    current.start();
                    Runtime.getRuntime().addShutdownHook(new Thread(current::stop));
                    container = current;
                }
            }
        }
        return current;
    }

    private static Map<String, String> getProperties(OracleContainer container) {
        return Map.of(
            "datasources.default.url", container.getJdbcUrl(),
            "datasources.default.username", container.getUsername()
        );
    }

}
