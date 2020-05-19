package io.micronaut.jdbc.nativeimage;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.configure.ResourcesRegistry;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A JDBC feature that configures JDBC drivers correctly for native image.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@AutomaticFeature
@Internal
final class JdbcFeature implements Feature {

    private static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // h2
        registerDriver(access, "org.h2.Driver");

        // postgres
        registerDriver(access, "org.postgresql.Driver");

        // sql server
        handleSqlServer(access);

        // mariadb
        handleMariadb(access);

        // oracle
        handleOracle(access);

    }

    private void handleOracle(BeforeAnalysisAccess access) {
        Class<?> oracleDriver = access.findClassByName("oracle.jdbc.OracleDriver");
        if (oracleDriver != null) {
            registerReflectionIfPresent(access, "oracle.jdbc.driver.T4CDriverExtension");
            registerReflectionIfPresent(access, "oracle.jdbc.driver.T2CDriverExtension");
            registerReflectionIfPresent(access, "oracle.net.ano.Ano");
            registerReflectionIfPresent(access, "oracle.net.ano.AuthenticationService");
            registerReflectionIfPresent(access, "oracle.net.ano.DataIntegrityService");
            registerReflectionIfPresent(access, "oracle.net.ano.EncryptionService");
            registerReflectionIfPresent(access, "oracle.net.ano.SupervisorService");

            ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
            if (resourcesRegistry != null) {
                resourcesRegistry.addResources("META-INF/services/java.sql.Driver");
                resourcesRegistry.addResources("oracle/sql/converter_xcharset/lx20002.glb");
                resourcesRegistry.addResources("oracle/sql/converter_xcharset/lx2001f.glb");
                resourcesRegistry.addResources("oracle/sql/converter_xcharset/lx200b2.glb");
            }

            initializeAtBuildTime(
                    access,
                    "oracle.net.jdbc.nl.mesg.NLSR_en",
                    "oracle.jdbc.driver.DynamicByteArray",
                    "oracle.sql.ConverterArchive",
                    "oracle.sql.converter.CharacterConverterJDBC",
                    "oracle.sql.converter.CharacterConverter1Byte"
            );

            initializeAtRuntime(
                    access,
                    "java.sql.DriverManager"
            );
        }
    }

    private void handleMariadb(BeforeAnalysisAccess access) {
        Class<?> mariaDriver = access.findClassByName("org.mariadb.jdbc.Driver");
        if (mariaDriver != null) {
            RuntimeReflection.register(mariaDriver);
            registerAllIfPresent(access, "org.mariadb.jdbc.util.Options");
            registerAllAccess(mariaDriver);
            RuntimeClassInitialization
                    .initializeAtBuildTime("org.mariadb");
            RuntimeClassInitialization
                    .initializeAtRunTime("org.mariadb.jdbc.credential.aws");
            initializeAtRuntime(access, "org.mariadb.jdbc.internal.failover.impl.MastersSlavesListener");
            initializeAtRuntime(access, "org.mariadb.jdbc.internal.com.send.authentication.SendPamAuthPacket");
        }
    }

    private void initializeAtRuntime(BeforeAnalysisAccess access, String n) {
        Class<?> t = access.findClassByName(n);
        if (t != null) {
            RuntimeClassInitialization.initializeAtRunTime(t);
        }
    }

    private void initializeAtBuildTime(BeforeAnalysisAccess access, String... names) {
        for (String name : names) {
            Class<?> t = access.findClassByName(name);
            if (t != null) {
                RuntimeClassInitialization.initializeAtBuildTime(t);
            }
        }
    }

    private void registerAllIfPresent(BeforeAnalysisAccess access, String n) {
        Class<?> t = access.findClassByName(n);
        if (t != null) {
            registerAllAccess(t);
        }
    }

    private void registerReflectionIfPresent(BeforeAnalysisAccess access, String n) {
        Class<?> t = access.findClassByName(n);
        if (t != null) {
            RuntimeReflection.register(t);
        }
    }

    private void registerAllAccess(Class<?> t) {
        RuntimeReflection.register(t);
        for (Method method : t.getMethods()) {
            RuntimeReflection.register(method);
        }
        Field[] fields = t.getFields();
        for (Field field : fields) {
            RuntimeReflection.register(field);
        }
    }

    private void handleSqlServer(BeforeAnalysisAccess access) {
        Class<?> sqlServerDriver = access.findClassByName(SQL_SERVER_DRIVER);
        if (sqlServerDriver != null) {

            ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
            if (resourcesRegistry != null) {
                resourcesRegistry
                        .addResourceBundles("com.microsoft.sqlserver.jdbc.SQLServerResource");
            }
            RuntimeReflection.register(sqlServerDriver);
            RuntimeClassInitialization
                    .initializeAtRunTime("java.sql.DriverManager");
            RuntimeClassInitialization
                    .initializeAtBuildTime(SQL_SERVER_DRIVER);
        }
    }

    private void registerDriver(BeforeAnalysisAccess access, String driverName) {
        Class<?> h2Driver = access.findClassByName(driverName);
        if (h2Driver != null) {
            RuntimeReflection.register(h2Driver);
            RuntimeClassInitialization.initializeAtBuildTime(h2Driver);
        }
    }
}
