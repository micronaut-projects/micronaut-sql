plugins {
    id("io.micronaut.build.internal.test-application")
}
dependencies {
    implementation(projects.micronautTests.micronautCommon)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautMybatisProcessor)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testAnnotationProcessor(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(projects.micronautMybatis)
    testImplementation(projects.micronautJdbcHikari)
    testRuntimeOnly(libs.managed.h2)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}
tasks.named<Test>("test") {
    useJUnitPlatform()
}

val isMacOsArm = System.getProperty("os.name") == "Mac OS X" &&
    System.getProperty("os.arch") in setOf("aarch64", "arm64")

if (isMacOsArm) {
    graalvmNative {
        binaries {
            all {
                quickBuild.set(true)
            }
        }
    }
}
