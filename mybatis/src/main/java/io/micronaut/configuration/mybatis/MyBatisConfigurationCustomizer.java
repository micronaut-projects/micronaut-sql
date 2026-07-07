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
package io.micronaut.configuration.mybatis;

import org.apache.ibatis.session.Configuration;

/**
 * Allows custom actions to be performed on a MyBatis {@link Configuration}.
 * Customizers may be annotated with {@link jakarta.inject.Named} to target a specific datasource.
 *
 * @author Graeme Rocher
 * @since 7.1.0
 */
public interface MyBatisConfigurationCustomizer {

    /**
     * Performs custom configuration operations on the given MyBatis configuration.
     *
     * @param configuration The configuration to customize
     */
    void customize(Configuration configuration);
}
