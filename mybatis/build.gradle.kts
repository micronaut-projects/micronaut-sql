plugins {
    id("io.micronaut.build.internal.sql-module")
}
dependencies {
    api(projects.micronautJdbc)
    api(mn.micronaut.aop)
    api(mn.micronaut.context)
    api(mn.micronaut.inject)
    api(libs.managed.mybatis)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(projects.micronautJdbcHikari)
    testRuntimeOnly(libs.managed.h2)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

micronautBuild {
    binaryCompatibility {
        enabledAfter("7.1.0")
    }
}
