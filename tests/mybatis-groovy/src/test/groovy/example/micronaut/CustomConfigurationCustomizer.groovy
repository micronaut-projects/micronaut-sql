package example.micronaut

import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.apache.ibatis.session.Configuration

@Named("default")
@Singleton
class CustomConfigurationCustomizer implements MyBatisConfigurationCustomizer {
    @Override
    void customize(Configuration configuration) {
        configuration.addMappers("example.micronaut")
    }
}
