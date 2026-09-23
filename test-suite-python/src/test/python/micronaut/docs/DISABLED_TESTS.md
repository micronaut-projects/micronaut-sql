# Python Docs Disabled Test Inventory

This file tracks Python docs examples that are present but disabled because the direct port currently fails
with the Python compiler shipped with Micronaut core, and the snippet targets that are deliberately not
ported to Python. Use it as the bug-fixing task list.

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" test-suite-python/src/test/python/micronaut/docs`.
- Last full-suite command: `./gradlew :test-suite-python:test -Ppython-ci --max-workers=1`.
- Last full-suite result (micronaut-core 5.2.3, micronaut-build 8.1.2): build successful, 4 tests, 0 skipped, 0 failures
  (`tests/mybatis-python`: 1 test, 0 failures).

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut, transaction and JDBC annotations are imported from their Java package
  (`jakarta.transaction`, `micronaut.transaction.annotation`, ...).
- The injected `DataSource` is transaction aware: connections are obtained inside `@Transactional` methods.
  Tests that need a raw connection (`ALTER USER ...`) unwrap it with `DataSourceResolver`.
- Prefer `@MicronautTest` with injected beans over `ApplicationContext.run()`.

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Intentionally Unsupported Snippet Targets

| Target | Reason |
| --- | --- |
| The whole `io.micronaut.docs.hibernate` package - `entityscan.Application`, `proxies.Owner`, `proxies.Pet`, `graalvm.OrderId`, `graalvm.Order`, `session.BookRepository` (`hibernate-entity-scan.adoc`, `hibernate-graalvm.adoc`, `hibernate-proxies.adoc`, `hibernate-inject-session.adoc`, `languages="java,kotlin,groovy"`) | The Hibernate examples are intentionally not ported to Python. The id Hibernate generates on `entity_manager.persist(entity)` is written to the Java wrapper of the Python object (the generated class holds the mapped fields) and is not written back to the Python object, so `entity.id` stays `None` after a save and the lazy-loading and round-trip assertions of the examples cannot be expressed. The `@PersistenceContext` injection, `@Transactional(...)`, `@GenerateProxy` and the entity mapping itself all work; only the generated-id write-back does not. The core change that would have mirrored the wrapper state back onto the Python object was evaluated and rejected as too intrusive, so the Hibernate pages stay Java/Kotlin/Groovy. The JDBC examples, which do not depend on generated ids being visible on the Python object, are ported and enabled. |
