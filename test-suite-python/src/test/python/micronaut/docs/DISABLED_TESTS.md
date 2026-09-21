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
- The JPA entities are plain `@Entity` classes; `test-suite-python/build.gradle` passes
  `-Amicronaut.introspection.allowReflection=micronaut.docs.hibernate.*` to the Python compiler
  (`micronautBuild.python.compilerArgs`) so that the generated classes carry the annotations Hibernate reads reflectively.
- A `java.lang.Class` returned by Java (`entity(Product).getJavaType()`) is compared with the Python class by name.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `micronaut.docs.hibernate.session.BookRepositoryTest` | The id Hibernate assigns to a Python entity on `entity_manager.persist(book)` is set on the Java wrapper of the Python object (the generated class copies the attributes into its fields) and is not written back to the Python object: `book.id` stays `None` after `save`, and the wrapper returned by the `@Transactional` bean is a new copy without the id. `entity_manager.merge(book)`, which returns the managed wrapper, does carry the id. The `@PersistenceContext` injection, `@Transactional("other")` and the entity mapping itself work. |
| `micronaut.docs.hibernate.proxies.CompileTimeProxiesTest` | Same cause: `pet.id` is `None` after `persist`, so the lazy-loading assertions cannot look the pet up (`IllegalArgumentException: Identifier may not be null`). The `@GenerateProxy` proxy of the Python `Owner` entity itself is generated (the same annotation works in the Micronaut Data Hibernate example). |

## Commented Unsupported Snippet Ports

None.

## Intentionally Unsupported Snippet Targets

None.
