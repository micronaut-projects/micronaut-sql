/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.configuration.jdbi;

import org.jdbi.v3.core.Jdbi;

/**
 * Allows custom actions to be performed on a jdbi instance.
 * Customizers must be annotated with a {@link jakarta.inject.Named} annotation that matches the datasource name
 * corresponding to the jdbi datasource to be customized.
 *
 * @author Dan Maas
 * @since 1.4.0
 */
public interface JdbiCustomizer {

    /**
     * Performs custom configuration operations on the given Jdbi instance.
     * See https://jdbi.org/apidocs/index.html?org/jdbi/v3/core/config/Configurable.html for available options
     *
     * @param jdbi the Jdbi instance
     */
    void customize(Jdbi jdbi);

}
