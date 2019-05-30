package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;

@Requires(classes = SQLExceptionTranslator.class)
@EachBean(DataSource.class)
public class JooqExceptionTranslatorProvider implements ExecuteListenerProvider {

    private final JooqExceptionTranslator translator = new JooqExceptionTranslator();

    @Override
    public ExecuteListener provide() {
        return translator;
    }

}
