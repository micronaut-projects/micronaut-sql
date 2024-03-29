/*
 * Copyright 2017-2021 original authors
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

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;

/**
 * Factory for {@link UniversalConnectionPoolManager}.
 *
 * @author Pavol Gressa
 * @since 4.1
 */
@Requires(property = "ucp-manager.enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Internal
@Factory
public class UniversalConnectionPoolManagerFactory {

    /**
     * Creates the {@link UniversalConnectionPoolManager}.
     *
     * @param jmxBeanConfiguration configuration
     * @return ucp manager
     * @throws UniversalConnectionPoolException when the manager can't be configured.
     */
    @Singleton
    public UniversalConnectionPoolManager connectionPoolManager(UniversalConnectionPoolManagerConfiguration.JMXBeanConfiguration jmxBeanConfiguration) throws UniversalConnectionPoolException {
        UniversalConnectionPoolManager connectionPoolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
        connectionPoolManager.setJmxEnabled(jmxBeanConfiguration.isEnabled());
        return connectionPoolManager;
    }
}
