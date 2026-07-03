import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.sql-module")
}
dependencies {
    implementation(mnData.micronaut.data.connection.jdbc)
    api(libs.managed.sqlite.jdbc)
    testImplementation(mnTest.junit.jupiter.params)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
    binaryCompatibility.enabledAfter("7.1.0")
}
