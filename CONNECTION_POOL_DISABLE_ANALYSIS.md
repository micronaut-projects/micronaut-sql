# Analysis: Disabling Database Connection Pooling in Micronaut SQL

## Executive Summary

This document provides a comprehensive analysis of what would be required to disable database connection pooling in Micronaut SQL, allowing each connection request to create a new database connection and each close operation to actually close the connection rather than returning it to a pool.

**Current State:** Micronaut SQL currently **requires** a connection pool implementation (Hikari, Tomcat, Apache DBCP, or Oracle UCP). There is no built-in mechanism to use unpooled connections.

**Recommendation:** A new module `jdbc-unpooled` or similar would need to be created to provide this functionality.

## Current Architecture

### Connection Pool Implementations

Micronaut SQL currently supports four connection pool implementations:

1. **HikariCP** (`jdbc-hikari` module)
   - Most popular and recommended option
   - High-performance JDBC connection pool
   - Minimal configuration required

2. **Apache Tomcat JDBC Pool** (`jdbc-tomcat` module)
   - Tomcat's connection pool implementation
   - Good performance and reliability

3. **Apache Commons DBCP2** (`jdbc-dbcp` module)
   - Apache's database connection pooling implementation
   - Mature and feature-rich

4. **Oracle Universal Connection Pool (UCP)** (`jdbc-ucp` module)
   - Oracle's connection pool implementation
   - Optimized for Oracle databases

### How It Works Today

All implementations follow a similar pattern:

1. **Configuration Bean**: Each module has a `DatasourceConfiguration` class that extends or wraps the pool's configuration object:
   - `io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration` extends `HikariConfig`
   - `io.micronaut.configuration.jdbc.dbcp.DatasourceConfiguration` extends `BasicDataSource`
   - Similar patterns for Tomcat and UCP

2. **Factory Bean**: A `DatasourceFactory` creates the actual `javax.sql.DataSource` bean:
   - Uses `@EachBean(DatasourceConfiguration.class)` to create one DataSource per configuration
   - All extend `BaseDatasourceFactory` for credential refresh support
   - Returns pooled `DataSource` implementations

3. **Base Module**: The `jdbc` module provides common interfaces and base classes:
   - `BasicJdbcConfiguration` - common configuration interface
   - `BaseDatasourceFactory` - base factory with refresh event handling
   - `CalculatedSettings` - calculates defaults for URL, driver, username, password

4. **Configuration**: Users configure datasources in `application.yml`:
   ```yaml
   datasources:
     default:
       url: jdbc:h2:mem:default
       driverClassName: org.h2.Driver
       username: sa
       password: ""
   ```

### Key Design Decisions

1. **All implementations use pooling** - There is no concept of an unpooled connection
2. **Modular design** - Each pool implementation is a separate optional dependency
3. **Consistent configuration** - All pools share the same base configuration structure
4. **Pool-specific settings** - Each implementation exposes all properties of its underlying pool

## Does Unpooled Connection Support Already Exist?

**No**, there is currently no way to disable connection pooling in Micronaut SQL.

### What Users Might Try (That Won't Work)

1. **Setting pool size to 1**: This still uses a pool, just with one connection
   ```yaml
   datasources:
     default:
       url: jdbc:h2:mem:default
       maximum-pool-size: 1  # Still pooled!
       minimum-idle: 0
   ```

2. **Disabling the datasource**: Setting `datasources.default.enabled: false` disables the entire datasource, not just pooling

3. **Using raw JDBC**: Users could bypass Micronaut's datasource support entirely and use `DriverManager.getConnection()` directly, but this:
   - Loses all Micronaut integration (dependency injection, health checks, metrics)
   - Requires manual connection management
   - Doesn't work with Micronaut Data, Hibernate, jOOQ, etc.

## Why Would Someone Want Unpooled Connections?

Valid use cases include:

1. **Testing**: Some integration tests might want clean connections without pool state
2. **Serverless/FaaS**: Short-lived functions where pool overhead outweighs benefits
3. **Low-volume applications**: Apps making very infrequent database calls
4. **Connection-per-request isolation**: Strict isolation requirements
5. **Debugging**: Simpler debugging without pool complexities
6. **Educational/prototype**: Learning JDBC without pool complexity
7. **Database connection limits**: When dealing with databases that have strict connection limits and you want predictable connection count

