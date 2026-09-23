package example.micronaut;

// tag::imports[]
import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer;
import io.micronaut.configuration.mybatis.MyBatisMapperScan;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.ibatis.session.Configuration;
// end::imports[]

// tag::clazz[]
@MyBatisMapperScan("example.micronaut.mappers")
@Named("default")
@Singleton
public class CustomConfigurationCustomizer implements MyBatisConfigurationCustomizer {
    @Override
    public void customize(Configuration configuration) {
        configuration.setMapUnderscoreToCamelCase(true);
    }
}
// end::clazz[]
