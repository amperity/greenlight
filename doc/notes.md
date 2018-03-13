Integration Testing Framework
=============================

## Goals

- Make it easy to write a test harness for a specific service to run in GoCD.
- Test 'steps' must be reusable and composable.
- Leverage existing assertion framework in `clojure.test`?
- Can be used to test multiple services together.
- Ideally, produce a nice report at the end about the runs performed and each
  step execution.


## Model

Integration tests are broken up into high-level 'runs' which are an end-to-end
validation of a particular use-case or interaction pattern. Runs _should_ always
clean up after themselves. (Note: this means additionally recording the
necessary context to disk in case the process crashes or is aborted.)

Each run has one or more _steps_, which are the building blocks of the tests.
A given step may be re-used in multiple runs to configure a part of the system a
certain way or to validate the behavior of some feature. For example, a common
step might be "create a testing tenant".

Each step takes a _component system_ and a _context map_ as input and returns an
updated context map for the next step. Steps should ideally be configurable
enough to draw their inputs from different keys in the context map.

The _test runner_ should handle the system setup, invocation of each step in
sequence, recording of metrics, final state cleanup, and system shutdown. There
should be a _clean-only_ mode which starts the system, loads a context snapshot,
and skips to the cleanup phase at the end.

If any step fails, remaining steps should be skipped. Registration of things
like metrics and cleanup information should be protected from modification by
downstream steps, so some data segregation is needed.
