# Implementation Checklist: Unpooled JDBC Datasource Module

This is a quick reference checklist for implementing the `jdbc-unpooled` module based on the analysis in `CONNECTION_POOL_DISABLE_ANALYSIS.md`.

## Phase 1: Module Setup (1-2 hours)

- [ ] Create `jdbc-unpooled` directory
- [ ] Add `include 'jdbc-unpooled'` to `settings.gradle`
- [ ] Create `jdbc-unpooled/build.gradle`:
  ```gradle
  plugins {
      id 'io.micronaut.build.internal.sql-module'
  }
  
  dependencies {
      api projects.micronautJdbc
      api(mn.micronaut.inject)
      
      testRuntimeOnly(libs.managed.h2)
      testAnnotationProcessor(mn.micronaut.inject.java)
      testImplementation(mn.micronaut.http.server.netty)
  }
  ```
- [ ] Create directory structure: `src/main/java/io/micronaut/configuration/jdbc/unpooled/`
- [ ] Create directory structure: `src/test/groovy/io/micronaut/configuration/jdbc/unpooled/`

## Phase 2: Core Implementation (4-6 hours)

### UnpooledDataSource.java
- [ ] Implement `javax.sql.DataSource` interface
- [ ] Add immutable fields: url, username, password, driverClassName, properties
- [ ] Implement `getConnection()` using `DriverManager.getDriver(url).connect()`
- [ ] Implement `getConnection(String username, String password)` override
- [ ] Implement all required DataSource methods (getLoginTimeout, setLoginTimeout, etc.)
- [ ] Add proper exception handling and logging
- [ ] Add method to update credentials (for refresh support)
- [ ] Add JavaDoc with warnings about performance implications

### DatasourceConfiguration.java
- [ ] Annotate with `@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")`
- [ ] Implement `BasicJdbcConfiguration` interface
- [ ] Add `name` field with `@Parameter` constructor injection
- [ ] Add `CalculatedSettings` field
- [ ] Implement all BasicJdbcConfiguration methods (getName, getUrl, getUsername, etc.)
- [ ] Add `@PostConstruct` to calculate defaults
- [ ] Add optional properties: loginTimeout, dataSourceProperties
- [ ] Add JavaDoc

### DatasourceFactory.java
- [ ] Annotate with `@Factory`
- [ ] Extend `BaseDatasourceFactory`
- [ ] Add `Map<String, UnpooledDataSource>` to track datasources
- [ ] Implement constructor with `ApplicationContext` parameter
- [ ] Add `@Context @EachBean(DatasourceConfiguration.class)` dataSource() method
- [ ] Implement `dataSourceCredentialsChanged()` for credential refresh
- [ ] Implement `@PreDestroy close()` method
- [ ] Add proper logging
- [ ] Add JavaDoc

### package-info.java
- [ ] Add package documentation
- [ ] Include usage examples
- [ ] Document performance implications
- [ ] Add since version tag

## Phase 3: Testing (6-8 hours)

### Unit Tests
- [ ] `UnpooledDataSourceSpec.groovy` - Test connection creation
- [ ] Test with H2 in-memory database
- [ ] Test connection actually opens and closes
- [ ] Test credential handling
- [ ] Test properties propagation
- [ ] Test exception handling

### Integration Tests
- [ ] `DatasourceFactorySpec.groovy` - Test factory creation
- [ ] Test single datasource creation
- [ ] Test multiple datasources
- [ ] Test datasource injection
- [ ] Test with `@MicronautTest`

### Configuration Tests
- [ ] Test with minimal configuration (only URL)
- [ ] Test with full configuration
- [ ] Test calculated settings (auto-detect driver, etc.)
- [ ] Test `datasources.default.enabled=false`

### Credential Refresh Tests
- [ ] Test username change
- [ ] Test password change
- [ ] Test both username and password change
- [ ] Verify new connections use new credentials

### Compatibility Tests (optional but recommended)
- [ ] Test with Micronaut Data JDBC
- [ ] Test with JPA/Hibernate
- [ ] Test with jOOQ
- [ ] Test with transaction management

## Phase 4: Documentation (3-4 hours)

### JavaDoc
- [ ] Review and complete all JavaDoc comments
- [ ] Add examples to class-level docs
- [ ] Add `@since` tags with correct version
- [ ] Add performance warnings where appropriate

### User Documentation
- [ ] Create `src/main/docs/guide/jdbc/jdbc-unpooled.adoc`:
  - [ ] Introduction section
  - [ ] When to use / when not to use
  - [ ] Configuration examples
  - [ ] Performance implications (with measurements if possible)
  - [ ] Migration guide (to/from pooled)
  - [ ] Troubleshooting section
