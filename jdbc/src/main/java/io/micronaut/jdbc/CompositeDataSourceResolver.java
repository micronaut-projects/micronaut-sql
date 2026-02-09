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
package io.micronaut.jdbc;

import io.micronaut.context.annotation.Primary;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A composite implementation combining all registered {@link DataSourceResolver} instances.
 *
 * @author Andreas Brenk
 * @since 7.0.0
 */
@Singleton
@Primary
public class CompositeDataSourceResolver implements DataSourceResolver {

    private final DataSourceResolver[] dataSourceResolvers;

    /**
     * Construct the CompositeDataSourceResolver from all data source resolvers.
     *
     * @param dataSourceResolvers The data source resolvers
     */
    public CompositeDataSourceResolver(DataSourceResolver[] dataSourceResolvers) {
        this.dataSourceResolvers = dataSourceResolvers;
    }

    /**
     * The underlying resolvers.
     *
     * @return The resolvers
     */
    public DataSourceResolver[] getDataSourceResolvers() {
        return dataSourceResolvers;
    }

    /**
     * Resolves the underlying target data source by iteratively unwrapping all proxying or instrumentation logic
     * using the registered {@link DataSourceResolver} instances. Continues resolving until no further unwrapping is possible.
     */
    @Override
    public DataSource resolve(DataSource dataSource) {
        DataSource resolved = dataSource;

        do {
            dataSource = resolved;
            for (DataSourceResolver resolver : dataSourceResolvers) {
                resolved = resolver.resolve(dataSource);
                if (resolved != dataSource) {
                    break; // One layer was unwrapped, run the outer while loop from the start.
                }
            }
        } while (resolved != dataSource); // If no resolver returned a different data source, we are done.

        return resolved;
    }

    @Override
    public String toString() {
        return "CompositeDataSourceResolver(" + Arrays.stream(dataSourceResolvers).map(DataSourceResolver::toString).collect(Collectors.joining(",")) + ")";
    }
}
