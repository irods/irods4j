# irods4j

> [!IMPORTANT]
> This project is not stable yet and may experience changes. Use of this library is encouraged.

A Java 17 client library designed for iRODS 4.3.2+.

This library provides a low-level API which exposes data types and functions seen in the iRODS C API. A high-level API is provided to ease use as well.

## Features

- Supports SSL/TLS for secure communication
- Configurable socket options
- Minimum use of abstractions
- Easy to use / Easy to understand
- Enables patterns supported by the iRODS C/C++ APIs
- Supports PAM via the `pam_password` and `pam_interactive` authentication schemes

## Maven

The library is available via the [Central Maven Repository](https://central.sonatype.com).

To use it, add the following to your pom.xml file.

```xml
<dependency>
    <groupId>org.irods</groupId>
    <artifactId>irods4j</artifactId>
    <version>0.6.0</version>
</dependency>
```

> [!NOTE]
> A Java 8 compatible implementation of the library is also available.
>
> See the [java8 branch](https://github.com/irods/irods4j/tree/java8) of this repository for the maven dependency coordinates.

## High-Level APIs

See the [high-level unit tests](src/test/java/org/irods/irods4j/high_level) for usage examples.

### Connection APIs

- IRODSConnection
- IRODSConnectionPool

### Administration APIs

- IRODSResources
- IRODSTickets
- IRODSUsers
- IRODSZones

### Virtual Filesystem APIs

- IRODSCollectionIterator
- IRODSFilesystem
- IRODSRecursiveCollectionIterator
- IRODSReplicas

### Query APIs

- IRODSQuery

### I/O APIs

- IRODSDataObjectStream
- IRODSDataObjectInputStream
- IRODSDataObjectOutputStream

### Metadata APIs

- IRODSMetadata

### Rule Execution APIs

- IRODSRules

## Low-Level APIs

> [!IMPORTANT]
> Not all API operations are supported by the high-level APIs.

For those users needing more control, see [IRODSApi.java](src/main/java/org/irods/irods4j/low_level/api/IRODSApi.java) for the available low-level APIs. The low-level APIs provided by this library mirror the design of the C API.

For proper usage, see the implementation of various high-level APIs and their C counterparts.

See the [low-level unit tests](src/test/java/org/irods/irods4j/low_level) for usage examples.

## Reporting Security Vulnerabilities

See [SECURITY.md](SECURITY.md) for details.
