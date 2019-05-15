/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.hibernate.jpa;

import com.sun.el.ExpressionFactoryImpl;
import io.micronaut.configuration.hibernate.jpa.condition.EntitiesInPackageCondition;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;
import io.micronaut.jdbc.spring.HibernatePresenceCondition;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.integrator.spi.Integrator;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.Entity;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.hql.internal.ast.HqlToken;
import org.hibernate.hql.internal.ast.tree.AggregateNode;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.internal.ast.tree.BetweenOperatorNode;
import org.hibernate.hql.internal.ast.tree.BinaryArithmeticOperatorNode;
import org.hibernate.hql.internal.ast.tree.BinaryLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.BooleanLiteralNode;
import org.hibernate.hql.internal.ast.tree.CastFunctionNode;
import org.hibernate.hql.internal.ast.tree.CollectionFunction;
import org.hibernate.hql.internal.ast.tree.ComponentJoin;
import org.hibernate.hql.internal.ast.tree.ConstructorNode;
import org.hibernate.hql.internal.ast.tree.CountNode;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.DotNode;
import org.hibernate.hql.internal.ast.tree.EntityJoinFromElement;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FromElementFactory;
import org.hibernate.hql.internal.ast.tree.FromReferenceNode;
import org.hibernate.hql.internal.ast.tree.HqlSqlWalkerNode;
import org.hibernate.hql.internal.ast.tree.IdentNode;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.hql.internal.ast.tree.InLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.IndexNode;
import org.hibernate.hql.internal.ast.tree.InsertStatement;
import org.hibernate.hql.internal.ast.tree.IntoClause;
import org.hibernate.hql.internal.ast.tree.IsNotNullLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.IsNullLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.JavaConstantNode;
import org.hibernate.hql.internal.ast.tree.LiteralNode;
import org.hibernate.hql.internal.ast.tree.MapEntryNode;
import org.hibernate.hql.internal.ast.tree.MapKeyEntityFromElement;
import org.hibernate.hql.internal.ast.tree.MapKeyNode;
import org.hibernate.hql.internal.ast.tree.MapValueNode;
import org.hibernate.hql.internal.ast.tree.MethodNode;
import org.hibernate.hql.internal.ast.tree.Node;
import org.hibernate.hql.internal.ast.tree.NullNode;
import org.hibernate.hql.internal.ast.tree.OrderByClause;
import org.hibernate.hql.internal.ast.tree.ParameterNode;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.ResultVariableRefNode;
import org.hibernate.hql.internal.ast.tree.SearchedCaseNode;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.hql.internal.ast.tree.SelectExpressionImpl;
import org.hibernate.hql.internal.ast.tree.SelectExpressionList;
import org.hibernate.hql.internal.ast.tree.SimpleCaseNode;
import org.hibernate.hql.internal.ast.tree.SqlFragment;
import org.hibernate.hql.internal.ast.tree.SqlNode;
import org.hibernate.hql.internal.ast.tree.UnaryArithmeticNode;
import org.hibernate.hql.internal.ast.tree.UnaryLogicOperatorNode;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.springframework.orm.hibernate5.SpringSessionContext;

/**
 * Configuration for JPA and Hibernate.
 *
 * @author graemerocher
 * @since 1.0
 */
