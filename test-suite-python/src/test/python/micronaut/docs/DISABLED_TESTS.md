# Python Docs Disabled Test Inventory

This file tracks Python docs examples that are present but disabled because the direct port currently fails
with the Python compiler shipped with Micronaut core. Use it as the bug-fixing task list.

## Reconciliation

- Last generated command: `rg -n "@Disabled\(" test-suite-python/src/test/python/micronaut/docs`.
- Last full-suite command: `./gradlew :test-suite-python:test -Ppython-ci`.

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut, JPA and JDBC annotations are imported from their Java package (`jakarta.persistence`,
  `jakarta.transaction`, `micronaut.transaction.annotation`, ...).
- Do not add Java-style getters or setters to Python docs models. Entities are plain classes with typed
  attributes and defaults (Hibernate mutates them), everything else prefers `@dataclass`.
- The injected `DataSource` is transaction aware: connections are obtained inside `@Transactional` methods.
  Tests that need a raw connection (`ALTER USER ...`) unwrap it with `DataSourceResolver`.
- Prefer `@MicronautTest` with injected beans over `ApplicationContext.run()`.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `micronaut.docs.hibernate.session.BookRepositoryTest` | The Python compiler only copies JUnit annotations onto the generated Java class of a Python class (`PythonStubGenerator.ANNOTATION_PACKAGES_TO_COPY`); the `jakarta.persistence` annotations (`@Entity`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@EmbeddedId`, ...) only exist in the Micronaut introspection metadata. Micronaut's entity scan finds the class, but Hibernate rejects it (`UnknownEntityTypeException: 'Book' is not annotated '@Entity'`). The generated class also lacks a no-arg constructor. The `@PersistenceContext` injection and `@Transactional("other")` parts of the example work. |
| `micronaut.docs.hibernate.entityscan.EntityScanTest` | Same cause: `Product` is introspected (`@Introspected(packages=..., includedAnnotations=[Entity])` works) but the `SessionFactory` metamodel does not contain it (`Not an entity`). |
| `micronaut.docs.hibernate.proxies.CompileTimeProxiesTest` | Same cause for `Pet`/`Owner`. Additionally the `@GenerateProxy` introduction methods (`getHibernateLazyInitializer`) of the generated `$Owner$Intercepted` proxy are bridged to the Python object instead of being implemented by the introduction advice (`No Python member [getHibernateLazyInitializer] found`). |
| `micronaut.docs.hibernate.graalvm.EmbeddedIdTest` | Same cause for `Order`/`OrderId` (`Unknown entity type 'micronaut.docs.hibernate.graalvm.Order'`). |

## Commented Unsupported Snippet Ports

None.

## Intentionally Unsupported Snippet Targets

None.
