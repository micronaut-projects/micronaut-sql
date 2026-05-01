plugins {
    id("io.micronaut.build.internal.test-application")
}

dependencies {
    implementation(projects.micronautJdbcHikari)
    implementation(projects.micronautTests.micronautCommonSync)
    implementation(projects.micronautSqlite)
    testImplementation(projects.micronautTests.micronautCommonTests)
    testImplementation(mnData.micronaut.data.tx.jdbc)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("micronaut.test.resources.enabled", "false")
}
micronaut {
    testResources {
        enabled = false
    }
}
