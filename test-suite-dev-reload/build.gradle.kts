plugins {
    `java-library`
    id("io.micronaut.build.internal.sql-base")
}

dependencies {
    testImplementation(platform(mn.micronaut.core.bom))
    testImplementation(projects.micronautJdbcHikari)
    testImplementation(mn.micronaut.dev.tck)
    // the reload harness compiles the application under test with the processors on the test classpath
    testImplementation(mn.micronaut.inject.java)

    testRuntimeOnly(libs.managed.h2)

    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.test {
    useJUnitPlatform()
}
