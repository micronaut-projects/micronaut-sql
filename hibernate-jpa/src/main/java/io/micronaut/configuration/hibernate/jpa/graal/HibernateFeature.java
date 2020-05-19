package io.micronaut.configuration.hibernate.jpa.graal;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.hibernate.dialect.*;

/**
 * Feature for automatically configuring the dialect for the active driver with GraalVM.
 *
 * @author graemerocher
 * @since 2.2.1
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
    }

    private void registerIfPresent(BeforeAnalysisAccess access, String name, Class<? extends Dialect>...dialects) {
        Class<?> h2 = access.findClassByName(name);
        if (h2 != null) {
            for (Class<? extends Dialect> dialect : dialects) {
                RuntimeReflection.register(dialect);
            }
        }
    }
}
