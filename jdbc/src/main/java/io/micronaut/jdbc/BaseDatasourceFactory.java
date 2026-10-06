/*
 * Copyright 2017-2025 original authors
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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

/**
 * An abstract base class for datasource factories whose pools follow changes of the datasource credentials.
 * <p>
 * The {@link DataSourceCredentialsRefresher} listens for refresh events, compares the configured credentials of
 * each affected datasource with the last applied ones, and calls {@link #dataSourceCredentialsChanged(String, DataSourceCredentials)}
 * of each datasource factory with the credentials that actually changed.
 * <p>
 * The factory holds neither the application context nor its environment, so a factory and the pools it created can
 * outlive the context that created them, as development mode does when it retains the pools across a restart.
 *
 * @since 6.2.0
 */
@Internal
public abstract class BaseDatasourceFactory {

    /**
     * Called when the datasource credentials have changed.
     * <p>
     * Subclasses must implement this method to handle the updated credentials. If the pool can not be updated,
     * the method must throw, so the change is not recorded as applied and a later refresh event retries it.
     *
     * @param dataSourceName      the name of the datasource
     * @param dataSourceCredentials the updated datasource credentials
     * @throws SQLException if the pool could not be updated
     */
    protected abstract void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) throws SQLException;

    /**
     * A record representing datasource credentials.
     * <p>
     * This record contains the username and password for a datasource.
     *
     * @param userName the username (can be null)
     * @param password the password (can be null)
     */
    protected record DataSourceCredentials(@Nullable String userName, @Nullable String password) {

        /**
         * Returns a new {@link DataSourceCredentials} instance with the given username.
         *
         * @param newUserName the new username
         * @return a new {@link DataSourceCredentials} instance
         */
        public DataSourceCredentials withUserName(String newUserName) {
            return new DataSourceCredentials(newUserName, password);
        }

        /**
         * Returns a new {@link DataSourceCredentials} instance with the given password.
         *
         * @param newPassword the new password
         * @return a new {@link DataSourceCredentials} instance
         */
        public DataSourceCredentials withPassword(String newPassword) {
            return new DataSourceCredentials(userName, newPassword);
        }

        /**
         * @return the credentials with the password masked, so logging them does not expose it
         */
        @Override
        public String toString() {
            return "DataSourceCredentials[userName=" + userName + ", password=" + (password == null ? null : "*****") + "]";
        }

        /**
         * @return The change type in datasource credentials
         */
        public ChangeType getChangeType() {
            if (userName != null && password != null) {
                return ChangeType.USERNAME_AND_PASSWORD;
            } else if (userName != null) {
                return ChangeType.USERNAME;
            } else if (password != null) {
                return ChangeType.PASSWORD;
            } else {
                return ChangeType.NONE;
            }
        }

        enum ChangeType {
            USERNAME,
            PASSWORD,
            USERNAME_AND_PASSWORD,
            NONE
        }
    }
}
