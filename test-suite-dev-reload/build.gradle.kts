plugins {
    `java-library`
    `jvm-test-suite`
    id("io.micronaut.build.internal.sql-base")
}

// each pool is tested in a suite of its own: the application under test sees the test classpath, which must hold one
// pool implementation
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            dependencies {
                implementation(projects.micronautJdbcHikari)
            }
        }
        register<JvmTestSuite>("ucpTest") {
            dependencies {
                implementation(projects.micronautJdbcUcp)
            }
        }
        register<JvmTestSuite>("dbcpTest") {
            dependencies {
                implementation(projects.micronautJdbcDbcp)
            }
        }
        withType<JvmTestSuite>().configureEach {
            targets.configureEach {
                testTask.configure {
                    useJUnitPlatform()
                }
            }
            dependencies {
                implementation(platform(mn.micronaut.core.bom))
                implementation(mn.micronaut.dev.tck)
                // the reload harness compiles the application under test with the processors on the test classpath
                implementation(mn.micronaut.inject.java)

                implementation(mnTest.junit.jupiter.api)
                runtimeOnly(mnTest.junit.jupiter.engine)
                runtimeOnly(mnTest.junit.platform.launcher)
                runtimeOnly(libs.managed.h2)
                runtimeOnly(mnLogging.logback.classic)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("ucpTest"), testing.suites.named("dbcpTest"))
}
