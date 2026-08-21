package example.micronaut

// tag::imports[]
import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer
import io.micronaut.configuration.mybatis.MyBatisMapperScan
import jakarta.inject.Named
// end::imports[]

// tag::clazz[]
@MyBatisMapperScan("example.micronaut.mappers")
@Named("default")
interface CustomConfigurationCustomizer extends MyBatisConfigurationCustomizer {
}
// end::clazz[]
