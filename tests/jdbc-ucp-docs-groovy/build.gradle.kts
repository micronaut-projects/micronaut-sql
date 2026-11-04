plugins {
    `java-library`
    groovy
}
dependencies {
    testCompileOnly(mn.micronaut.inject.groovy)
    testImplementation(projects.micronautJdbcUcp)
    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(libs.managed.h2)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
