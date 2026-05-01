plugins {
    id("io.micronaut.build.internal.testing-base")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testRuntimeOnly(mnLogging.logback.classic)

    implementation(projects.micronautTests.micronautCommonSync)
    implementation(projects.micronautSqlite)
    testImplementation(projects.micronautTests.micronautCommonTests)
    testImplementation(mnData.micronaut.data.tx.jdbc)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(mnTest.junit.platform.launcher)

    implementation(projects.micronautJdbcHikari)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("micronaut.test.resources.enabled", "false")
}
