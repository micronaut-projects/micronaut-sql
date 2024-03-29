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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

/**
 * Configuration of {@link oracle.ucp.admin.UniversalConnectionPoolManager}.
 *
 * @author Pavol Gressa
 * @since 4.1
 */
@ConfigurationProperties(UniversalConnectionPoolManagerConfiguration.PREFIX)
public interface UniversalConnectionPoolManagerConfiguration {

    String PREFIX = "ucp-manager";

    /**
     * Enables {@link oracle.ucp.admin.UniversalConnectionPoolManager}.
     * @return flag to enable UCP manager. Defaults to <code>true</code>.
     */
    @Bindable(defaultValue = "true")
    boolean isEnabled();

    /**
     * MX Bean configuration.
     */
    @ConfigurationProperties(JMXBeanConfiguration.PREFIX)
    interface JMXBeanConfiguration {

        String PREFIX = "jmx";

        /**
         * Enables the JMX-Based Management of UCP.
         *
         * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/21/jjucp/jmx-based-management.html">Overview of JMX-Based Management in UCP</a>
         * @return flag to enable JMX bean. Defaults to <code>false</code>.
         */
        @Bindable(defaultValue = "false")
        boolean isEnabled();
    }
}
