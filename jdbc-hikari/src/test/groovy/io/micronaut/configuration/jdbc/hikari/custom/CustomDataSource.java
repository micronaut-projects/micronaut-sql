package io.micronaut.configuration.jdbc.hikari.custom;

import com.zaxxer.hikari.HikariConfig;
import io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource;


public class CustomDataSource extends HikariUrlDataSource {

    public CustomDataSource(HikariConfig configuration) {
        super(configuration);
    }
}
