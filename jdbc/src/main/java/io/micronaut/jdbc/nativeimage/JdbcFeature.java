/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.jdbc.nativeimage;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.configure.ResourcesRegistry;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;

import java.util.Arrays;

import static io.micronaut.core.graal.AutomaticFeatureUtils.addResourceBundles;
import static io.micronaut.core.graal.AutomaticFeatureUtils.addResourcePatterns;
import static io.micronaut.core.graal.AutomaticFeatureUtils.initializeAtBuildTime;
import static io.micronaut.core.graal.AutomaticFeatureUtils.initializeAtRunTime;
import static io.micronaut.core.graal.AutomaticFeatureUtils.initializePackagesAtBuildTime;
import static io.micronaut.core.graal.AutomaticFeatureUtils.initializePackagesAtRunTime;
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerAllForRuntimeReflection;
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerClassForRuntimeReflection;
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerClassForRuntimeReflectionAndReflectiveInstantiation;
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerFieldsAndMethodsWithReflectiveAccess;
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerMethodsForRuntimeReflection;

/**
 * A JDBC feature that configures JDBC drivers correctly for native image.
 *
 * @author graemerocher
 * @author Iván López
 * @since 2.2.1
 */
@AutomaticFeature
@Internal
final class JdbcFeature implements Feature {

    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    private static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";
    private static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    private ResourcesRegistry resourcesRegistry;

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        handleH2(access);

        handlePostgres(access);

        handleMariadb(access);

        handleOracle(access);

        handleSqlServer(access);