## Implementation Approaches

### Option 1: New `jdbc-unpooled` Module (Recommended)

Create a new module that implements a simple, unpooled `DataSource`.

#### Architecture

```
jdbc-unpooled/
├── src/main/java/io/micronaut/configuration/jdbc/unpooled/
│   ├── DatasourceConfiguration.java    # Configuration bean
│   ├── DatasourceFactory.java          # Factory to create UnpooledDataSource
│   ├── UnpooledDataSource.java         # Main DataSource implementation
│   └── package-info.java
└── build.gradle
```

#### Key Components

**UnpooledDataSource.java**
```java
public class UnpooledDataSource implements DataSource {
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final Properties properties;
    
    @Override
    public Connection getConnection() throws SQLException {
        // Load driver if needed
        Driver driver = DriverManager.getDriver(url);
        
        // Create a new connection each time
        Properties props = new Properties(properties);
        if (username != null) props.setProperty("user", username);
        if (password != null) props.setProperty("password", password);
        
        return driver.connect(url, props);
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties props = new Properties(properties);
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        Driver driver = DriverManager.getDriver(url);
        return driver.connect(url, props);
    }
    
    // Other DataSource methods...
}
```

**DatasourceConfiguration.java**
```java
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration implements BasicJdbcConfiguration {
    private final String name;
    private final CalculatedSettings calculatedSettings;
    private Map<String, Object> dataSourceProperties = new HashMap<>();
    
    public DatasourceConfiguration(@Parameter String name) {
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
    }
    
    // Implement BasicJdbcConfiguration methods
    // Similar to existing configurations but simpler (no pool settings)
}
```

**DatasourceFactory.java**
```java
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {
    private final Map<String, UnpooledDataSource> dataSources = new LinkedHashMap<>();
    
    public DatasourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
    }
    
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public DataSource dataSource(DatasourceConfiguration config) {
        UnpooledDataSource ds = new UnpooledDataSource(
            config.getUrl(),
            config.getUsername(),
            config.getPassword(),
            config.getDriverClassName(),
            config.getDataSourceProperties()
        );
        dataSources.put(config.getName(), ds);
        return ds;
    }
    
    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, 
                                               DataSourceCredentials credentials) {
        // Update credentials on the unpooled datasource
        UnpooledDataSource ds = dataSources.get(dataSourceName);
        if (ds != null) {
            ds.updateCredentials(credentials.userName(), credentials.password());
        }
    }
    
    @Override
    @PreDestroy
    public void close() {
        // No cleanup needed for unpooled connections
        dataSources.clear();
    }
}
```

#### Benefits
- ✅ Follows existing Micronaut SQL patterns
- ✅ Full Micronaut integration (DI, health checks, metrics)
- ✅ Works with Micronaut Data, Hibernate, jOOQ, etc.
- ✅ Simple to implement and maintain
- ✅ Users can easily switch between pooled and unpooled

#### Drawbacks
- ❌ Requires new module and maintenance
- ❌ No connection validation/health checking by default
- ❌ No metrics on connection usage
- ❌ Performance impact of creating connections on each request

#### Configuration Example
```yaml
datasources:
  default:
    url: jdbc:h2:mem:default
    driverClassName: org.h2.Driver
    username: sa
    password: ""
    # No pool-specific settings needed
```

Users would add this dependency:
```gradle
runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
```

### Option 2: Pool Configuration with Size=0/1 (Not Recommended)

Extend existing pool implementations to support a special mode that disables pooling.

#### Approach

Add a configuration flag like `pooling-enabled: false` that would:
- Set pool sizes to minimum values
- Configure pools to not cache connections
- Set connection timeout to 0

Example for Hikari:
```yaml
datasources:
  default:
    url: jdbc:h2:mem:default
    pooling-enabled: false  # New flag
    # Would translate to:
    # maximum-pool-size: 1
    # minimum-idle: 0
    # connection-timeout: 0
    # etc.
```

#### Benefits
- ✅ No new module needed
- ✅ Works with existing pools

#### Drawbacks
- ❌ Not truly unpooled - still has pool overhead
- ❌ Behavior varies by pool implementation
- ❌ Misleading configuration (says "no pool" but still has one)
- ❌ May not work well with all pool implementations
- ❌ Still creates pool management threads and structures

