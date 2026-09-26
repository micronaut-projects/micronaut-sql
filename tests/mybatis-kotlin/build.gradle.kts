plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
}
dependencies {
    kspTest(mn.micronaut.inject.kotlin)
    kspTest(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    kspTest(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(projects.micronautMybatis)
    testImplementation(projects.micronautJdbcHikari)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
