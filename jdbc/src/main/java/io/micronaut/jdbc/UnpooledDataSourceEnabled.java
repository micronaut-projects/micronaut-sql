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

import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.Qualifier;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.naming.Named;
import io.micronaut.inject.BeanDefinition;

/**
 * Condition checking whether unpooled datasource is enabled.
 * If there is property datasources.datasource-name.allow-unpooled=true or
 * datasources.allow-unpooled=true then this condition will be satisfied
 * for datasource with given datasource-name.
 *
 * @author Micronaut Team
 * @since 6.3.0
 */
public final class UnpooledDataSourceEnabled implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        BeanResolutionContext beanResolutionContext = context.getBeanResolutionContext();
        String dataSourceName;
        if (beanResolutionContext == null) {
            // Check global property when no bean context available
            String globalProperty = "datasources.allow-unpooled";
            return context.getProperty(globalProperty, Boolean.class, false);
        } else {
            Qualifier<?> currentQualifier = beanResolutionContext.getCurrentQualifier();
            if (currentQualifier == null && context.getComponent() instanceof BeanDefinition<?> definition) {
                currentQualifier = definition.getDeclaredQualifier();
            }
            if (currentQualifier instanceof Named named) {
                dataSourceName = named.getName();
            } else {
                dataSourceName = "default";
            }
        }
        
        // Check datasource-specific property first
        String specificProperty = "datasources." + dataSourceName + ".allow-unpooled";
        Boolean specificValue = context.getProperty(specificProperty, Boolean.class).orElse(null);
        if (specificValue != null) {
            return specificValue;
        }
        
        // Check global property
        String globalProperty = "datasources.allow-unpooled";
        return context.getProperty(globalProperty, Boolean.class, false);
    }
}
