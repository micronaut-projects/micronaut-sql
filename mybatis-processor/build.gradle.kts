plugins {
    id("io.micronaut.build.internal.sql-module")
}

dependencies {
    implementation(projects.micronautMybatis)
    implementation(mn.micronaut.core.processor)
    implementation(mn.micronaut.sourcegen.model)
    implementation(mn.micronaut.sourcegen.bytecode.writer)

    testImplementation(mn.micronaut.inject.java)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(mnTest.junit.platform.launcher)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

micronautBuild {
    binaryCompatibility {
        enabledAfter("7.2.0")
    }
}
