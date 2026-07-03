plugins {
    `java-library`
}
dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    implementation(projects.micronautMybatis)
    implementation(projects.micronautJdbcHikari)
    testAnnotationProcessor(mn.micronaut.inject.java)
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
