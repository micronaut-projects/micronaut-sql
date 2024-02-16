import io.micronaut.testresources.buildtools.KnownModules.JDBC_ORACLE_XE

plugins {
    id("io.micronaut.build.internal.test-application")
}

dependencies {
    implementation(projects.micronautJdbcUcp)
    implementation(projects.micronautTests.micronautCommonSync)

    implementation(libs.ucp)

    runtimeOnly(mnOraclecloud.oracle.jdbc)

    testImplementation(projects.micronautTests.micronautCommonTests)
    testImplementation(mnData.micronaut.data.tx.jdbc)
}

micronaut {
    testResources {
        additionalModules.add(JDBC_ORACLE_XE)
    }
}
