/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.sql.hibernate.jpa.conf.settings.internal;

import io.micronaut.sql.hibernate.jpa.JpaConfiguration;
import io.micronaut.sql.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.hibernate.cache.jcache.ConfigSettings;

import javax.cache.CacheManager;
import java.util.Collections;
import java.util.Map;

/**
 * JCache setting supplier.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Prototype
@Requires(classes = {ConfigSettings.class, CacheManager.class})
@Requires(beans = CacheManager.class)
final class JCacheManagerSettingSupplier implements SettingsSupplier {

    private final CacheManager cacheManager;

    JCacheManagerSettingSupplier(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        // Backwards compatibility
        jpaConfiguration.getProperties().put(ConfigSettings.CACHE_MANAGER, cacheManager);
        return Collections.singletonMap(ConfigSettings.CACHE_MANAGER, cacheManager);
    }
}
