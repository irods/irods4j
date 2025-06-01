# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project **only** adheres to the following _(as defined at [Semantic Versioning](https://semver.org/spec/v2.0.0.html))_:

> Given a version number MAJOR.MINOR.PATCH, increment the:
> 
> - MAJOR version when you make incompatible API changes
> - MINOR version when you add functionality in a backward compatible manner
> - PATCH version when you make backward compatible bug fixes

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
