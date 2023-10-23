# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.4.2 - 2023-10-23

### Fixed

- Force update to `org.json/json` to version `20231013`, resolves CVE-2023-5072

## 0.4.1 - 2023-03-02

### Fixed

- Force update to `org.json/json` to version `20230227`, resolves CVE-2022-45688

## 0.4.0 - 2022-10-03

- Added Custom Format Validators (thanks to @ModestasPeciokas)
- Updated dependencies

For an example on how to use the Custom Format Validators, check the unit tests.

## 0.3.4 - 2022-05-12

- Updated Clojure -> 1.11.1
- Updated other dependencies

## 0.3.3 - 2022-01-05

- Fixed issue when EDN input to be validated contains lists
- Updated Java Library

## 0.3.2 - 2021-09-15

- Eliminated reflective function calls
- Updated Java Library

## 0.3.1 - 2021-04-25

Now relative $ref's are supported.

## 0.3.0 - 2021-04-15

This release adds usage of $ref pointing to resources in the classpath.

## 0.2.9 - 2020-12-03

### Changed

- Significant speed-up when performing batch validation

## 0.2.8 - 2020-09-09

### Added

- Infer: optional parameter `additional-props` allow additional properties for objects described by generated schema

## 0.2.7 - 2020-06-24

### Fixed
- Bug when nested siblings had different optionality

## 0.2.6 - 2020-06-24

### Added
- Optionality by nullability when inferring schemas

## 0.2.5 - 2020-06-24

### Added
- Schemas can now be inferred on multiple input documents, thus supporting implicit optionality (while still supporting the explicit optionality from version 0.2.2).

### Removed
- deprecated `infer-strict->json`

## 0.2.4 - 2020-05-29

### Changed
- Updated Java library

## 0.2.3 - 2020-05-06

### Fixed
- removed `println`s

## 0.2.2 - 2020-05-06

### Changed
- API to infer schema has been changed! Check README and `infer_test.clj`
  - schema-name -> title
- Added `optional` parameter, for keys that should NOT be required by schema
  - This doesn't support paths/nesting, only simple keys. Hence a key marked as optional will be optional everywhere in the schema

## 0.2.1 - 2020-04-30

- Optional parameter to include original validation exception: :include-original-exception

## 0.2.0 - 2020-04-23

### Added

- infer JSON Schema based on EDN data

## 0.1.9 - 2020-04-21

### Changed
- cheshire 5.9.0 -> 5.10.0

## 0.1.8 - 2019-09-04

### Changed
- Original exception included when validation fails

## 0.1.7 - 2019-08-21

### Changed
- Updated depencies (clojure, cheshire)

## 0.1.6 - 2019-05-11

### Fixed
- EDN based array (vector) in root failed validation (issue #3)

## 0.1.5 - 2019-05-10

### Added
- API to prepare JSON Schema for reuse
  (better performance when using same Schema to validate multiple JSON documents)

## 0.1.4 - 2019-04-29

### Changed
- Updated Java library
- Changed to Apache License, Version 2.0

## 0.1.3 - 2019-03-12

### Changed
- BREAKING: valid entries return the entries instead of `nil`, also to ease pipelining
- Error messages now show error count

## 0.1.2 - 2019-03-07

### Changed
- BREAKING: re-ordered params for `validate` fn, to ease pipelining

## 0.1.1 - 2019-03-01

### Changed
- Updated Java library

## 0.1.0 - 2019-01-07

### Added
- Initial public release
