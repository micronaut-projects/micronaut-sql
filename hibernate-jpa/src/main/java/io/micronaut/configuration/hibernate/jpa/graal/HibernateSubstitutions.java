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
package io.micronaut.configuration.hibernate.jpa.graal;

/*
 * Internal class that provides substitutions for Hibernate on GraalVM substrate.
 *
 * @author graemerocher
 * @since 1.2.0
 */

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.micronaut.configuration.hibernate.jpa.proxy.IntrospectedHibernateBytecodeProvider;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.annotation.TypeHint.AccessType;
import io.micronaut.jdbc.spring.HibernatePresenceCondition;
import org.dom4j.DocumentFactory;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy;
import org.hibernate.envers.configuration.internal.ClassesAuditingData;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.event.spi.EventType;
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
import org.hibernate.id.Assigned;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.jmx.spi.JmxService;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.hibernate.type.EnumType;

import javax.management.ObjectName;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

// Additional classes
@TypeHint(
        typeNames = {
                "org.hibernate.internal.CoreMessageLogger_$logger",
                "org.hibernate.internal.EntityManagerMessageLogger_$logger",
                "org.hibernate.annotations.common.util.impl.Log_$logger",
                "com.sun.xml.internal.stream.events.XMLEventFactoryImpl",
                "org.hibernate.bytecode.enhance.spi.interceptor.BytecodeInterceptorLogging_$logger",
                "org.hibernate.envers.internal.EnversMessageLogger_$logger",
        }
)
final class Loggers {
}

// For HQL and other Hibernate internal classes
@TypeHint(value = {
        // Hibernate
        HibernatePresenceCondition.class,
        SingleTableEntityPersister.class,
        EntityMetamodel.class,
        PojoComponentTuplizer.class,
        PojoEntityTuplizer.class,
        BasicCollectionPersister.class,
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
        // Hibernate Envers
        LegacyModifiedColumnNamingStrategy.class,
        DefaultRevisionEntity.class,
        RevisionInfoConfiguration.class,
        DefaultAuditStrategy.class,
        org.hibernate.envers.strategy.internal.DefaultAuditStrategy.class,
        DocumentFactory.class,
        EnversMessageLogger.class,
        ClassesAuditingData.class,
        // Others
        ImplicitNamingStrategyJpaCompliantImpl.class
}, typeNames = {
        "org.hibernate.event.spi.AutoFlushEventListener[]",
        "org.hibernate.event.spi.PersistEventListener[]",
        "org.hibernate.event.spi.ClearEventListener[]",
        "org.hibernate.event.spi.DeleteEventListener[]",
        "org.hibernate.event.spi.DirtyCheckEventListener[]",
        "org.hibernate.event.spi.EvictEventListener[]",
        "org.hibernate.event.spi.FlushEntityEventListener[]",
        "org.hibernate.event.spi.FlushEventListener[]",
        "org.hibernate.event.spi.InitializeCollectionEventListener[]",
        "org.hibernate.event.spi.LockEventListener[]",
        "org.hibernate.event.spi.MergeEventListener[]",
        "org.hibernate.event.spi.PersistEventListener[]",
        "org.hibernate.event.spi.PostActionEventListener[]",
        "org.hibernate.event.spi.PostCollectionRecreateEventListener[]",
        "org.hibernate.event.spi.PostCollectionRemoveEventListener[]",
        "org.hibernate.event.spi.PostCollectionUpdateEventListener[]",
        "org.hibernate.event.spi.PostCommitDeleteEventListener[]",
        "org.hibernate.event.spi.PostCommitInsertEventListener[]",
        "org.hibernate.event.spi.PostCommitUpdateEventListener[]",
        "org.hibernate.event.spi.PostDeleteEventListener[]",
        "org.hibernate.event.spi.PostInsertEventListener[]",
        "org.hibernate.event.spi.PostLoadEventListener[]",
        "org.hibernate.event.spi.PostUpdateEventListener[]",
        "org.hibernate.event.spi.PreCollectionRecreateEventListener[]",
        "org.hibernate.event.spi.PreCollectionRemoveEventListener[]",
        "org.hibernate.event.spi.PreCollectionUpdateEventListener[]",
        "org.hibernate.event.spi.PreDeleteEventListener[]",
        "org.hibernate.event.spi.PreInsertEventListener[]",
        "org.hibernate.event.spi.PreLoadEventListener[]",
        "org.hibernate.event.spi.PreUpdateEventListener[]",
        "org.hibernate.event.spi.RefreshEventListener[]",
        "org.hibernate.event.spi.ReplicateEventListener[]",
        "org.hibernate.event.spi.ResolveNaturalIdEventListener[]",
        "org.hibernate.event.spi.SaveOrUpdateEventListener[]",
        "org.hibernate.event.spi.LoadEventListener[]",
        "java.util.ServiceLoader$Provider"
},
   accessType = {TypeHint.AccessType.ALL_PUBLIC})
