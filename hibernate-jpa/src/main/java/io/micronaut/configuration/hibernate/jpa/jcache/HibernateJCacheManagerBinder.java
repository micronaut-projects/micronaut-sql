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
package io.micronaut.configuration.hibernate.jpa.jcache;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import org.hibernate.cache.jcache.ConfigSettings;

import javax.cache.CacheManager;
import java.util.Map;

/**
 * Binds the JCache {@link CacheManager} to Hibernate.
 *
 * @author Marcel Overdijk
 * @since 1.1.0
 */
@Singleton
@Requires(classes = ConfigSettings.class)
@Requires(beans = CacheManager.class)
public class HibernateJCacheManagerBinder implements BeanCreatedEventListener<JpaConfiguration> {

    private final CacheManager cacheManager;

    /**
     * Default constructor.
     *
     * @param cacheManager The cache manager
     */
    public HibernateJCacheManagerBinder(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public JpaConfiguration onCreated(BeanCreatedEvent<JpaConfiguration> event) {
        JpaConfiguration jpaConfiguration = event.getBean();
        Map<String, Object> jpaProperties = jpaConfiguration.getProperties();
        jpaProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
        return jpaConfiguration;
    }
}
