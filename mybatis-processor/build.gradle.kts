plugins {
    id("io.micronaut.build.internal.sql-module")
}

dependencies {
    implementation(mn.micronaut.core.processor)
    api(projects.micronautMybatis)
    testImplementation(mn.micronaut.inject.java)
    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testImplementation(mnTest.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

micronautBuild {
    binaryCompatibility {
        enabledAfter("7.2.0")
    }
}
