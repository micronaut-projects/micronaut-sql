plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
}
dependencies {
    kspTest(mn.micronaut.inject.kotlin)
    testImplementation(projects.micronautJdbcUcp)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
