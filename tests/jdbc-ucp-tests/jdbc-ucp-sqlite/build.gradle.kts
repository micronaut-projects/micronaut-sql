plugins {
    id("io.micronaut.build.internal.test-application")
}

dependencies {
    implementation(projects.micronautJdbcUcp)
    implementation(projects.micronautTests.micronautCommonSync)
    testRuntimeOnly(projects.micronautSqlite)
    testRuntimeOnly(mnLogging.logback.classic)
    testImplementation(projects.micronautTests.micronautCommonTests)
    testImplementation(mnData.micronaut.data.tx.jdbc)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("micronaut.test.resources.enabled", "false")
}

configurations.all {
    resolutionStrategy.preferProjectModules()
}