        handleMySql(access);
    }

    private void handleH2(BeforeAnalysisAccess access) {
        Class<?> h2Driver = access.findClassByName(H2_DRIVER);
        if (h2Driver != null) {
            registerFieldsAndMethodsWithReflectiveAccess(access, "org.h2.mvstore.db.MVTableEngine");

            registerClassForRuntimeReflection(access, H2_DRIVER);
            initializeAtBuildTime(access, H2_DRIVER);

            registerClassForRuntimeReflection(access, "org.h2.engine.Constants");
            registerMethodsForRuntimeReflection(access, "org.h2.engine.Constants");

            // required for file-based H2 databases
            Arrays.asList(
                    "org.h2.store.fs.FilePathDisk",
                    "org.h2.store.fs.FilePathMem",
                    "org.h2.store.fs.FilePathMemLZF",
                    "org.h2.store.fs.FilePathNioMem",
                    "org.h2.store.fs.FilePathNioMemLZF",
                    "org.h2.store.fs.FilePathSplit",
                    "org.h2.store.fs.FilePathNio",
                    "org.h2.store.fs.FilePathNioMapped",
                    "org.h2.store.fs.FilePathAsync",
                    "org.h2.store.fs.FilePathZip",
                    "org.h2.store.fs.FilePathRetryOnInterrupt"
            ).forEach(c -> registerClassForRuntimeReflectionAndReflectiveInstantiation(access, c));

            initializeAtRunTime(access, "sun.nio.ch.WindowsAsynchronousFileChannelImpl$DefaultIocpHolder");

            addResourcePatterns(
                    "META-INF/services/java.sql.Driver",
                    "org/h2/util/data.zip"
            );

            initializeAtBuildTime(access, "java.sql.DriverManager");
        }
    }

    private void handlePostgres(BeforeAnalysisAccess access) {
        Class<?> postgresDriver = access.findClassByName(POSTGRESQL_DRIVER);
        if (postgresDriver != null) {
            registerClassForRuntimeReflection(access, POSTGRESQL_DRIVER);

            initializeAtBuildTime(access,
                    POSTGRESQL_DRIVER,
                    "org.postgresql.util.SharedTimer"
            );

            registerAllForRuntimeReflection(access, "org.postgresql.PGProperty");

            addResourcePatterns("META-INF/services/java.sql.Driver");

            initializeAtBuildTime(access, "java.sql.DriverManager");
        }
    }

    private void handleMariadb(BeforeAnalysisAccess access) {
        Class<?> mariaDriver = access.findClassByName(MARIADB_DRIVER);
        if (mariaDriver != null) {
            registerFieldsAndMethodsWithReflectiveAccess(access, MARIADB_DRIVER);

            addResourcePatterns("META-INF/services/java.sql.Driver");

            registerFieldsAndMethodsWithReflectiveAccess(access, "org.mariadb.jdbc.util.Options");

            initializePackagesAtBuildTime("org.mariadb");
            initializePackagesAtRunTime("org.mariadb.jdbc.credential.aws");

            initializeAtRunTime(access, "org.mariadb.jdbc.internal.failover.impl.MastersSlavesListener");
            initializeAtRunTime(access, "org.mariadb.jdbc.internal.com.send.authentication.SendPamAuthPacket");

            initializeAtBuildTime(access, "java.sql.DriverManager");
        }
    }

    private void handleOracle(BeforeAnalysisAccess access) {
        Class<?> oracleDriver = access.findClassByName(ORACLE_DRIVER);
        if (oracleDriver != null) {
            Arrays.asList(
                    "oracle.jdbc.driver.T4CDriverExtension",
                    "oracle.jdbc.driver.T2CDriverExtension",
                    "oracle.net.ano.Ano",
                    "oracle.net.ano.AuthenticationService",
                    "oracle.net.ano.DataIntegrityService",
                    "oracle.net.ano.EncryptionService",
                    "oracle.net.ano.SupervisorService"
            ).forEach(c -> registerFieldsAndMethodsWithReflectiveAccess(access, c));

            registerAllForRuntimeReflection(access, "oracle.jdbc.logging.annotations.Supports");
            registerAllForRuntimeReflection(access, "oracle.jdbc.logging.annotations.Feature");

            addResourcePatterns(
                    "META-INF/services/java.sql.Driver",
                    "oracle/sql/converter_xcharset/lx20002.glb",
                    "oracle/sql/converter_xcharset/lx2001f.glb",
                    "oracle/sql/converter_xcharset/lx200b2.glb"
            );

            addResourceBundles(
                    "oracle.net.jdbc.nl.mesg.NLSR",
                    "oracle.net.mesg.Message"
            );

            initializeAtBuildTime(
                    access,
                    "oracle.net.jdbc.nl.mesg.NLSR_en",
                    "oracle.jdbc.driver.DynamicByteArray",
                    "oracle.jdbc.logging.annotations.Supports",
                    "oracle.sql.ConverterArchive",
                    "oracle.sql.converter.CharacterConverterJDBC",
                    "oracle.sql.converter.CharacterConverter1Byte",
                    "com.sun.jmx.mbeanserver.MBeanInstantiator",
                    "com.sun.jmx.mbeanserver.MXBeanLookup",
                    "com.sun.jmx.mbeanserver.Introspector",
                    "com.sun.jmx.defaults.JmxProperties"
            );

            initializeAtRunTime(access, "java.sql.DriverManager");
        }
    }

    private void handleSqlServer(BeforeAnalysisAccess access) {
        Class<?> sqlServerDriver = access.findClassByName(SQL_SERVER_DRIVER);
        if (sqlServerDriver != null) {
            registerFieldsAndMethodsWithReflectiveAccess(access, SQL_SERVER_DRIVER);

            initializeAtBuildTime(access,
                    SQL_SERVER_DRIVER,
                    "com.microsoft.sqlserver.jdbc.Util",
                    "com.microsoft.sqlserver.jdbc.SQLServerException"
            );

            addResourcePatterns(
                    "META-INF/services/java.sql.Driver",
                    "javax.crypto.Cipher.class"
            );
            addResourceBundles("com.microsoft.sqlserver.jdbc.SQLServerResource");

            initializeAtBuildTime(access, "java.sql.DriverManager");
        }
    }

    private void handleMySql(BeforeAnalysisAccess access) {
        Class<?> mysqlDriver = access.findClassByName(MYSQL_DRIVER);
        if (mysqlDriver != null) {
            registerFieldsAndMethodsWithReflectiveAccess(access, MYSQL_DRIVER);

            registerAllForRuntimeReflection(access, "com.mysql.cj.log.StandardLogger");
            registerAllForRuntimeReflection(access, "com.mysql.cj.conf.url.SingleConnectionUrl");

            registerFieldsAndMethodsWithReflectiveAccess(access, "com.mysql.cj.protocol.StandardSocketFactory");
            registerFieldsAndMethodsWithReflectiveAccess(access, "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");

            addResourcePatterns(
                    "META-INF/services/java.sql.Driver",
                    "com/mysql/cj/TlsSettings.properties",
                    "com/mysql/cj/LocalizedErrorMessages.properties",
                    "com/mysql/cj/util/TimeZoneMapping.properties"
            );
            addResourceBundles("com.mysql.cj.LocalizedErrorMessages");

            initializeAtRunTime(access, "java.sql.DriverManager");
        }
    }

}