- [ ] Update `src/main/docs/guide/jdbc/jdbc-connection-pools.adoc`:
  - [ ] Add unpooled to implementations table
  - [ ] Link to new documentation
- [ ] Update `src/main/docs/guide/jdbc.adoc`:
  - [ ] Mention unpooled as option
  - [ ] Add dependency example
- [ ] Update `src/main/docs/guide/toc.yml`:
  - [ ] Add jdbc-unpooled section

### README/Examples
- [ ] Add example configuration to relevant READMEs
- [ ] Consider adding example project in `examples/` directory

## Phase 5: Quality Checks (2-3 hours)

### Code Quality
- [ ] Run Checkstyle: `./gradlew -q cM`
- [ ] Fix any checkstyle violations
- [ ] Run Spotless: `./gradlew -q spotlessCheck`
- [ ] Fix any spotless violations with `./gradlew -q spotlessApply`
- [ ] Review code for nullability annotations (@Nullable, @NonNull)
- [ ] Ensure no reflection usage (GraalVM compatibility)

### Build & Test
- [ ] Compile module: `./gradlew -q :jdbc-unpooled:compileJava`
- [ ] Compile tests: `./gradlew -q :jdbc-unpooled:compileTestGroovy`
- [ ] Run tests: `./gradlew :jdbc-unpooled:test`
- [ ] Run integration tests if created
- [ ] Check binary compatibility: `./gradlew japiCmp` (should be clean for new module)

### Documentation Build
- [ ] Build docs: `./gradlew docs`
- [ ] Verify docs at `build/docs/`
- [ ] Check for broken links
- [ ] Review formatting and examples

## Phase 6: Optional Enhancements (3-5 hours each)

### Health Check Support
- [ ] Create `UnpooledDataSourceHealthIndicator`
- [ ] Implement validation query execution
- [ ] Test health indicator with actuator
- [ ] Add documentation

### Metrics Support
- [ ] Create metrics beans for connection tracking
- [ ] Track connection creation count
- [ ] Track connection failures
- [ ] Track connection creation time
- [ ] Integrate with Micrometer
- [ ] Test metrics collection
- [ ] Add documentation

### Advanced Configuration
- [ ] Support for connection properties
- [ ] Support for custom driver properties
- [ ] Support for SSL/TLS configuration
- [ ] Add validation of configuration

## Phase 7: Final Review (1-2 hours)

### Code Review Checklist
- [ ] All public APIs documented
- [ ] No TODOs or FIXMEs in code
- [ ] Consistent naming conventions
- [ ] Error messages are clear and helpful
- [ ] Logging is appropriate (not too verbose, not too quiet)
- [ ] Thread safety verified
- [ ] Resource cleanup verified (no leaks)

### Testing Review
- [ ] All critical paths tested
- [ ] Edge cases covered
- [ ] Error conditions tested
- [ ] Test coverage > 80%
- [ ] No flaky tests

### Documentation Review
- [ ] All features documented
- [ ] Examples are correct and tested
- [ ] Performance implications clearly stated
- [ ] Migration path documented
- [ ] Troubleshooting tips included

## Estimated Total Time

- **Minimum Implementation**: 16-21 hours
  - Core implementation + basic tests + basic docs
  
- **Complete Implementation**: 25-35 hours
  - Core + comprehensive tests + complete docs + health checks + metrics

## Success Criteria

The implementation is complete when:

1. ✅ Module compiles without errors
2. ✅ All tests pass
3. ✅ Checkstyle and Spotless checks pass
4. ✅ Documentation builds without errors
5. ✅ Can create unpooled datasource with minimal config
6. ✅ Can execute JDBC queries through the datasource
7. ✅ Connections are truly created and closed (not pooled)
8. ✅ Works with Micronaut Data/Hibernate/jOOQ
9. ✅ Credential refresh works
10. ✅ Performance implications are clearly documented

## Quick Start After Implementation

Users would use it like this:

```gradle
dependencies {
    runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
    runtimeOnly("com.h2database:h2")  // or any JDBC driver
}
```

```yaml
datasources:
  default:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password: ""
```

```java
@Inject
DataSource dataSource;

public void query() {
    try (Connection conn = dataSource.getConnection()) {
        // Each getConnection() creates a new connection
        // Each close() actually closes the connection
    }
}
```

## Notes

- This is a **new module**, so binary compatibility checks don't apply
- Keep implementation simple - don't over-engineer
- Focus on clarity and maintainability
- Document performance implications heavily
- Consider this a "special use case" module, not a replacement for pools
