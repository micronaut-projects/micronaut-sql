import org.gradle.kotlin.dsl.`java-library`

plugins {
    `java-library`
    `java-test-fixtures`
}
dependencies {
    testFixturesAnnotationProcessor(mn.micronaut.inject.java)
    testFixturesApi(libs.managed.ojdbc11)
    testFixturesImplementation(platform(mnTest.boms.testcontainers))
    testFixturesImplementation(libs.testcontainers.oracle.free)
}