final class Hql {
}

@TypeHint(typeNames = "org.hibernate.cfg.beanvalidation.TypeSafeActivator", accessType = {TypeHint.AccessType.ALL_PUBLIC})
final class Cfg {
}

// ID Generators
@TypeHint({
        UUIDGenerator.class,
        GUIDGenerator.class,
        UUIDHexGenerator.class,
        Assigned.class,
        IdentityGenerator.class,
        SelectGenerator.class,
        SequenceStyleGenerator.class,
        IncrementGenerator.class,
        ForeignGenerator.class,
        TableGenerator.class
})
final class IdGenerators {
}

// ID Optimizers
@TypeHint({
        NoopOptimizer.class,
        HiLoOptimizer.class,
        LegacyHiLoAlgorithmOptimizer.class,
        PooledOptimizer.class,
        PooledLoOptimizer.class,
        PooledLoThreadLocalOptimizer.class
})
final class IdOptimizers {
}

// Disable Runtime Byte Code Enhancement
@TargetClass(className = "org.hibernate.cfg.Environment")
@TypeHint(
        value = {EventType.class, EnumType.class},
        accessType = {AccessType.ALL_DECLARED_FIELDS, AccessType.ALL_DECLARED_METHODS, AccessType.ALL_DECLARED_CONSTRUCTORS}
)
final class EnvironmentSubs {
    @Substitute
    public static BytecodeProvider buildBytecodeProvider(Properties properties) {
        return new IntrospectedHibernateBytecodeProvider();
    }
}

// Disable JMX support
@TargetClass(className = "org.hibernate.jmx.internal.JmxServiceImpl")
@Substitute
final class NoopJmxService implements JmxService, Stoppable {

    @Substitute
    public NoopJmxService(Map configValues) {
    }

    @Override
    public void stop() {

    }

    @Override
    public void registerService(Manageable service, Class<? extends Service> serviceRole) {

    }

    @Override
    public void registerMBean(ObjectName objectName, Object mBean) {

    }
}

// Disable XML support
@TargetClass(className = "org.hibernate.boot.spi.XmlMappingBinderAccess")
@Substitute
final class NoopXmlMappingBinderAccess {

    @Substitute
    public NoopXmlMappingBinderAccess(ServiceRegistry serviceRegistry) {
    }

    @Substitute
    public MappingBinder getMappingBinder() {
        return null;
    }

    @Substitute
    public Binding bind(String resource) {
        return null;
    }

    @Substitute
    public Binding bind(File file) {
        return null;
    }

    @Substitute
    public Binding bind(InputStreamAccess xmlInputStreamAccess) {
        return null;
    }

    @Substitute
    public Binding bind(InputStream xmlInputStream) {
        return null;
    }

    @Substitute
    public Binding bind(URL url) {
        return null;
    }
}

// Disable Schema Resolution
@TargetClass(className = "org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver")
@Substitute
final class NoopSchemaResolver implements XMLResolver {
    @Substitute
    public NoopSchemaResolver(ClassLoaderService classLoaderService) {
    }

    @Override
    @Substitute
    public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
            throws XMLStreamException {
        return null;
    }
}
