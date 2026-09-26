from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.apache.ibatis.session import SqlSessionFactory
from org.junit.jupiter.api import Test


@MicronautTest
class CustomConfigurationCustomizerTest:
    sql_session_factory: Annotated[SqlSessionFactory, Inject]

    @Test
    def test_customizer_is_applied(self):
        configuration = self.sql_session_factory.getConfiguration()
        assert configuration.isMapUnderscoreToCamelCase()
