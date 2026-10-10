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
package io.micronaut.configuration.hibernate.jpa.dev;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Any;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jdbc.DataSourceResolver;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.tool.schema.Action;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Keeps Hibernate's {@code create} schema generation from dropping the tables of a data source that a development
 * restart retained. It exists only in development mode, so nothing of it is on the path of a production session
 * factory.
 *
 * <p>The connection pools are retained across a development restart, and with them an in-memory database, while
 * each generation builds a session factory of its own. {@code create}, the {@code drop-and-create} of the
 * Jakarta Persistence specification, drops the tables and creates them again as each session factory starts, so
 * each restart would silently delete the rows. The first session factory built on a data source generates the
 * schema as configured, as the application does when it starts; one built on a data source that an earlier
 * generation used instead updates the schema, which creates the missing tables and columns and drops nothing,
 * and a warning recommends {@code create-drop} or {@code update} for the {@code dev} environment.</p>
 *
 * <p>It remembers the data sources weakly and by identity, and nothing else: the pools are of the classes that
 * outlive each generation, and one the runtime closes and releases is forgotten.</p>
 *
 * @author graemerocher
 * @since 7.3.0
 */
@Internal
@Prototype
@DevelopmentActive
@Requires(classes = {DataSource.class, DataSourceResolver.class})
final class DevelopmentSchemaSettingsSupplier implements SettingsSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentSchemaSettingsSupplier.class);

    /**
     * The data sources that a session factory of this process was built on. A development runtime runs one
     * application per process, so a data source seen again is one a restart retained.
     */
    private static final List<WeakReference<DataSource>> BUILT_ON = new ArrayList<>();

    private static final String[] JPA_ACTIONS = {
        SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
        SchemaToolingSettings.HBM2DDL_DATABASE_ACTION
    };

    private final BeanProvider<DataSource> dataSourceBeanProvider;
    private final @Nullable DataSourceResolver dataSourceResolver;

    DevelopmentSchemaSettingsSupplier(@Any BeanProvider<DataSource> dataSourceBeanProvider, @Nullable DataSourceResolver dataSourceResolver) {
        this.dataSourceBeanProvider = dataSourceBeanProvider;
        this.dataSourceResolver = dataSourceResolver;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        DataSource dataSource = dataSourceBeanProvider.find(Qualifiers.byName(jpaConfiguration.getName())).orElse(null);
        if (dataSource == null) {
            return Collections.emptyMap();
        }
        if (dataSourceResolver != null) {
            dataSource = dataSourceResolver.resolve(dataSource);
        }
        if (remember(dataSource)) {
            // the first session factory on this data source: the schema is generated as configured
            return Collections.emptyMap();
        }
        String key = createActionKey(jpaConfiguration.getProperties());
        if (key == null) {
            return Collections.emptyMap();
        }
        String property = "jpa." + jpaConfiguration.getName() + ".properties." + key;
        LOG.warn("""
            {}={} drops and recreates the tables of persistence unit [{}] as each session factory starts, but development \
            mode retained its data source across this restart: the schema is updated instead, which drops no table and \
            keeps the rows. To choose, set {}=create-drop (fresh tables on each restart) or {}=update in \
            application-dev.properties.""",
            property, jpaConfiguration.getProperties().get(key), jpaConfiguration.getName(), property, property);
        // Hibernate reads its own action names under the keys of the specification too
        return Collections.singletonMap(key, Action.UPDATE.getExternalHbm2ddlName());
    }

    /**
     * Remembers a data source, by identity: a pool need not define equality, and one that does may equal another.
     *
     * @param dataSource The data source
     * @return True when no session factory of this process was built on it before
     */
    @SuppressWarnings("ReferenceEquality") // a data source is the same pool by identity
    private static boolean remember(DataSource dataSource) {
        synchronized (BUILT_ON) {
            boolean seen = false;
            for (Iterator<WeakReference<DataSource>> i = BUILT_ON.iterator(); i.hasNext(); ) {
                DataSource remembered = i.next().get();
                if (remembered == null) {
                    i.remove();
                } else if (remembered == dataSource) {
                    seen = true;
                }
            }
            if (!seen) {
                BUILT_ON.add(new WeakReference<>(dataSource));
            }
            return !seen;
        }
    }

    /**
     * The setting that has Hibernate drop and recreate the schema on the database as the session factory starts, as
     * Hibernate reads them: the database action of the specification takes precedence over {@code hibernate.hbm2ddl.auto}.
     *
     * @param properties The properties of the persistence unit
     * @return The key of the setting, or null when the schema is not dropped and recreated
     */
    private static @Nullable String createActionKey(Map<String, Object> properties) {
        for (String key : JPA_ACTIONS) {
            Object value = properties.get(key);
            if (value != null) {
                return Action.interpretJpaSetting(value) == Action.CREATE ? key : null;
            }
        }
        Object value = properties.get(SchemaToolingSettings.HBM2DDL_AUTO);
        return value != null && Action.interpretHbm2ddlSetting(value) == Action.CREATE ? SchemaToolingSettings.HBM2DDL_AUTO : null;
    }
}
