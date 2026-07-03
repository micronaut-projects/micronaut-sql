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

import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Shared helper to apply Oracle session program (v$session.program)
 * consistently across JDBC pools.
 */
@Internal
public final class OracleSessionProgramHelper {

    private static final String ORACLE_JDBC_PREFIX = "jdbc:oracle";
    private static final String DIALECT_ORACLE = "oracle";
    private static final String KEY_PROGRAM = "v$session.program";

    private OracleSessionProgramHelper() {
    }

    /**
     * Applies the <code>v$session.program</code> property to the given connection properties if the
     * following conditions are met:
     * <ul>
     *     <li>The dialect is 'oracle' or the URL starts with 'jdbc:oracle'</li>
     *     <li>The property 'datasources.{dataSourceName}.oracle.session.enabled' is true (default)</li>
     *     <li>The 'micronaut.application.name' property is non-empty</li>
     *     <li>The 'v$session.program' property is not already provided</li>
     * </ul>.
     *
     * @param dataSourceName the name of the data source
     * @param url the JDBC URL
     * @param dialect the database dialect
     * @param environment the Micronaut environment
     * @param addConnectionProperty a consumer to add a connection property
     * @param alreadyProvided a supplier indicating whether the 'v$session.program' property is already provided
     * @return true if the 'v$session.program' property was applied, false otherwise
     */
    public static boolean apply(
            String dataSourceName,
            @Nullable String url,
            @Nullable String dialect,
            Environment environment,
            BiConsumer<String, String> addConnectionProperty,
            BooleanSupplier alreadyProvided
    ) {
        Objects.requireNonNull(environment, "Environment must not be null");
        Objects.requireNonNull(addConnectionProperty, "addConnectionProperty must not be null");
        Objects.requireNonNull(alreadyProvided, "alreadyProvided must not be null");

        boolean isOracle = false;
        if (dialect != null) {
            isOracle = DIALECT_ORACLE.equalsIgnoreCase(dialect);
        }
        if (!isOracle && url != null) {
            isOracle = url.regionMatches(true, 0, ORACLE_JDBC_PREFIX, 0, ORACLE_JDBC_PREFIX.length());
        }
        if (!isOracle) {
            return false;
        }

        boolean enabled = environment.getProperty("datasources." + dataSourceName + ".oracle.session.enabled", boolean.class)
                .orElse(true);
        if (!enabled || alreadyProvided.getAsBoolean()) {
            return false;
        }

        String program = environment.getProperty("micronaut.application.name", String.class).orElse(null);
        if (StringUtils.isNotEmpty(program)) {
            addConnectionProperty.accept(KEY_PROGRAM, program);
            return true;
        }
        return false;
    }
}
