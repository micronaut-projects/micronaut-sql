plugins {
    id("io.micronaut.build.internal.sql-test-suite")
    id("io.micronaut.build.internal.kotlin-ksp")
}

dependencies {
    kspTest(mn.micronaut.inject.kotlin)
    // maps jakarta.transaction.Transactional to Micronaut's transactional advice
    kspTest(mnData.micronaut.data.processor)
    testImplementation(mnTest.micronaut.test.junit5)

    // the hibernate examples of the guide
    testImplementation(projects.micronautHibernateJpa)
    testImplementation(mnData.micronaut.data.tx.hibernate) {
        exclude(group = "org.hibernate.orm")
    }
}
