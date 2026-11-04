plugins {
    `java-library`
}
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(projects.micronautJdbcUcp)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(libs.managed.h2)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
