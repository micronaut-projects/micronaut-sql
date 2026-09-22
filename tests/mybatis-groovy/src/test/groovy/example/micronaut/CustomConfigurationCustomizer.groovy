package example.micronaut

// tag::imports[]
import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer
import io.micronaut.configuration.mybatis.MyBatisMapperScan
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.apache.ibatis.session.Configuration
// end::imports[]

// tag::clazz[]
@MyBatisMapperScan("example.micronaut.mappers") // <1>
@Named("default") // <2>
@Singleton
class CustomConfigurationCustomizer implements MyBatisConfigurationCustomizer {
    @Override
    void customize(Configuration configuration) { // <3>
        configuration.mapUnderscoreToCamelCase = true
    }
}
// end::clazz[]
