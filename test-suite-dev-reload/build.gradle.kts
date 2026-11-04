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
                // the pool reports to the meter registry of the current generation
                implementation(mnMicrometer.micronaut.micrometer.core)
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
        // Hibernate builds its session factory on the retained Hikari pool
        register<JvmTestSuite>("hibernateTest") {
            dependencies {
                implementation(projects.micronautHibernateJpa)
                implementation(mnData.micronaut.data.tx.hibernate) {
                    exclude(group = "org.hibernate.orm")
                }
                implementation(projects.micronautJdbcHikari)
                // the schema warning is asserted on
                implementation(mnLogging.logback.classic)
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
    dependsOn(testing.suites.named("ucpTest"), testing.suites.named("dbcpTest"), testing.suites.named("hibernateTest"))
}