@TypeHint(value = {
        // Hibernate
        HibernatePresenceCondition.class,
        EntitiesInPackageCondition.class,
        IdentityGenerator.class,
        SingleTableEntityPersister.class,
        EntityMetamodel.class,
        PojoEntityTuplizer.class,
        BasicCollectionPersister.class,
        SpringSessionContext.class,
        HqlToken.class,
        OneToManyPersister.class,
        // Hibernate AST
        AggregateNode.class,
        AssignmentSpecification.class,
        BetweenOperatorNode.class,
        BinaryArithmeticOperatorNode.class,
        BinaryLogicOperatorNode.class,
        BooleanLiteralNode.class,
        CastFunctionNode.class,
        CollectionFunction.class,
        ComponentJoin.class,
        ConstructorNode.class,
        CountNode.class,
        DeleteStatement.class,
        DotNode.class,
        EntityJoinFromElement.class,
        FromClause.class,
        FromElement.class,
        FromElementFactory.class,
        FromReferenceNode.class,
        HqlSqlWalkerNode.class,
        IdentNode.class,
        ImpliedFromElement.class,
        IndexNode.class,
        InLogicOperatorNode.class,
        InsertStatement.class,
        IntoClause.class,
        IsNotNullLogicOperatorNode.class,
        IsNullLogicOperatorNode.class,
        JavaConstantNode.class,
        LiteralNode.class,
        MapEntryNode.class,
        MapKeyEntityFromElement.class,
        MapKeyNode.class,
        MapValueNode.class,
        MethodNode.class,
        Node.class,
        NullNode.class,
        OrderByClause.class,
        ParameterNode.class,
        QueryNode.class,
        ResultVariableRefNode.class,
        SearchedCaseNode.class,
        SelectClause.class,
        SelectExpressionImpl.class,
        SelectExpressionList.class,
        SimpleCaseNode.class,
        SqlFragment.class,
        SqlNode.class,
        UnaryArithmeticNode.class,
        UnaryLogicOperatorNode.class,
        UpdateStatement.class,
        // Others
        ExpressionFactoryImpl.class,
        ImplicitNamingStrategyJpaCompliantImpl.class
}, accessType = {TypeHint.AccessType.ALL_PUBLIC})
@EachProperty(value = JpaConfiguration.PREFIX, primary = "default")
public class JpaConfiguration {
    public static final String PREFIX = "jpa";

    private final BootstrapServiceRegistry bootstrapServiceRegistry;
    private final Environment environment;

    private Map<String, Object> jpaProperties;
    private EntityScanConfiguration entityScanConfiguration;

    /**
     * @param applicationContext The application context
     * @param integrator         The {@link Integrator}
     */
    protected JpaConfiguration(ApplicationContext applicationContext,
                               @Nullable Integrator integrator) {
        this(applicationContext, integrator, new EntityScanConfiguration(applicationContext.getEnvironment()));
    }

    /**
     * @param applicationContext The application context
     * @param integrator         The {@link Integrator}
     */
    @Inject
    protected JpaConfiguration(ApplicationContext applicationContext,
                               @Nullable Integrator integrator,
                               @Nullable EntityScanConfiguration entityScanConfiguration) {
        ClassLoader classLoader = applicationContext.getClassLoader();
        BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder =
                createBootstrapServiceRegistryBuilder(integrator, classLoader);

        this.bootstrapServiceRegistry = bootstrapServiceRegistryBuilder.build();
        this.entityScanConfiguration = entityScanConfiguration != null ? entityScanConfiguration : new EntityScanConfiguration(applicationContext.getEnvironment());
        this.environment = applicationContext.getEnvironment();
    }

    /**
     * @return The entity scan configuration
     */
    public EntityScanConfiguration getEntityScanConfiguration() {
        return entityScanConfiguration;
    }

    /**
     * Builds the standard service registry.
     *
     * @param additionalSettings Additional settings for the service registry
     * @return The standard service registry
     */
    @SuppressWarnings("WeakerAccess")
    public StandardServiceRegistry buildStandardServiceRegistry(@Nullable Map<String, Object> additionalSettings) {
        StandardServiceRegistryBuilder standardServiceRegistryBuilder = createStandServiceRegistryBuilder(bootstrapServiceRegistry);

        if (jpaProperties != null) {
            standardServiceRegistryBuilder.applySettings(jpaProperties);
        }
        if (additionalSettings != null) {
            standardServiceRegistryBuilder.applySettings(additionalSettings);
        }
        return standardServiceRegistryBuilder.build();
    }

    /**
     * Sets the packages to scan.
     *
     * @param packagesToScan The packages to scan
     */
    public void setPackagesToScan(String... packagesToScan) {
        if (ArrayUtils.isNotEmpty(packagesToScan)) {
            EntityScanConfiguration entityScanConfiguration = new EntityScanConfiguration(environment);
            entityScanConfiguration.setClasspath(true);
            entityScanConfiguration.setPackages(packagesToScan);
            this.entityScanConfiguration = entityScanConfiguration;
        }
    }