**Verdict**: This is not a good solution because it doesn't truly disable pooling.

### Option 3: Wrapper Around Spring's DriverManagerDataSource

Use or adapt Spring's `DriverManagerDataSource` which provides unpooled connections.

#### Approach

Create a thin wrapper module around Spring Framework's JDBC support:

```java
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Factory
public class DatasourceFactory extends BaseDatasourceFactory {
    
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public DataSource dataSource(DatasourceConfiguration config) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(config.getUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName(config.getDriverClassName());
        return ds;
    }
}
```

#### Benefits
- ✅ Leverages tested, production-ready code
- ✅ Faster to implement
- ✅ Spring's DriverManagerDataSource is well-documented

#### Drawbacks
- ❌ Adds Spring Framework dependency to Micronaut (undesirable)
- ❌ Increases binary size unnecessarily
- ❌ Goes against Micronaut's "no Spring" philosophy
- ❌ Spring dependency conflicts could arise

**Verdict**: Not recommended for Micronaut projects due to Spring dependency.

### Option 4: Enhanced Configuration in Base Module

Add unpooled support directly to the `jdbc` base module.

#### Approach

Modify the `jdbc` module to detect when no pool implementation is available and create an unpooled datasource:

```java
// In jdbc module
@Factory
@Requires(missingBeans = DataSource.class)
public class UnpooledDatasourceFactory extends BaseDatasourceFactory {
    // Create unpooled datasource only if no other implementation is present
}
```

#### Benefits
- ✅ Automatic fallback to unpooled when no pool is available
- ✅ No additional dependency needed

#### Drawbacks
- ❌ Adds complexity to base module
- ❌ Makes it harder to explicitly choose unpooled
- ❌ Potential confusion about which implementation is being used
- ❌ Violates separation of concerns

**Verdict**: Could work as a fallback, but explicit module is clearer.

## Recommended Implementation: Option 1 (New Module)

The recommended approach is **Option 1: Create a new `jdbc-unpooled` module**.

### Implementation Steps

1. **Create new Gradle module**
   - Add `jdbc-unpooled` to `settings.gradle`
   - Create `jdbc-unpooled/build.gradle` with dependencies on `jdbc` base module
   - No external pool library dependencies needed

2. **Implement core classes**
   - `UnpooledDataSource`: Simple DataSource implementation using DriverManager
   - `DatasourceConfiguration`: Configuration bean implementing BasicJdbcConfiguration
   - `DatasourceFactory`: Factory extending BaseDatasourceFactory
   - `package-info.java`: Package documentation

3. **Add tests**
   - Basic connection creation test
   - Multiple datasource support test
   - Credential refresh test
   - Integration with Micronaut Data/Hibernate test

4. **Add documentation**
   - New section in `/src/main/docs/guide/jdbc/jdbc-connection-pools.adoc`
   - Document when to use unpooled connections
   - Document configuration
   - Add warnings about performance implications

5. **Add health check support** (optional)
   - Simple validation query execution
   - Compatible with existing health check infrastructure

6. **Add metrics support** (optional)
   - Track connection creation count
   - Track connection failures
   - Compatible with Micrometer integration

### Configuration Details

Users would configure it identically to other datasources:

```yaml
datasources:
  default:
    url: jdbc:postgresql://localhost:5432/mydb
    driverClassName: org.postgresql.Driver
    username: myuser
    password: mypass
    # Optional unpooled-specific settings:
    login-timeout: 30  # Connection timeout
    data-source-properties:
      # Any JDBC driver properties
      ssl: true
      sslmode: require
```

And add the dependency:

```gradle
runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
```

### Technical Considerations

1. **Thread Safety**: The UnpooledDataSource itself is thread-safe (immutable configuration)
2. **Connection Lifecycle**: Each connection is independent; closing truly closes
3. **Driver Management**: Use DriverManager.getDriver() to avoid driver registration issues
4. **Properties Handling**: Support both standard JDBC properties and datasource-specific properties
5. **Error Handling**: Propagate SQLExceptions appropriately
6. **Logging**: Log connection creation/failures for debugging
7. **GraalVM**: Ensure native-image compatibility (no reflection on drivers)

### Compatibility

