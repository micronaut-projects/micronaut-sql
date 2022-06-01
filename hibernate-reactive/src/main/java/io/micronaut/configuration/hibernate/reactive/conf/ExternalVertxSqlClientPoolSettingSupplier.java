/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate.reactive.conf;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.vertx.sqlclient.Pool;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.impl.SqlClientPool;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.util.impl.CompletionStages;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Requires(bean = Pool.class)
@Prototype
final class ExternalVertxSqlClientPoolSettingSupplier implements SettingsSupplier {

    private final Pool pool;

    ExternalVertxSqlClientPoolSettingSupplier(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        return Collections.singletonMap(Settings.SQL_CLIENT_POOL, new ExternalSqlClientPool(pool));
    }

    private static final class ExternalSqlClientPool extends SqlClientPool implements ServiceRegistryAwareService {

        private final transient Pool pool;
        private transient SqlStatementLogger sqlStatementLogger;

        private ExternalSqlClientPool(Pool pool) {
            this.pool = pool;
        }

        @Override
        protected Pool getPool() {
            return pool;
        }

        @Override
        protected SqlStatementLogger getSqlStatementLogger() {
            return sqlStatementLogger;
        }

        @Override
        public CompletionStage<Void> getCloseFuture() {
            return CompletionStages.voidFuture();
        }

        @Override
        public void injectServices(ServiceRegistryImplementor serviceRegistry) {
            sqlStatementLogger = serviceRegistry.getService(JdbcServices.class).getSqlStatementLogger();
        }
    }

}
