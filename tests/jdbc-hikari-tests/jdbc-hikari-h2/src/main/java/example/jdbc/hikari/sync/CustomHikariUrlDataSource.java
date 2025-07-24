package example.jdbc.hikari.sync;

import com.zaxxer.hikari.HikariConfig;
import io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource;

public class CustomHikariUrlDataSource extends HikariUrlDataSource {
    public CustomHikariUrlDataSource(HikariConfig configuration) {
        super(configuration);
    }
}