    /**
     * @return The packages to scan
     */
    public String[] getPackagesToScan() {
        return entityScanConfiguration.getPackages();
    }

    /**
     * Sets the JPA properties to be passed to the JPA implementation.
     *
     * @param jpaProperties The JPA properties
     */
    public final void setProperties(
        @MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.UNDER_SCORE_SEPARATED_LOWER_CASE)
            Map<String, Object> jpaProperties) {
        this.jpaProperties = jpaProperties;
    }

    /**
     * @return The JPA properties
     */
    public Map<String, Object> getProperties() {
        return jpaProperties;
    }

    /**
     * Creates the default {@link BootstrapServiceRegistryBuilder}.
     *
     * @param integrator  The integrator to use. Can be null
     * @param classLoader The class loade rto use
     * @return The BootstrapServiceRegistryBuilder
     */
    @SuppressWarnings("WeakerAccess")
    protected BootstrapServiceRegistryBuilder createBootstrapServiceRegistryBuilder(
        @Nullable Integrator integrator,
        ClassLoader classLoader) {
        BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
        bootstrapServiceRegistryBuilder.applyClassLoader(classLoader);
        if (integrator != null) {
            bootstrapServiceRegistryBuilder.applyIntegrator(integrator);
        }
        return bootstrapServiceRegistryBuilder;
    }

    /**
     * Creates the standard service registry builder.
     *
     * @param bootstrapServiceRegistry The {@link BootstrapServiceRegistry} instance
     * @return The {@link StandardServiceRegistryBuilder} instance
     */
    @SuppressWarnings("WeakerAccess")
    protected StandardServiceRegistryBuilder createStandServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
        return new StandardServiceRegistryBuilder(
            bootstrapServiceRegistry
        );
    }

    /**
     * The entity scan configuration.
     */
    @ConfigurationProperties("entity-scan")
    public static class EntityScanConfiguration implements Toggleable {

        private boolean enabled = true;
        private boolean classpath = false;
        private String[] packages = StringUtils.EMPTY_STRING_ARRAY;

        private final Environment environment;

        /**
         * Default constructor.
         * @param environment The environment
         */
        public EntityScanConfiguration(Environment environment) {
            this.environment = environment;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @return Whether to scan the whole classpath or just look for introspected beans compiled by this application.
         */
        public boolean isClasspath() {
            return classpath;
        }

        /**
         * Sets whether to scan the whole classpath including external JAR files using classpath scanning or just look for introspected beans compiled by this application.
         * @param classpath True if extensive classpath scanning should be used
         */
        public void setClasspath(boolean classpath) {
            this.classpath = classpath;
        }

        /**
         * Set whether entity scan is enabled. Defaults to true.
         * @param enabled True if it is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * The packages to limit the scan to.
         * @return The packages to limit the scan to
         */
        public String[] getPackages() {
            return packages;
        }

        /**
         * @param packages The packages
         */
        public void setPackages(String[] packages) {
            this.packages = packages;
        }

        /**
         * Find entities for the current configuration.
         * @return The entities
         */
        public Collection<Class<?>> findEntities() {
            Collection<Class<?>> entities = new HashSet<>();
            if (isClasspath()) {

                if (ArrayUtils.isNotEmpty(packages)) {
                    environment.scan(Entity.class, packages).forEach(entities::add);
                } else {
                    environment.scan(Entity.class).forEach(entities::add);
                }
            }

            if (isEnabled()) {
                Collection<BeanIntrospection<Object>> introspections;
                if (ArrayUtils.isNotEmpty(packages)) {
                    introspections = BeanIntrospector.SHARED.findIntrospections(Entity.class, packages);
                } else {
                    introspections = BeanIntrospector.SHARED.findIntrospections(Entity.class);
                }
                introspections
                        .stream().map(BeanIntrospection::getBeanType)
                        .forEach(entities::add);
            }
            return Collections.unmodifiableCollection(entities);
        }
    }
}
