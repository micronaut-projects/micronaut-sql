package example.controllers

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider

@MicronautTest
class H2AppSpec extends AbstractAppSpec implements TestPropertyProvider {

    @Override
    Map<String, String> getProperties() {
        return [
                "jpa.default.properties.hibernate.dialect"  : "org.hibernate.dialect.H2Dialect",
                "datasources.default.url"                   : "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE",
                "datasources.default.driverClassName"       : "org.h2.Driver",
                "datasources.default.username"              : "sa",
                "datasources.default.password"              : ""
        ]
    }

}

