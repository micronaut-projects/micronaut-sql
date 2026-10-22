/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.jdbc.ucp;

import oracle.ucp.jdbc.PoolDataSource;

import java.sql.SQLException;

/**
 * Customizes a UCP {@link PoolDataSource} after it is configured and before the pool starts.
 *
 * <p>Use it for settings that configuration cannot express because they take objects, such as
 * {@link PoolDataSource#setTokenSupplier(java.util.function.Supplier)},
 * {@link PoolDataSource#registerConnectionCreationConsumer(java.util.function.Consumer)} and
 * {@link PoolDataSource#setConnectionLabelingCallback(oracle.ucp.ConnectionLabelingCallback)}.
 * Every customizer bean is applied to every UCP data source, ordered by
 * {@link io.micronaut.core.order.Ordered}; use the data source name to target one.</p>
 *
 * @author Graeme Rocher
 * @since 7.3.0
 */
@FunctionalInterface
public interface PoolDataSourceCustomizer {

    /**
     * Customizes the pool data source.
     *
     * @param dataSourceName The name of the data source, such as {@code default}
     * @param poolDataSource The pool data source, not yet started
     * @throws SQLException If a UCP setter fails
     */
    void customize(String dataSourceName, PoolDataSource poolDataSource) throws SQLException;
}
