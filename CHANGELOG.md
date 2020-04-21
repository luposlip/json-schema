# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

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

[Unreleased]: https://github.com/luposlip/json-schema/compare/0.1.9...HEAD
[0.1.9]: https://github.com/luposlip/json-schema/compare/0.1.8...0.1.9
[0.1.8]: https://github.com/luposlip/json-schema/compare/0.1.7...0.1.8
[0.1.7]: https://github.com/luposlip/json-schema/compare/0.1.6...0.1.7
[0.1.6]: https://github.com/luposlip/json-schema/compare/0.1.5...0.1.6
[0.1.5]: https://github.com/luposlip/json-schema/compare/0.1.4...0.1.5
[0.1.4]: https://github.com/luposlip/json-schema/compare/0.1.3...0.1.4
[0.1.3]: https://github.com/luposlip/json-schema/compare/0.1.2...0.1.3
[0.1.2]: https://github.com/luposlip/json-schema/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/luposlip/json-schema/compare/0.1.0...0.1.1