The unpooled implementation would work with:
- ✅ Micronaut Data JDBC
- ✅ Micronaut Data JPA (Hibernate)
- ✅ jOOQ integration
- ✅ JDBI integration
- ✅ JPA/Hibernate without Micronaut Data
- ✅ Health checks
- ✅ Metrics (with custom implementation)
- ✅ Transaction management
- ✅ Multiple datasources

### Performance Implications

**Important**: Users should be aware of performance impacts:

1. **Connection Overhead**: Creating a new connection is expensive (typically 10-100ms)
2. **Network Handshakes**: Each connection requires network negotiation
3. **Authentication**: Database authentication on each connection
4. **No Connection Validation**: No pool-level health checking
5. **Database Load**: Can overwhelm database with connection requests

**Recommendation**: Document clearly that this should only be used for:
- Development/testing
- Very low-volume applications (< 1 req/sec)
- Serverless/FaaS with cold starts
- Specific isolation requirements

For production applications with normal load, pooling should be used.

### Migration Path

Users could easily migrate between pooled and unpooled:

**From Hikari to Unpooled:**
```diff
dependencies {
-    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
+    runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
}
```

Configuration stays the same (pool-specific settings would be ignored).

**From Unpooled to Hikari:**
```diff
dependencies {
-    runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
+    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
}
```

Add optional Hikari-specific settings:
```yaml
datasources:
  default:
    # ... existing config ...
    maximum-pool-size: 10
    minimum-idle: 5
```

## Documentation Requirements

The following documentation would need to be created/updated:

1. **New Section**: `/src/main/docs/guide/jdbc/jdbc-unpooled.adoc`
   - What is an unpooled connection
   - When to use it
   - When NOT to use it
   - Configuration examples
   - Performance implications

2. **Update**: `/src/main/docs/guide/jdbc/jdbc-connection-pools.adoc`
   - Add unpooled to the table of implementations
   - Link to new documentation

3. **Update**: `/src/main/docs/guide/jdbc.adoc`
   - Mention unpooled as an option

4. **Update**: `/src/main/docs/guide/toc.yml`
   - Add new section to table of contents

5. **JavaDoc**: Comprehensive documentation on all classes
   - Clear warnings about performance
   - Usage examples
   - Configuration options

## Alternative: Configuration Flag Approach (Simpler)

A simpler alternative to a full module would be to add a configuration flag to existing pools:

### In each pool's DatasourceConfiguration:

```java
@ConfigurationProperties("datasources.*.pooling")
public static class PoolingConfiguration {
    private boolean enabled = true;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

### In DatasourceFactory:

```java
@Context
@EachBean(DatasourceConfiguration.class)
@Requires(condition = JdbcDataSourceEnabled.class)
public DataSource dataSource(DatasourceConfiguration config) {
    if (!config.getPooling().isEnabled()) {
        // Return unpooled datasource
        return new UnpooledDataSource(config);
    }
    // Return pooled datasource (existing code)
    return new HikariDataSource(config);
}
```

### Configuration:

```yaml
datasources:
  default:
    url: jdbc:h2:mem:default
    pooling:
      enabled: false  # Disable pooling
```

### Benefits:
- ✅ No new module
- ✅ Works with any pool implementation
- ✅ Easy to toggle

### Drawbacks:
- ❌ Adds complexity to existing modules
- ❌ Need to implement in all 4 pool modules
- ❌ Testing complexity increases
- ❌ Maintenance burden on existing code

## Conclusion

To support unpooled database connections in Micronaut SQL:

1. **Current State**: Not supported - all implementations require connection pools
2. **Recommended Approach**: Create new `jdbc-unpooled` module
3. **Implementation Effort**: Low to Medium
   - ~500-800 lines of code
   - 2-3 days development
   - 1-2 days testing and documentation
4. **Maintenance**: Low (simple codebase, no external dependencies)
5. **User Impact**: Positive for specific use cases, with clear documentation on limitations

The implementation would follow existing Micronaut SQL patterns, integrate seamlessly with the framework, and provide users with a clear choice between pooled and unpooled connections based on their requirements.

## References

- [Micronaut SQL Documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby)
- [Apache DBCP Configuration](https://commons.apache.org/proper/commons-dbcp/configuration.html)
- [JDBC DataSource Javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/javax/sql/DataSource.html)
- Existing Micronaut SQL modules: `jdbc`, `jdbc-hikari`, `jdbc-tomcat`, `jdbc-dbcp`, `jdbc-ucp`
