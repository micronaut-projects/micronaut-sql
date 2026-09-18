package example.micronaut;

// tag::imports[]
import io.micronaut.configuration.mybatis.MyBatisConfigurationCustomizer;
import io.micronaut.configuration.mybatis.MyBatisMapperScan;
// end::imports[]

// tag::clazz[]
@MyBatisMapperScan("example.micronaut.mappers")
public interface CustomConfigurationCustomizer extends MyBatisConfigurationCustomizer {
}
// end::clazz[]
