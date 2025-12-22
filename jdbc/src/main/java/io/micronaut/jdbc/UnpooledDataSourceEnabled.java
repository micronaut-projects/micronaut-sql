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

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;

/**
 * Condition checking whether unpooled datasource is enabled.
 * This condition is satisfied when the property datasources.allow-unpooled=true is set.
 *
 * @author Micronaut Team
 * @since 6.3.0
 */
public final class UnpooledDataSourceEnabled implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        // Check global property
        String globalProperty = "datasources.allow-unpooled";
        return context.getProperty(globalProperty, Boolean.class, false);
    }
}
