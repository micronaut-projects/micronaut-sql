package example.micronaut

import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.apache.ibatis.session.Configuration

@Named("default")
@Singleton
class CustomConfigurationCustomizer : MyBatisConfigurationCustomizer {
    override fun customize(configuration: Configuration) {
        configuration.addMappers("example.micronaut")
    }
}
