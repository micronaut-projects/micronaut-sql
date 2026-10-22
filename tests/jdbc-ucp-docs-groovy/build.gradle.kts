plugins {
    `java-library`
    groovy
}
dependencies {
    testCompileOnly(mn.micronaut.inject.groovy)
    testImplementation(projects.micronautJdbcUcp)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mnTest.micronaut.test.spock)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
