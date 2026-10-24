package example.vertx.oracle;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import org.testcontainers.oracle.OracleContainer;

import java.util.Map;

/**
 * Supplies the address and credentials of a shared Oracle Free test container to the Python tests run with the
 * {@code vertx-oracle} environment, like {@code TestPropertyProvider} does for the Java, Kotlin and Groovy suites.
 * {@code application-vertx-oracle.yml} maps them to the Vert.x Oracle client properties, so that the
 * {@code vertx.oracle.client} configuration exists when the bean conditions are evaluated.
 * <p>
 * The configurer is written in Java because Micronaut Test calls {@code TestPropertyProvider} before the
 * application context, and with it the GraalPy runtime, exists, so a Python test class cannot supply the
 * container properties.
 */
@ContextConfigurer
public class OracleTestConfigurer implements ApplicationContextConfigurer {

    private static final String ENVIRONMENT = "vertx-oracle";
    private static OracleContainer container;

    @Override
    public void configure(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (environment.getActiveNames().contains(ENVIRONMENT)) {
            OracleContainer oracle = container();
            environment.addPropertySource(PropertySource.of(ENVIRONMENT, Map.of(
                "test.oracle.host", oracle.getHost(),
                "test.oracle.port", oracle.getOraclePort(),
                "test.oracle.service-name", oracle.getDatabaseName(),
                "test.oracle.user", oracle.getUsername(),
                "test.oracle.password", oracle.getPassword()
            )));
        }
    }

    private static synchronized OracleContainer container() {
        if (container == null) {
            container = new OracleContainer("gvenzl/oracle-free:23.26.1-faststart");
            container.start();
            Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        }
        return container;
    }
}
