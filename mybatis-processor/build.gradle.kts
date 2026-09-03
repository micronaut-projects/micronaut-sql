plugins {
    id("io.micronaut.build.internal.sql-module")
}

dependencies {
    api(projects.micronautMybatis)
    api(mn.micronaut.sourcegen.annotations)
    api(mn.micronaut.sourcegen.generator.bytecode)

    implementation(mn.micronaut.core.processor)

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
