plugins {
    `java-library`
    id("io.micronaut.build.internal.python")
}
dependencies {
    // Annotation processors MUST be testImplementation (not testAnnotationProcessor): the Python compiler
    // compiler takes the compile classpath as its annotation processor path.
    testImplementation(mn.micronaut.inject.python.test)
    testImplementation(mn.micronaut.context.python)
    testImplementation(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(projects.micronautMybatis)
    testImplementation(projects.micronautJdbcHikari)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnLogging.logback.classic)
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("micronaut.python.pool.enabled", "false")
}
