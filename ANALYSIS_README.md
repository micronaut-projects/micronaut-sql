# Unpooled Database Connection Analysis

This directory contains the analysis and implementation guide for adding support for unpooled (non-pooled) database connections to Micronaut SQL.

## 📋 Documents

### 1. [CONNECTION_POOL_DISABLE_ANALYSIS.md](./CONNECTION_POOL_DISABLE_ANALYSIS.md)
**Comprehensive technical analysis** of the current state and proposed solution.

**Contents:**
- Current architecture overview
- Analysis of existing pool implementations (Hikari, Tomcat, DBCP, UCP)
- Answer to: "Does unpooled support already exist?" (Spoiler: No)
- 4 implementation approaches evaluated with pros/cons
- Recommended solution with detailed design
- Code examples and architecture diagrams
- Performance implications and use cases
- Documentation requirements

**Target Audience:** Technical decision makers, architects, senior developers

### 2. [IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md)
**Step-by-step implementation guide** for building the `jdbc-unpooled` module.

**Contents:**
- 7 implementation phases with time estimates
- Detailed task checklists
- Code quality verification steps
- Testing strategy
- Documentation requirements
- Success criteria

**Target Audience:** Developers implementing the feature

## 🎯 Quick Summary

### The Question
> "Can I disable database connection pooling in Micronaut SQL? I want every connection to be opened and closed, not returned to a pool."

### The Answer
**No, this is not currently possible.** Micronaut SQL requires one of four connection pool implementations:
- HikariCP (`jdbc-hikari`)
- Apache Tomcat Pool (`jdbc-tomcat`)
- Apache Commons DBCP2 (`jdbc-dbcp`)
- Oracle UCP (`jdbc-ucp`)

### The Solution
Create a new `jdbc-unpooled` module that provides unpooled connections using standard JDBC `DriverManager`.

### Key Features
- ✅ Each `getConnection()` creates a new database connection
- ✅ Each `close()` actually closes the connection
- ✅ Full Micronaut integration (DI, health checks, metrics)
- ✅ Works with Micronaut Data, Hibernate, jOOQ, etc.
- ✅ Easy to switch between pooled and unpooled

### Implementation Effort
- **Code:** ~500-800 lines
- **Time:** 2-3 days development + 1-2 days testing/docs
- **Complexity:** Low to Medium

## 🎓 When to Use Unpooled Connections

### ✅ Good Use Cases
1. **Testing** - Clean connection state between tests
2. **Serverless/FaaS** - Short-lived functions
3. **Low-volume applications** - < 1 request/second
4. **Learning/prototyping** - Simplicity over performance
5. **Strict isolation** - Connection-per-request requirements

### ❌ Bad Use Cases (Use Pooling Instead)
1. **Production applications** - Normal or high load
2. **High-performance requirements** - Connection creation is expensive (10-100ms)
3. **High-volume applications** - > 1 request/second
4. **Microservices** - Multiple concurrent requests

## 🚀 Quick Start (After Implementation)

### 1. Add Dependency
```gradle
dependencies {
    runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
    runtimeOnly("com.h2database:h2")  // or your JDBC driver
}
```

### 2. Configure
```yaml
datasources:
  default:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password: ""
```

### 3. Use
```java
@Inject
DataSource dataSource;

public void query() {
    try (Connection conn = dataSource.getConnection()) {
        // New connection created here
        // ... use connection ...
    } // Connection actually closed here (not returned to pool)
}
```

### 4. Migrate Between Pooled/Unpooled

**From HikariCP to Unpooled:**
```diff
- runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
+ runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
```

**From Unpooled to HikariCP:**
```diff
- runtimeOnly("io.micronaut.sql:micronaut-jdbc-unpooled")
+ runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
```

Configuration stays the same! Just change the dependency.

## 📊 Implementation Approaches Evaluated

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **New `jdbc-unpooled` module** | Clean separation, follows patterns, maintainable | New module to maintain | ✅ **Recommended** |
| Pool config with size=0/1 | No new module | Not truly unpooled, still has overhead | ❌ Not recommended |
| Spring DriverManagerDataSource wrapper | Fast to implement | Adds Spring dependency | ❌ Not recommended |
| Config flag in existing pools | Works with any pool | Increases complexity | ❌ Not recommended |

## ⚠️ Performance Impact

Unpooled connections are **significantly slower** than pooled:

| Metric | Pooled (HikariCP) | Unpooled |
|--------|-------------------|----------|
| Connection acquisition | ~1ms | ~10-100ms |
| Network handshake | Once (pooled) | Every time |
| Authentication | Once (pooled) | Every time |
| Overhead per request | Minimal | High |

**Recommendation:** Only use unpooled connections for the specific use cases listed above.

## 🔗 Related Resources

- [Micronaut SQL Documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide)
- [JDBC DataSource API](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/javax/sql/DataSource.html)
- [HikariCP (recommended pool)](https://github.com/brettwooldridge/HikariCP)

## 📝 Status

**Analysis Status:** ✅ Complete  
**Implementation Status:** ⏳ Not started (analysis only)

This analysis was created in response to the question about disabling connection pooling. No code changes have been made - this is analysis and planning only.

## 🤝 Next Steps

If you decide to implement this feature:

1. Review the [detailed analysis](./CONNECTION_POOL_DISABLE_ANALYSIS.md)
2. Follow the [implementation checklist](./IMPLEMENTATION_CHECKLIST.md)
3. Create the `jdbc-unpooled` module
4. Submit for review with tests and documentation

## 📞 Questions?

If you have questions about this analysis or implementation:
- Review the detailed documents linked above
- Check existing Micronaut SQL modules for implementation patterns
- Consult the Micronaut community

---

**Generated:** 2025-12-10  
**Branch:** `copilot/investigate-disable-connection-pool`  
**Purpose:** Analysis only - no code changes
