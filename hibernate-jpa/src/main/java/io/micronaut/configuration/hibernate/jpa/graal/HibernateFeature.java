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
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.hibernate.dialect.*;

/**
 * Feature for automatically configuring the dialect for the active driver with GraalVM.
 *
 * @author graemerocher
 * @since 1.4.1
 */
@AutomaticFeature
final class HibernateFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerIfPresent(access, "org.h2.Driver", H2Dialect.class);

        registerIfPresent(access, "org.postgresql.Driver",
                PostgreSQL9Dialect.class,
                PostgreSQL10Dialect.class,
                PostgreSQL91Dialect.class,
                PostgreSQL92Dialect.class,
                PostgreSQL93Dialect.class,
                PostgreSQL94Dialect.class,
                PostgreSQL95Dialect.class,
                PostgreSQL81Dialect.class,
                PostgreSQL82Dialect.class
        );

        registerIfPresent(access, "com.microsoft.sqlserver.jdbc.SQLServerResource",
                SQLServer2005Dialect.class,
                SQLServer2008Dialect.class,
                SQLServer2012Dialect.class);

        registerIfPresent(access, "org.mariadb.jdbc.Driver",
                MariaDBDialect.class,
                MariaDB10Dialect.class,
                MariaDB102Dialect.class,
                MariaDB103Dialect.class,
                MariaDB53Dialect.class);

        registerIfPresent(access, "oracle.jdbc.OracleDriver",
                Oracle8iDialect.class,
                Oracle9iDialect.class,
                Oracle10gDialect.class,
                Oracle12cDialect.class);

        registerIfPresent(access, "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                SQLServerDialect.class,
                SQLServer2005Dialect.class,
                SQLServer2008Dialect.class,
                SQLServer2012Dialect.class);

        registerIfPresent(access, "com.mysql.cj.jdbc.Driver",
                MySQL5Dialect.class,
                MySQL55Dialect.class,
                MySQL57Dialect.class,
                MySQL8Dialect.class);
    }

    private void registerIfPresent(BeforeAnalysisAccess access, String name, Class<? extends Dialect>... dialects) {
        Class<?> driver = access.findClassByName(name);
        if (driver != null) {
            for (Class<? extends Dialect> dialect : dialects) {
                RuntimeReflection.register(dialect);
                RuntimeReflection.registerForReflectiveInstantiation(dialect);
            }
        }
    }
}
