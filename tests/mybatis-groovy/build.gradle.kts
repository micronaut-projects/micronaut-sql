plugins {
    `java-library`
    groovy
}
dependencies {
    testCompileOnly(mn.micronaut.inject.groovy)
    testCompileOnly(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testCompileOnly(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(projects.micronautMybatis)
    testImplementation(projects.micronautJdbcHikari)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.micronaut.test.spock)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
