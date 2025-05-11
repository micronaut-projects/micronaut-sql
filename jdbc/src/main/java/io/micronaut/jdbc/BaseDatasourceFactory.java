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
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.context.scope.refresh.RefreshEventListener;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract base class for creating datasource factories that listen for refresh events and update
 * datasource passwords accordingly.
 * <p>
 * This class implements the {@link RefreshEventListener} interface and provides a basic implementation
 * for handling refresh events. Subclasses are required to implement the
 * {@link #dataSourcePasswordChanged(String, String)} method to handle password changes.
 */
public abstract class BaseDatasourceFactory implements RefreshEventListener {

    /**
     * A regular expression pattern used to match datasource password properties.
     */
    private static final Pattern DATASOURCE_PASSWORD_MATCHER = Pattern.compile(BasicJdbcConfiguration.PREFIX + "\\.(.*)\\.password");

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
        for (String property : changes.keySet()) {
            Matcher matcher = DATASOURCE_PASSWORD_MATCHER.matcher(property);
            if (matcher.matches()) {
                String dataSourceName = matcher.group(1);
                if (StringUtils.isNotEmpty(dataSourceName)) {
                    Object password = changes.get(property);
                    if (password instanceof byte[] bytes) {
                        dataSourcePasswordChanged(dataSourceName, new String(bytes));
                    } else {
                        dataSourcePasswordChanged(dataSourceName, password.toString());
                    }
                }
            }
        }
    }

    /**
     * Called when a datasource password has changed.
     * <p>
     * Subclasses must implement this method to handle the password change.
     *
     * @param dataSourceName the name of the datasource whose password has changed
     * @param password       the new password
     */
    protected abstract void dataSourcePasswordChanged(String dataSourceName, String password);
}
