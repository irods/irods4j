# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project **only** adheres to the following _(as defined at [Semantic Versioning](https://semver.org/spec/v2.0.0.html))_:

> Given a version number MAJOR.MINOR.PATCH, increment the:
> 
> - MAJOR version when you make incompatible API changes
> - MINOR version when you add functionality in a backward compatible manner
> - PATCH version when you make backward compatible bug fixes

## [0.8.0] - 2026-11-06

> [!IMPORTANT]
> This release introduces breaking changes and will require modifications to client code. See **Changed** section for details.

This release adds support for the new **irods** authentication scheme and passive logical quotas. These features require a minimum server version of 5.1.0.

This release also reworks authentication interfaces to accept more options, exposes tokens for reauthentication, adds support for proxy users to the connection pool, and adds convenience functions for correctly preparing arguments for the authentication plugins.

### Security

- Bump log4j version to 2.26.1 (#171).

### Changed

- Default construct `User`/`Group` instances with empty strings (#178).
- Rework `rcAuthenticateClient` function to accept more authentication plugin options (#185).
- Rework authentication function of `IRODSConnection` to accept more authentication plugin options (#185).

### Fixed

- Fix name of low level RegDataObj API function (#180).
- Do not send password on first network request during native authentication (#188).
- Honor `count` parameter in obfuscation support function `copyByteArrayToIntArray` (#192).

### Added

- Add support for new irods authentication scheme (#139).
- Add support for initializing connection pool with proxy user (#160). 
- Add support for passive logical quotas API endpoint (#174).
- Add missing iRODS error codes (#175).
- Add convenience functions to ease preparation of arguments for authentication plugins (#185, #187).
- Expose functions for obfuscation (#187).
- Add support for GetLimitedPassword API endpoint (#187).
- Add support for TTL to native authentication plugin (#187).
- Expose authentication tokens for pam_password authentication plugin (#187).
- Expose authentication tokens for pam_interactive authentication plugin (#187).
- Add single argument constructor to `User` class for local zone usage (#196).

## [0.7.0] - 2026-07-29

This release fixes a bug that caused the connected iRODS agent to crash during a TLS-enabled server redirect. It also improves handling of single quotes in logical paths.

### Security

- Bump log4j version to 2.25.4 (#145).

### Changed

- `LogicalPath#parentPath`: Return empty string when path does not contain a forward slash and is not absolute (#150).
- `IRODSFilesystem`: Escape single quotes in logical paths before sending API request to server (#150).
- `IRODSReplicas`: Escape single quotes in logical paths before sending API request to server (#150).

### Fixed

- Fix handling of `SHARED_SECRET` payload for TLS (#142).
- Mirror behavior of `procApiRequest` in authentication flow method (#144).
- `IRODSReplicas`: Fix emptiness checks for GenQuery2 (#151).

### Added

- Add function for hex-escaping single quotes in logical paths (#150).

## [0.6.0] - 2025-12-04

This release expands support for secure communication and improves retrieval of permission information when using the high-level filesystem API.

### Changed

- Increase maximum number of data-object permissions returned by `IRODSFilesystem#status` from 250 to 10000 (#131).

### Fixed

- Fix `IRODSFilesystem#status`'s handling of data objects which have no permissions (#132).

### Added

- Add support for secure communication via JSSE `TrustManager`s (#130).

## [0.5.0] - 2025-10-15

This release makes it so that users can retrieve the checksum of a data object while iterating over a collection.

### Changed

- Tweak log level needed to view authentication messages (#24).
- Expose checksum through `CollectionEntry` class (#126).

## [0.4.0] - 2025-09-17

This is a patch release. No changes have been made to the public API.

### Fixed

- Fix support for strings containing multibyte characters (#120).

## [0.3.0] - 2025-08-13

This release adds support for the PAM Interactive authentication scheme. It also fixes some compatibility issues with Microsoft Windows.

### Changed

- Require authentication plugin instance be passed to `AuthManager` instead of string (#2).

### Fixed

- Close TCP socket on `rcConnect` failures (#17).
- Pass correct user type to the server when creating new users (#30).
- Fix path handling code for Microsoft Windows (#104).
- Fix `IRODSCollectionIterator` to not show multiple replicas for a data object (#107).
- Check for correct authentication flow string (#110).

### Added

- Implement support for PAM Interactive authentication scheme (#2).
- Introduce generic path manipulation library (#104).

## [0.2.0] - 2025-06-02

This release focuses on making the library compatible with iRODS 5 and fixing various bugs.

### Changed

- Use legacy permission names to avoid issues with federation (#71).
- Update `IRODSDataObjectStream#read` signature to take byte buffer instead of `ByteArrayReference` (#74).
- Use `group` keyword to create groups in iRODS 4.3.4 and later (#75).
- Update use of GenQuery2 for changes in `DISTINCT` keyword (#79).
- Add symbolic names for all iRODS keywords (#80).
- Update `rcGetMiscSvrInfo` to support TLS payload (#81).
- Add symbolic names for new iRODS error codes (#82).
- Rename `SYS_SIGNED_SID_NOT_MATCHED` error code to `ZONE_KEY_SIGNATURE_MISMATCH` (#83).
- Avoid unnecessary network call when previous read operation does not fill buffer (#84).

### Fixed

- Convert byte to unsigned byte to meet `InputStream#read` requirements (#85).
- Initialize `KeyValPair_PI` before use (#86).
- Fix bugs found in high-level APIs (#87).
- Update input/output streams for secure communication (#92).
- `rcDisconnect`: Shutdown TLS socket properly (#94).
- `rcDisconnect`: Flush output stream (#95).

### Added

- Add utility function for comparing version numbers (#75).
- Add support for iRODS 5 access time (#76).

## [0.1.0] - 2025-02-03

This is the first release of the irods4j library.

The library is designed for servers running iRODS 4.3.2 and later.

The library is not stable yet, but use and experimentation is encouraged.
