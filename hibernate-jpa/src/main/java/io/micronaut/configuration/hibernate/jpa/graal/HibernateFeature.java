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
package io.micronaut.configuration.hibernate.jpa.graal;

import com.oracle.svm.core.annotate.AutomaticFeature;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
import org.graalvm.nativeimage.hosted.Feature;
import org.hibernate.dialect.*;

/**
 * Feature for automatically configuring the dialect for the active driver with GraalVM.
 *
 * @author graemerocher
 * @author Iván López
 * @since 2.2.1
 */
@AutomaticFeature
@Internal
final class HibernateFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerIfPresent(access, "org.h2.Driver",
                H2Dialect.class
        );

        Class<?>[] postgresDialects = {PostgreSQLDialect.class};

        registerIfPresent(access, "org.postgresql.Driver", postgresDialects);
        registerIfPresent(access, "io.vertx.pgclient.spi.PgDriver", postgresDialects);

        Class<?>[] mariaDialects = {MariaDBDialect.class,
                MariaDB103Dialect.class};

        registerIfPresent(access, "org.mariadb.jdbc.Driver", mariaDialects);
        registerIfPresent(access, "io.vertx.mysqlclient.spi.MySQLDriver", mariaDialects);

        Class<?>[] oracleDialects = {Oracle12cDialect.class};

        registerIfPresent(access, "oracle.jdbc.OracleDriver", oracleDialects);
        registerIfPresent(access, "io.vertx.oracleclient.spi.OracleDriver", oracleDialects);

        Class<?>[] sqlServerDialects = {SQLServerDialect.class,
                SQLServer2008Dialect.class,
                SQLServer2012Dialect.class};

        registerIfPresent(access, "com.microsoft.sqlserver.jdbc.SQLServerDriver", sqlServerDialects);
        registerIfPresent(access, "io.vertx.mssqlclient.spi.MSSQLDriver", sqlServerDialects);

        Class<?>[] mysqlDialects = {MySQL57Dialect.class,
                MySQL8Dialect.class};

        registerIfPresent(access, "com.mysql.cj.jdbc.Driver", mysqlDialects);
        registerIfPresent(access, "io.vertx.mysqlclient.spi.MySQLDriver", mysqlDialects);

        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.persister.entity.JoinedSubclassEntityPersister");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.persister.entity.SingleTableEntityPersister");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.persister.entity.UnionSubclassEntityPersister");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.persister.collection.OneToManyPersister");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.persister.collection.BasicCollectionPersister");

        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.sql.ordering.antlr.NodeSupport");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.sql.ordering.antlr.SortKey");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.sql.ordering.antlr.OrderingSpecification");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(
            access, "org.hibernate.sql.ordering.antlr.SortSpecification");
        AutomaticFeatureUtils.registerClassForRuntimeReflectionAndReflectiveInstantiation(
            access, "org.hibernate.sql.ordering.antlr.OrderByFragment");
        AutomaticFeatureUtils.registerConstructorsForRuntimeReflection(access, "antlr.CommonToken");
    }

    private void registerIfPresent(BeforeAnalysisAccess access, String name, Class<?>... dialects) {
        Class<?> driver = access.findClassByName(name);
        boolean present = driver != null;
        if (present) {
            for (Class<?> dialect : dialects) {
                AutomaticFeatureUtils.registerClassForRuntimeReflectionAndReflectiveInstantiation(access, dialect.getName());
            }
        }
    }
}
