plugins {
    `java-library`
    id("io.micronaut.build.internal.python")
}
dependencies {
    // Annotation processors MUST be testImplementation (not testAnnotationProcessor): the Python compiler
    // takes the compile classpath as its annotation processor path.
    testImplementation(mn.micronaut.inject.python.test)
    testImplementation(mn.micronaut.context.python)
    testImplementation(projects.micronautJdbcUcp)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnLogging.logback.classic)
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("micronaut.python.pool.enabled", "false")
}
