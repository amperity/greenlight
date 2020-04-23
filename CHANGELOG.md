Change Log
==========

All notable changes to this project will be documented in this file, which
follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

...


## [0.4.0] - 2020-04-23

### Added
- Prompt for retries if a step fails.


## [0.3.0] - 2020-04-18

### Changed
- Update dependencies to latest versions.
- Exclude `clojure` from `lein-codox` dependencies so that it will actually
  build.

### Fixed
- JUnit test output now properly adds `<failure>` child objects of `<testcase>`
  objects.


## [0.2.0] - 2020-03-25

### Fixed
- JUnit test output now reports `:timeout`s as errors.


## [0.1.7] - 2019-05-23

### Added
- Added the ability to use component systems other Stuart Sierra component
  via a `runner/ManagedSystem` protocol.


## [0.1.6] - 2019-04-26

### Fixed
- Clojure test failure output is collocated with Greenlight failure reporting


## [0.1.5] - 2019-04-25

### Added
- Expands test `:only` options to specify a single namespace for excution
- Adds support for a test `--parallel` option to run tests in parallel


## [0.1.4] - 2019-02-14

### Added
- Make docstring optional for `deftest` (closes #13)
- Make assertion outcomes extensible


## [0.1.3] - 2019-02-01

### Added
- Added a default for a step title defined with `defstep`
- Added the ability to define a step with `defstep` omitting the docstring


## [0.1.2] - 2018-11-13

### Added
- Added the ability to specify a namespace for execution with an `:only`
  argument on test and info commands


## [0.1.1] - 2018-11-06

### Added
- Add the ability to specify a test's `title` as a function of the context, in
  addition to as a string.


## 0.1.0 - 2018-07-01

Initial project release


[Unreleased]: https://github.com/amperity/greenlight/compare/0.4.0...HEAD
[0.4.0]: https://github.com/amperity/greenlight/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/amperity/greenlight/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/amperity/greenlight/compare/0.1.7...0.2.0
[0.1.7]: https://github.com/amperity/greenlight/compare/0.1.6...0.1.7
[0.1.6]: https://github.com/amperity/greenlight/compare/0.1.5...0.1.6
[0.1.5]: https://github.com/amperity/greenlight/compare/0.1.4...0.1.5
[0.1.4]: https://github.com/amperity/greenlight/compare/0.1.3...0.1.4
[0.1.3]: https://github.com/amperity/greenlight/compare/0.1.2...0.1.3
[0.1.2]: https://github.com/amperity/greenlight/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/amperity/greenlight/compare/0.1.0...0.1.1
