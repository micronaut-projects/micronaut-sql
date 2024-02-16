plugins {
    id("io.micronaut.build.internal.sql-module")
}

dependencies {
    api(projects.micronautJdbc)
    api(mn.micronaut.inject)
    api(mn.micronaut.context)
    api(mnOraclecloud.oracle.jdbc)
    implementation(platform(mnOraclecloud.boms.oracle.jdbc))
    implementation(libs.ucp)

    testRuntimeOnly(libs.managed.h2)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.management)
    testImplementation(mnMicrometer.micronaut.micrometer.core)
    testImplementation(mnCache.micronaut.cache.core)

    testImplementation(mnSpring.spring.jdbc)
    testImplementation(platform(mnOraclecloud.boms.oracle.jdbc))
    testImplementation(libs.ojdbc11dms)
    testImplementation(libs.dms)
}
