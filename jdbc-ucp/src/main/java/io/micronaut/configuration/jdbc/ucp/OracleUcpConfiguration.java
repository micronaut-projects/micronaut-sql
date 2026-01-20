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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import org.jspecify.annotations.Nullable;

import static io.micronaut.configuration.jdbc.ucp.OracleUcpConfiguration.PREFIX;

/**
 * Configuration properties for Oracle Universal Connection Pooling (UCP).
 *
 * @param destroyOnReload An indicating whether to destroy connections on reload
 * @param createConnectionInBorrowThread An indicator telling whether connection pool should create connection in borrow thread.
 *
 */
@ConfigurationProperties(PREFIX)
@Requires(property = PREFIX)
public record OracleUcpConfiguration(@Nullable Boolean destroyOnReload, @Nullable Boolean createConnectionInBorrowThread) {

    /** Prefix used for configuration properties. */
    static final String PREFIX = "oracle.ucp";
}
