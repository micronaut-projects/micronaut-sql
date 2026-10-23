plugins {
    `java-library`
}
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(projects.micronautJdbcUcp)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
