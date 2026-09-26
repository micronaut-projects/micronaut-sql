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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.context.scope.refresh.RefreshEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract base class for datasource factories that listens for refresh events and updates datasource credentials accordingly.
 * <p>
 * This class provides a basic implementation for handling refresh events and updating datasource credentials.
 * Subclasses are expected to implement the {@link #dataSourceCredentialsChanged(String, DataSourceCredentials)} method to handle the updated credentials.
 * <p>
 * A refresh event without specific keys (for example {@code new RefreshEvent()}) is handled by comparing the configured
 * credentials of each datasource with the last known ones, so only credentials that actually changed are propagated.
 *
 * @since 6.2.0
 */
@Internal
public abstract class BaseDatasourceFactory implements RefreshEventListener {

    /**
     * A regular expression pattern used to match datasource password properties.
     */
    private static final Pattern DATASOURCE_PASSWORD_MATCHER = Pattern.compile(BasicJdbcConfiguration.PREFIX + "\\.(.*)\\.password");

    /**
     * A regular expression pattern used to match datasource username properties.
     */
    private static final Pattern DATASOURCE_USERNAME_MATCHER = Pattern.compile(BasicJdbcConfiguration.PREFIX + "\\.(.*)\\.username");

    private static final Logger LOG = LoggerFactory.getLogger(BaseDatasourceFactory.class);

    private static final DataSourceCredentials NO_CREDENTIALS = new DataSourceCredentials(null, null);

    protected final ApplicationContext applicationContext;

    /**
     * The last known configured credentials per datasource, used to detect changes on a full refresh.
     */
    private final Map<String, DataSourceCredentials> configuredCredentials = new ConcurrentHashMap<>(2);

    protected BaseDatasourceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        for (String dataSourceName : getConfiguredDataSourceNames()) {
            configuredCredentials.put(dataSourceName, readConfiguredCredentials(dataSourceName));
        }
    }

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
        if (isFullRefresh(changes)) {
            onFullRefresh();
            return;
        }
        Map<String, DataSourceCredentials> dataSourceCredentialsMap = new HashMap<>(2);
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            String property = change.getKey();
            // value in change set is an old value, and we want to get new from the application context
            Matcher userNameMatcher = DATASOURCE_USERNAME_MATCHER.matcher(property);
            if (userNameMatcher.matches()) {
                checkAndUpdateUsernameChange(property, userNameMatcher, dataSourceCredentialsMap);
            } else {
                Matcher passwordMatcher = DATASOURCE_PASSWORD_MATCHER.matcher(property);
                if (passwordMatcher.matches()) {
                    checkAndUpdatePasswordChange(property, passwordMatcher, dataSourceCredentialsMap);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dataSourceCredentialsMap)) {
            for (Map.Entry<String, DataSourceCredentials> dataSourceCredentialsEntry : dataSourceCredentialsMap.entrySet()) {
                String datasourceName = dataSourceCredentialsEntry.getKey();
                notifyCredentialsChanged(datasourceName, dataSourceCredentialsEntry.getValue());
                configuredCredentials.put(datasourceName, readConfiguredCredentials(datasourceName));
            }
        }
    }

    /**
     * Handles a refresh event without specific keys, such as {@code new RefreshEvent()}, by comparing
     * the configured credentials of each datasource with the last known ones.
     */
    private void onFullRefresh() {
        for (String dataSourceName : getConfiguredDataSourceNames()) {
            DataSourceCredentials previous = configuredCredentials.getOrDefault(dataSourceName, NO_CREDENTIALS);
            DataSourceCredentials current = readConfiguredCredentials(dataSourceName);
            configuredCredentials.put(dataSourceName, current);
            String userName = changedValue(previous.userName(), current.userName());
            if (userName != null && userName.isEmpty()) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Datasource [{}] username is changed to empty or could not be read. Ignoring the username change.", dataSourceName);
                }
                userName = null;
            }
            String password = changedValue(previous.password(), current.password());
            if (userName != null || password != null) {
                notifyCredentialsChanged(dataSourceName, new DataSourceCredentials(userName, password));
            }
        }
    }

    private void notifyCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource [{}] credentials changed [{}]. Trying to refresh connection pool.", dataSourceName,
                dataSourceCredentials.getChangeType());
        }
        try {
            dataSourceCredentialsChanged(dataSourceName, dataSourceCredentials);
        } catch (Exception e) {
            // do not fail the refresh event publisher or prevent other datasources from being updated
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to update credentials for datasource [{}]", dataSourceName, e);
            }
        }
    }

    private Collection<String> getConfiguredDataSourceNames() {
        Environment environment = applicationContext.getEnvironment();
        if (environment == null) {
            return Collections.emptyList();
        }
        return environment.getPropertyEntries(BasicJdbcConfiguration.PREFIX);
    }

    private DataSourceCredentials readConfiguredCredentials(String dataSourceName) {
        String prefix = BasicJdbcConfiguration.PREFIX + "." + dataSourceName + ".";
        return new DataSourceCredentials(readProperty(prefix + "username"), readProperty(prefix + "password"));
    }

    private @Nullable String readProperty(String property) {
        try {
            return applicationContext.getProperty(property, String.class).orElse(null);
        } catch (Exception e) {
            // for example an unresolvable placeholder, which the datasource configuration itself will report
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to read property [{}]: {}", property, e.getMessage());
            }
            return null;
        }
    }

    /**
     * @return the current value if it differs from the previous one, otherwise {@code null}. A removed value is not a change.
     */
    private static @Nullable String changedValue(@Nullable String previous, @Nullable String current) {
        return Objects.equals(previous, current) ? null : current;
    }

    private static boolean isFullRefresh(Map<String, Object> changes) {
        // RefreshEvent() uses a singleton map of "all" -> "*" as its source
        return changes.size() == 1 && "*".equals(changes.get("all"));
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

    private void checkAndUpdateUsernameChange(String property, Matcher userNameMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap) {
        String dataSourceName = userNameMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String userName = readProperty(property);
            if (StringUtils.isEmpty(userName)) {
                // username may not be empty while password can
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Datasource [{}] username is changed to empty or could not be read. Ignoring the username change.", dataSourceName);
                }
                return;
            }
            DataSourceCredentials dataSourceCredentials = dataSourceCredentialsMap.get(dataSourceName);
            dataSourceCredentialsMap.put(dataSourceName, dataSourceCredentials == null ? new DataSourceCredentials(userName, null) : dataSourceCredentials.withUserName(userName));
        }
    }

    private void checkAndUpdatePasswordChange(String property, Matcher passwordMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap) {
        String dataSourceName = passwordMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String password = readProperty(property);
            if (password == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Datasource [{}] password is removed or could not be read. Ignoring the password change.", dataSourceName);
                }
                return;
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
