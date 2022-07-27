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

import java.util.Arrays;
import java.util.Collections;

import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
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
import static io.micronaut.core.graal.AutomaticFeatureUtils.registerFieldsForRuntimeReflection;
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
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        handleH2(access);

        handlePostgres(access);

        handleMariadb(access);

        handleSqlServer(access);

        handleMySql(access);
    }

    private void handleH2(BeforeAnalysisAccess access) {
        Class<?> h2Driver = access.findClassByName(H2_DRIVER);
        if (h2Driver != null) {
            registerFieldsAndMethodsWithReflectiveAccess(access, "org.h2.mvstore.db.MVTableEngine");

            registerClassForRuntimeReflection(access, H2_DRIVER);
            initializeAtBuildTime(access, H2_DRIVER);

            Collections.singletonList("org.h2.engine.Constants")
                    .forEach(s -> {
                        registerClassForRuntimeReflection(access, s);
                        registerMethodsForRuntimeReflection(access, s);
                        registerFieldsForRuntimeReflection(access, s);
                    });

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
            initializePackagesAtRunTime("org.mariadb.jdbc.internal.failover.impl");
            initializeAtRunTime(access, "org.mariadb.jdbc.internal.com.send.authentication.SendPamAuthPacket");

            initializeAtBuildTime(access, "java.sql.DriverManager");
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
            Arrays.asList(
                    "com.mysql.cj.exceptions.AssertionFailedException",
                    "com.mysql.cj.exceptions.CJCommunicationsException",
                    "com.mysql.cj.exceptions.CJConnectionFeatureNotAvailableException",
                    "com.mysql.cj.exceptions.CJException",
                    "com.mysql.cj.exceptions.CJOperationNotSupportedException",
                    "com.mysql.cj.exceptions.CJPacketTooBigException",
                    "com.mysql.cj.exceptions.CJTimeoutException",
                    "com.mysql.cj.exceptions.ClosedOnExpiredPasswordException",
                    "com.mysql.cj.exceptions.ConnectionIsClosedException",
                    "com.mysql.cj.exceptions.DataConversionException",
                    "com.mysql.cj.exceptions.DataReadException",
                    "com.mysql.cj.exceptions.DataTruncationException",
                    "com.mysql.cj.exceptions.FeatureNotAvailableException",
                    "com.mysql.cj.exceptions.InvalidConnectionAttributeException",
                    "com.mysql.cj.exceptions.MysqlErrorNumbers",
                    "com.mysql.cj.exceptions.NumberOutOfRange",
                    "com.mysql.cj.exceptions.OperationCancelledException",
                    "com.mysql.cj.exceptions.PasswordExpiredException",
                    "com.mysql.cj.exceptions.PropertyNotModifiableException",
                    "com.mysql.cj.exceptions.RSAException",
                    "com.mysql.cj.exceptions.SSLParamsException",
                    "com.mysql.cj.exceptions.StatementIsClosedException",
                    "com.mysql.cj.exceptions.UnableToConnectException",
                    "com.mysql.cj.exceptions.UnsupportedConnectionStringException",
                    "com.mysql.cj.exceptions.WrongArgumentException"
            ).forEach(name -> {
                AutomaticFeatureUtils.registerClassForRuntimeReflection(access, name);
                AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(access, name);
            });
            registerFieldsAndMethodsWithReflectiveAccess(access, MYSQL_DRIVER);

            registerAllForRuntimeReflection(access, "com.mysql.cj.log.StandardLogger");
            registerAllForRuntimeReflection(access, "com.mysql.cj.conf.url.SingleConnectionUrl");
            // Required for X protocol
            registerAllForRuntimeReflection(access, "com.mysql.cj.conf.url.XDevApiConnectionUrl");
            registerAllForRuntimeReflection(access, "com.mysql.cj.protocol.x.SyncFlushDeflaterOutputStream");
            registerAllForRuntimeReflection(access, "java.util.zip.InflaterInputStream");

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
