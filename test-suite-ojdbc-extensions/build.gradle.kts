import org.gradle.kotlin.dsl.`java-library`

plugins {
    `java-library`
}
dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    implementation(libs.managed.ojdbc11)
    implementation(platform(mnTest.boms.testcontainers))
    implementation(libs.testcontainers.oracle.free)
}
