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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.context.scope.refresh.RefreshEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract base class for datasource factories that listens for refresh events and updates datasource credentials accordingly.
 * <p>
 * This class provides a basic implementation for handling refresh events and updating datasource credentials.
 * Subclasses are expected to implement the {@link #dataSourceCredentialsChanged(String, DataSourceCredentials)} method to handle the updated credentials.
 */
public abstract class BaseDatasourceFactory implements RefreshEventListener {

    /**
     * A regular expression pattern used to match datasource password properties.
     */
    private static final Pattern DATASOURCE_PASSWORD_MATCHER = Pattern.compile(BasicJdbcConfiguration.PREFIX + "\\.(.*)\\.password");

    /**
     * A regular expression pattern used to match datasource username properties.
     */
    private static final Pattern DATASOURCE_USERNAME_MATCHER = Pattern.compile(BasicJdbcConfiguration.PREFIX + "\\.(.*)\\.username");

    @Override
    public @NonNull Set<String> getObservedConfigurationPrefixes() {
        return Set.of(BasicJdbcConfiguration.PREFIX);
    }

    @Override
    public void onApplicationEvent(RefreshEvent event) {
        Map<String, Object> changes = event.getSource();
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        Map<String, DataSourceCredentials> dataSourceCredentialsMap = new HashMap<>(2);
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            String property = change.getKey();
            Object value = change.getValue();
            Matcher userNameMatcher = DATASOURCE_USERNAME_MATCHER.matcher(property);
            if (userNameMatcher.matches()) {
                checkAndUpdateUsernameChange(value, userNameMatcher, dataSourceCredentialsMap);
            } else {
                Matcher passwordMatcher = DATASOURCE_PASSWORD_MATCHER.matcher(property);
                if (passwordMatcher.matches()) {
                    checkAndUpdatePasswordChange(value, passwordMatcher, dataSourceCredentialsMap);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dataSourceCredentialsMap)) {
            for (Map.Entry<String, DataSourceCredentials> dataSourceCredentialsEntry : dataSourceCredentialsMap.entrySet()) {
                dataSourceCredentialsChanged(dataSourceCredentialsEntry.getKey(), dataSourceCredentialsEntry.getValue());
            }
        }
    }

    /**
     * Called when the datasource credentials have changed.
     * <p>
     * Subclasses must implement this method to handle the updated credentials.
     *
     * @param dataSourceName      the name of the datasource
     * @param dataSourceCredentials the updated datasource credentials
     */
    protected abstract void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials);

    private void checkAndUpdateUsernameChange(Object value, Matcher userNameMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap) {
        String dataSourceName = userNameMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String userName;
            if (value instanceof byte[] bytes) {
                userName = new String(bytes);
            } else {
                userName = value.toString();
            }
            if (StringUtils.isEmpty(userName)) {
                // username may not be empty while password can
                throw new IllegalStateException("Datasource [" + dataSourceName + "] username is change to empty.");
            }
            DataSourceCredentials dataSourceCredentials = dataSourceCredentialsMap.get(dataSourceName);
            dataSourceCredentialsMap.put(dataSourceName, dataSourceCredentials == null ? new DataSourceCredentials(userName, null) : dataSourceCredentials.withUserName(userName));
        }
    }

    private void checkAndUpdatePasswordChange(Object value, Matcher passwordMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap) {
        String dataSourceName = passwordMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String password;
            if (value instanceof byte[] bytes) {
                password = new String(bytes);
            } else {
                password = value.toString();
            }
            DataSourceCredentials dataSourceCredentials = dataSourceCredentialsMap.get(dataSourceName);
            dataSourceCredentialsMap.put(dataSourceName, dataSourceCredentials == null ? new DataSourceCredentials(null, password) : dataSourceCredentials.withPassword(password));
        }
    }

    /**
     * A record representing datasource credentials.
     * <p>
     * This record contains the username and password for a datasource.
     *
     * @param userName the username (may be null)
     * @param password the password (may be null)
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

    }
}
