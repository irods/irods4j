# irods4j

**IMPORTANT: This project is not stable yet and may experience changes.**

A Java client library designed for iRODS 4.3.2+.

This library provides a low-level API which exposes data types and functions seen in the iRODS C API. A high-level API is provided to ease use as well.

## Features

- Supports SSL/TLS for secure communication
- Configurable socket options
- Minimum use of abstractions
- Easy to use / Easy to understand
- Enables patterns supported by the iRODS C/C++ APIs
- Supports PAM via the `pam_password` authentication scheme

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

**IMPORTANT: Not all API operations are supported by the high-level APIs.**

For those users needing more control, see [IRODSApi.java](src/main/java/org/irods/irods4j/low_level/api/IRODSApi.java) for the available low-level APIs. The low-level APIs provided by this library mirror the design of the C API.

For proper usage, see the implementation of various high-level APIs and their C counterparts.

See the [low-level unit tests](src/test/java/org/irods/irods4j/low_level) for usage examples.

## Reporting Security Vulnerabilities

See [SECURITY.md](SECURITY.md) for details.
