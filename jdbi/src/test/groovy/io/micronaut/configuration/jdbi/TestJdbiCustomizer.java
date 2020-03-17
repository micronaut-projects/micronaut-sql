package io.micronaut.configuration.jdbi;

import org.jdbi.v3.core.Jdbi;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("default")
public class TestJdbiCustomizer implements JdbiCustomizer {
    @Override
    public void customize(Jdbi jdbi) {
        jdbi.define("test", "test");
    }
}
