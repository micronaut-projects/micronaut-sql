import org.gradle.kotlin.dsl.`java-library`

plugins {
    `java-library`
}
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    implementation(projects.testSuiteOjdbcExtensions)
    implementation(projects.micronautJdbcUcp)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.jupiter.api)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
