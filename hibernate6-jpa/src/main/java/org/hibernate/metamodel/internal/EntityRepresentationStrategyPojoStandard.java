/*
 * Copyright 2017-2023 original authors
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
package org.hibernate.metamodel.internal;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.ProxyFactoryHelper;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.CompositeTypeImplementor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * @author Steve Ebersole
 */
public class EntityRepresentationStrategyPojoStandard implements EntityRepresentationStrategy {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityRepresentationStrategyPojoStandard.class );

	private final JavaType<?> mappedJtd;
	private final JavaType<?> proxyJtd;

	private final boolean isBytecodeEnhanced;
	private final boolean lifecycleImplementor;

	private final ReflectionOptimizer reflectionOptimizer;
	private final ProxyFactory proxyFactory;
	private final EntityInstantiator instantiator;

	private final StrategySelector strategySelector;

	private final String identifierPropertyName;
	private final PropertyAccess identifierPropertyAccess;
	private final Map<String, PropertyAccess> propertyAccessMap = new ConcurrentHashMap<>();
	private final EmbeddableRepresentationStrategyPojo mapsIdRepresentationStrategy;

	public EntityRepresentationStrategyPojoStandard(
			PersistentClass bootDescriptor,
			EntityPersister runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final JavaTypeRegistry jtdRegistry = creationContext.getTypeConfiguration()
				.getJavaTypeRegistry();

		final Class<?> mappedJavaType = bootDescriptor.getMappedClass();
		this.mappedJtd = jtdRegistry.resolveEntityTypeDescriptor( mappedJavaType );

		final Class<?> proxyJavaType = bootDescriptor.getProxyInterface();
		if ( proxyJavaType != null ) {
			this.proxyJtd = jtdRegistry.getDescriptor( proxyJavaType );
		}
		else {
			this.proxyJtd = null;
		}

		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedJavaType );
		this.isBytecodeEnhanced = isPersistentAttributeInterceptableType( mappedJavaType );

		final Property identifierProperty = bootDescriptor.getIdentifierProperty();
		if ( identifierProperty == null ) {
			identifierPropertyName = null;
			identifierPropertyAccess = null;

			final KeyValue bootDescriptorIdentifier = bootDescriptor.getIdentifier();

			if ( bootDescriptorIdentifier instanceof Component ) {
				if ( bootDescriptor.getIdentifierMapper() != null ) {
					mapsIdRepresentationStrategy = new EmbeddableRepresentationStrategyPojo(
							bootDescriptor.getIdentifierMapper(),
							() -> ( ( CompositeTypeImplementor) bootDescriptor.getIdentifierMapper().getType() )
									.getMappingModelPart().getEmbeddableTypeDescriptor(),
							// we currently do not support custom instantiators for identifiers
							null,
							null,
							creationContext
					);
				}
				else if ( bootDescriptorIdentifier != null ) {
					mapsIdRepresentationStrategy = new EmbeddableRepresentationStrategyPojo(
							(Component) bootDescriptorIdentifier,
							() -> ( ( CompositeTypeImplementor) bootDescriptor.getIdentifierMapper().getType() )
									.getMappingModelPart().getEmbeddableTypeDescriptor(),
							// we currently do not support custom instantiators for identifiers
							null,
							null,
							creationContext
					);
				}
				else {
					mapsIdRepresentationStrategy = null;
				}
			}
			else {
				mapsIdRepresentationStrategy = null;
			}
		}
		else {
			mapsIdRepresentationStrategy = null;
			identifierPropertyName = identifierProperty.getName();
			identifierPropertyAccess = makePropertyAccess( identifierProperty );
		}

		final BytecodeProvider bytecodeProvider = creationContext.getBootstrapContext().getServiceRegistry().getService(BytecodeProvider.class);
		//final BytecodeProvider bytecodeProvider = Environment.getBytecodeProvider();

		final EntityMetamodel entityMetamodel = runtimeDescriptor.getEntityMetamodel();
		ProxyFactory proxyFactory = null;
		if ( proxyJtd != null && entityMetamodel.isLazy() ) {
			proxyFactory = createProxyFactory( bootDescriptor, bytecodeProvider, creationContext );
			if ( proxyFactory == null ) {
				entityMetamodel.setLazy( false );
			}
		}
		this.proxyFactory = proxyFactory;

		// resolveReflectionOptimizer may lead to a makePropertyAccess call which requires strategySelector
		this.strategySelector = sessionFactory.getServiceRegistry().getService( StrategySelector.class );

		this.reflectionOptimizer = resolveReflectionOptimizer( bootDescriptor, bytecodeProvider, sessionFactory );

		this.instantiator = determineInstantiator( bootDescriptor, entityMetamodel );
	}

	private EntityInstantiator determineInstantiator(PersistentClass bootDescriptor, EntityMetamodel entityMetamodel) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			final InstantiationOptimizer instantiationOptimizer = reflectionOptimizer.getInstantiationOptimizer();
			return new EntityInstantiatorPojoOptimized(
					entityMetamodel,
					bootDescriptor,
					mappedJtd,
					instantiationOptimizer
			);
		}

		return new EntityInstantiatorPojoStandard( entityMetamodel, bootDescriptor, mappedJtd );
	}

	private ProxyFactory createProxyFactory(
			PersistentClass bootDescriptor,
			BytecodeProvider bytecodeProvider,
			RuntimeModelCreationContext creationContext) {

		final Set<Class<?>> proxyInterfaces = new java.util.HashSet<>();

		final Class<?> mappedClass = mappedJtd.getJavaTypeClass();
		Class<?> proxyInterface;
		if ( proxyJtd != null ) {
			proxyInterface = proxyJtd.getJavaTypeClass();
		}
		else {
			proxyInterface = null;
		}

		if ( proxyInterface != null && ! mappedClass.equals( proxyInterface ) ) {
			if ( ! proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + bootDescriptor.getEntityName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		for ( Subclass subclass : bootDescriptor.getSubclasses() ) {
			final Class<?> subclassProxy = subclass.getProxyInterface();
			final Class<?> subclassClass = subclass.getMappedClass();
			if ( subclassProxy != null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		proxyInterfaces.add( HibernateProxy.class );

		Class<?> clazz = bootDescriptor.getMappedClass();
		final Method idGetterMethod;
		final Method idSetterMethod;
		try {
			for ( Property property : bootDescriptor.getProperties() ) {
				ProxyFactoryHelper.validateGetterSetterMethodProxyability(
						"Getter",
						property.getGetter( clazz ).getMethod()
				);
				ProxyFactoryHelper.validateGetterSetterMethodProxyability(
						"Setter",
						property.getSetter( clazz ).getMethod()
				);
			}
			if ( identifierPropertyAccess != null ) {
				idGetterMethod = identifierPropertyAccess.getGetter().getMethod();
				idSetterMethod = identifierPropertyAccess.getSetter().getMethod();
				ProxyFactoryHelper.validateGetterSetterMethodProxyability(
						"Getter",
						idGetterMethod
				);
				ProxyFactoryHelper.validateGetterSetterMethodProxyability(
						"Setter",
						idSetterMethod
				);
			}
			else {
				idGetterMethod = null;
				idSetterMethod = null;
			}
		}
		catch (HibernateException he) {
			//LOG.unableToCreateProxyFactory( clazz.getName(), he );
			return null;
		}

		final Method proxyGetIdentifierMethod = idGetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idGetterMethod );
		final Method proxySetIdentifierMethod = idSetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idSetterMethod );

		ProxyFactory pf = bytecodeProvider.getProxyFactoryFactory().buildProxyFactory( creationContext.getSessionFactory() );
		try {
			pf.postInstantiate(
					bootDescriptor.getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					bootDescriptor.hasEmbeddedIdentifier() ?
							(CompositeType) bootDescriptor.getIdentifier().getType() :
							null
			);

			return pf;
		}
		catch (HibernateException he) {
			//LOG.unableToCreateProxyFactory( bootDescriptor.getEntityName(), he );
			return null;
		}
	}

	private ReflectionOptimizer resolveReflectionOptimizer(
			PersistentClass bootType,
			BytecodeProvider bytecodeProvider,
			SessionFactoryImplementor sessionFactory) {
		final Class<?> javaTypeToReflect;
		if ( proxyFactory != null ) {
			assert proxyJtd != null;
			javaTypeToReflect = proxyJtd.getJavaTypeClass();
		}
		else {
			javaTypeToReflect = mappedJtd.getJavaTypeClass();
		}

		final List<String> getterNames = new ArrayList<>();
		final List<String> setterNames = new ArrayList<>();
		final List<Class<?>> getterTypes = new ArrayList<>();

		boolean foundCustomAccessor = false;

		final Iterator<Property> itr = bootType.getPropertyClosureIterator();
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			final Property property = itr.next();
			final PropertyAccess propertyAccess = makePropertyAccess( property );

			propertyAccessMap.put( property.getName(), propertyAccess );

			if ( ! (propertyAccess instanceof PropertyAccessBasicImpl) ) {
				foundCustomAccessor = true;
			}

			getterNames.add( propertyAccess.getGetter().getMethodName() );
			getterTypes.add( propertyAccess.getGetter().getReturnTypeClass() );

			setterNames.add( propertyAccess.getSetter().getMethodName() );
		}

		if ( foundCustomAccessor || ! Environment.useReflectionOptimizer() ) {
			return null;
		}

		return bytecodeProvider.getReflectionOptimizer(
				javaTypeToReflect,
				getterNames.toArray( new String[0] ),
				setterNames.toArray( new String[0] ),
				getterTypes.toArray( new Class[0] )
		);
	}

	private PropertyAccess makePropertyAccess(Property bootAttributeDescriptor) {
		PropertyAccessStrategy strategy = bootAttributeDescriptor.getPropertyAccessStrategy( mappedJtd.getJavaTypeClass() );

		if ( strategy == null ) {
			final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
			if ( StringHelper.isNotEmpty( propertyAccessorName ) ) {
				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy( PropertyAccessStrategy.class, propertyAccessorName );
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref ) {
					final Backref backref = (Backref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyBackRefImpl( backref.getCollectionRole(), backref
							.getEntityName() );
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref ) {
					final IndexBackref indexBackref = (IndexBackref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							mappedJtd.getJavaType().getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess( mappedJtd.getJavaTypeClass(), bootAttributeDescriptor.getName(), true );
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return reflectionOptimizer;
	}

	@Override
	public EntityInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	@Override
	public boolean isLifecycleImplementor() {
		return lifecycleImplementor;
	}

	@Override
	public boolean isBytecodeEnhanced() {
		return isBytecodeEnhanced;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return mappedJtd;
	}

	@Override
	public JavaType<?> getProxyJavaType() {
		return proxyJtd;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		if ( bootAttributeDescriptor.getName().equals( identifierPropertyName ) ) {
			return identifierPropertyAccess;
		}

		PropertyAccess propertyAccess = propertyAccessMap.get( bootAttributeDescriptor.getName() );
		if ( propertyAccess != null ) {
			return propertyAccess;
		}

		if ( mapsIdRepresentationStrategy != null ) {
			return mapsIdRepresentationStrategy.resolvePropertyAccess( bootAttributeDescriptor );
		}

		return null;
	}
}
