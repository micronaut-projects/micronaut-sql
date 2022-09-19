# Micronaut SQL

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.sql/micronaut-sql-bom.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.sql%22%20AND%20a:%22micronaut-sql-bom%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-sql/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-sql/actions)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

Projects to support SQL Database access in Micronaut

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide) for more information.

## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://oss.jfrog.org/artifactory/oss-snapshot-local/) using [Github Actions](https://github.com/micronaut-projects/micronaut-sql/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-sql/actions).

A release is performed with the following steps:

- [Edit the version](https://github.com/micronaut-projects/micronaut-sql/edit/master/gradle.properties) specified by `projectVersion` in `gradle.properties` to a semantic, unreleased version. Example `1.0.0`
- [Create a new release](https://github.com/micronaut-projects/micronaut-sql/releases/new). The Git Tag should start with `v`. For example `v1.0.0`.
- [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-sql/actions?query=workflow%3ARelease) to check it passed successfully.
- Celebrate!
