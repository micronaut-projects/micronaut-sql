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
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.event.spi.EventType;
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
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.EnumType;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

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
        BasicCollectionPersister.class,
        OneToManyPersister.class,
        // Others
        ImplicitNamingStrategyJpaCompliantImpl.class
}, typeNames = {
        "org.hibernate.event.spi.AutoFlushEventListener[]",
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
        // Hibernate Envers
        "org.dom4j.DocumentFactory",
        "org.hibernate.envers.DefaultRevisionEntity",
        "org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy",
        "org.hibernate.envers.configuration.internal.ClassesAuditingData",
        "org.hibernate.envers.configuration.internal.RevisionInfoConfiguration",
        "org.hibernate.envers.internal.EnversMessageLogger",
        "org.hibernate.envers.strategy.DefaultAuditStrategy",
        "org.hibernate.envers.strategy.internal.DefaultAuditStrategy",
        "java.util.ServiceLoader$Provider"
},
   accessType = {AccessType.ALL_PUBLIC})
final class Hql {
}

@TypeHint(typeNames = "org.hibernate.cfg.beanvalidation.TypeSafeActivator", accessType = {AccessType.ALL_PUBLIC})
final class Cfg {
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
    public NoopSchemaResolver(ResourceStreamLocator resourceStreamLocator) {
    }

    @Override
    @Substitute
    public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
            throws XMLStreamException {
        return null;
    }
}
