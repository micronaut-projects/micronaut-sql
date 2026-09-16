# tag::imports[]
from jakarta.inject import Named, Singleton
from micronaut.configuration.mybatis import MyBatisConfigurationCustomizer
from org.apache.ibatis.session import Configuration
# end::imports[]


# tag::clazz[]
@Named("default")
@Singleton
class CustomConfigurationCustomizer(MyBatisConfigurationCustomizer):
    def customize(self, configuration: Configuration) -> None:
        configuration.addMappers("example.micronaut.mappers")
        configuration.setMapUnderscoreToCamelCase(True)
# end::clazz[]
