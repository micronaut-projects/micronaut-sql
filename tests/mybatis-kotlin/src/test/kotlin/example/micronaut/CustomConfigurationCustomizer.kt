package example.micronaut

// tag::imports[]
import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer
import io.micronaut.configuration.mybatis.MyBatisMapperScan
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.apache.ibatis.session.Configuration
// end::imports[]

// tag::clazz[]
@MyBatisMapperScan("example.micronaut.mappers")
@Named("default")
@Singleton
class CustomConfigurationCustomizer : MyBatisConfigurationCustomizer {
    override fun customize(configuration: Configuration) {
        configuration.isMapUnderscoreToCamelCase = true
    }
}
// end::clazz[]
