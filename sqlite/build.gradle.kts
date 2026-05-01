plugins {
    id("io.micronaut.build.internal.sql-module")
}
dependencies {
    implementation(mnData.micronaut.data.connection.jdbc)
    api(libs.managed.sqlite.jdbc)
}
micronautBuild {
    binaryCompatibility.enabledAfter("7.1.0")
}
