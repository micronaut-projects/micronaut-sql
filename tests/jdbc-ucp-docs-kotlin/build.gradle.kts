plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
}
dependencies {
    kspTest(mn.micronaut.inject.kotlin)
    testImplementation(projects.micronautJdbcUcp)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.platform.launcher)
    testRuntimeOnly(libs.managed.h2)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}
