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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final CredentialsFingerprint NO_CREDENTIALS = new CredentialsFingerprint(null, null);

    /**
     * The source of {@code new RefreshEvent()}, which Micronaut compares by identity to detect a full refresh.
     */
    private static final Map<String, Object> ALL_KEYS = new RefreshEvent().getSource();

    protected final ApplicationContext applicationContext;

    /**
     * The last known configured credentials per datasource, used to detect changes on a full refresh.
     * Passwords are kept only as salted digests.
     */
    private final Map<String, CredentialsFingerprint> knownCredentials = new ConcurrentHashMap<>(2);

    private final byte[] passwordDigestSalt = new byte[16];

    /**
     * Refresh events are handled one at a time, so the same change is not applied twice by concurrent events.
     */
    private final Object refreshLock = new Object();

    protected BaseDatasourceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        new SecureRandom().nextBytes(passwordDigestSalt);
        for (String dataSourceName : getConfiguredDataSourceNames()) {
            knownCredentials.put(dataSourceName, fingerprint(readConfiguredCredentials(dataSourceName)));
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
        synchronized (refreshLock) {
            if (isFullRefresh(changes)) {
                onFullRefresh();
            } else {
                onRefresh(changes);
            }
        }
    }

    /**
     * Handles a refresh event with the changed keys.
     *
     * @param changes The changed keys
     */
    private void onRefresh(Map<String, Object> changes) {
        Map<String, DataSourceCredentials> dataSourceCredentialsMap = new HashMap<>(2);
        Set<String> invalidDataSources = new HashSet<>(2);
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            String property = change.getKey();
            // value in change set is an old value, and we want to get new from the application context
            Matcher userNameMatcher = DATASOURCE_USERNAME_MATCHER.matcher(property);
            if (userNameMatcher.matches()) {
                checkAndUpdateUsernameChange(property, userNameMatcher, dataSourceCredentialsMap, invalidDataSources);
            } else {
                Matcher passwordMatcher = DATASOURCE_PASSWORD_MATCHER.matcher(property);
                if (passwordMatcher.matches()) {
                    checkAndUpdatePasswordChange(property, passwordMatcher, dataSourceCredentialsMap, invalidDataSources);
                }
            }
        }
        for (Map.Entry<String, DataSourceCredentials> dataSourceCredentialsEntry : dataSourceCredentialsMap.entrySet()) {
            String datasourceName = dataSourceCredentialsEntry.getKey();
            if (invalidDataSources.contains(datasourceName)) {
                // never combine a new username with an old password or the other way round
                continue;
            }
            if (notifyCredentialsChanged(datasourceName, dataSourceCredentialsEntry.getValue())) {
                knownCredentials.put(datasourceName, fingerprint(readConfiguredCredentials(datasourceName)));
            }
        }
    }

    /**
     * Handles a refresh event without specific keys, such as {@code new RefreshEvent()}, by comparing
     * the configured credentials of each datasource with the last known ones.
     */
    private void onFullRefresh() {
        for (String dataSourceName : getConfiguredDataSourceNames()) {
            CredentialsFingerprint previous = knownCredentials.getOrDefault(dataSourceName, NO_CREDENTIALS);
            DataSourceCredentials current = readConfiguredCredentials(dataSourceName);
            CredentialsFingerprint currentFingerprint = fingerprint(current);
            boolean userNameChanged = !Objects.equals(previous.userName(), currentFingerprint.userName());
            boolean passwordChanged = !Objects.equals(previous.passwordDigest(), currentFingerprint.passwordDigest());
            if (!userNameChanged && !passwordChanged) {
                continue;
            }
            // never combine a new username with an old password or the other way round, the last known
            // credentials are kept so the change is applied once the configuration is fixed
            if (userNameChanged && StringUtils.isEmpty(current.userName())) {
                warnIgnoredChange(dataSourceName, "username is changed to empty or could not be read");
                continue;
            }
            if (passwordChanged && current.password() == null) {
                warnIgnoredChange(dataSourceName, "password is removed or could not be read");
                continue;
            }
            DataSourceCredentials changedCredentials = new DataSourceCredentials(
                userNameChanged ? current.userName() : null,
                passwordChanged ? current.password() : null
            );
            if (notifyCredentialsChanged(dataSourceName, changedCredentials)) {
                knownCredentials.put(dataSourceName, currentFingerprint);
            }
        }
    }

    /**
     * @return whether the credentials were updated, {@code false} if the update failed
     */
    private boolean notifyCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource [{}] credentials changed [{}]. Trying to refresh connection pool.", dataSourceName,
                dataSourceCredentials.getChangeType());
        }
        try {
            dataSourceCredentialsChanged(dataSourceName, dataSourceCredentials);
            return true;
        } catch (Exception e) {
            // do not fail the refresh event publisher or prevent other datasources from being updated
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to update credentials for datasource [{}]", dataSourceName, e);
            }
            return false;
        }
    }

    private static void warnIgnoredChange(String dataSourceName, String reason) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Datasource [{}] {}. Ignoring the credentials change of this datasource.", dataSourceName, reason);
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

    private CredentialsFingerprint fingerprint(DataSourceCredentials credentials) {
        return new CredentialsFingerprint(credentials.userName(), digest(credentials.password()));
    }

    private @Nullable String digest(@Nullable String password) {
        if (password == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(passwordDigestSalt);
            return Base64.getEncoder().encodeToString(messageDigest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required to be supported by every Java platform
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static boolean isFullRefresh(Map<String, Object> changes) {
        // same check as the refresh scope does
        return changes == ALL_KEYS;
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

    private void checkAndUpdateUsernameChange(String property, Matcher userNameMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap,
                                              Set<String> invalidDataSources) {
        String dataSourceName = userNameMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String userName = readProperty(property);
            if (StringUtils.isEmpty(userName)) {
                // username may not be empty while password can
                warnIgnoredChange(dataSourceName, "username is changed to empty or could not be read");
                invalidDataSources.add(dataSourceName);
                return;
            }
            DataSourceCredentials dataSourceCredentials = dataSourceCredentialsMap.get(dataSourceName);
            dataSourceCredentialsMap.put(dataSourceName, dataSourceCredentials == null ? new DataSourceCredentials(userName, null) : dataSourceCredentials.withUserName(userName));
        }
    }

    private void checkAndUpdatePasswordChange(String property, Matcher passwordMatcher, Map<String, DataSourceCredentials> dataSourceCredentialsMap,
                                              Set<String> invalidDataSources) {
        String dataSourceName = passwordMatcher.group(1);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            String password = readProperty(property);
            if (password == null) {
                warnIgnoredChange(dataSourceName, "password is removed or could not be read");
                invalidDataSources.add(dataSourceName);
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

    /**
     * The last known credentials of a datasource.
     *
     * @param userName The username
     * @param passwordDigest The salted digest of the password
     */
    private record CredentialsFingerprint(@Nullable String userName, @Nullable String passwordDigest) {
    }
}
