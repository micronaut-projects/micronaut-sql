/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;

/**
 * Allows {@link SQLExceptionTranslator} to be used with JOOQ.
 *
 * @author Vladimir Kulev
 * @since 1.2.0
 */
@Requires(classes = SQLExceptionTranslator.class)
@EachBean(DataSource.class)
public class JooqExceptionTranslatorProvider implements ExecuteListenerProvider {

    private final JooqExceptionTranslator translator = new JooqExceptionTranslator();

    @Override
    public ExecuteListener provide() {
        return translator;
    }

}
