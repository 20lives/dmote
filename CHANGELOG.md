# Change Log
This log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/). It picks up from DMOTE
version 0.2.0, thus covering only a fraction of the project’s history.

## [Unreleased]
### Changed
- Moved some options:
    - Moved `case` → `rear-housing` → `offsets` into
      `case` → `rear-housing` → `position`.
    - Moved `case` → `rear-housing` → `distance` into
      `case` → `rear-housing` → `position` → `offsets` as `south`.
- Renamed the ‘finger’ key cluster to ‘main‘.
- Removed `create-models.sh`, adding equivalent functionality to the Clojure
  application itself (new flags: `--render`, `--renderer`).
- Added intermediate `scad` and `stl` folders under `things`.
- Split generated documentation (options.md) into three separate documents.

### Added
- This log.
- Support for naming your key clusters much more freely, and/or adding
  additional clusters. Even the new ‘main’ cluster is optional.
    - Support for a `cluster` parameter to `case` → `rear-housing` →
      `position`. The rear housing would previously be attached to the finger
      cluster.
    - Support for a `cluster` parameter to `case` → `leds` → `position`.
      LEDs would previously be attached to the finger cluster.
    - Support for anchoring any cluster to any other, within logical limits.
- Support for a filename whitelist in the CLI.

### Developer
- Started migration to docstring-first function definitions.
- Rearranged derived parameter structure somewhat to support arbitrary key
  clusters.
- Removed the `new-scad` function without replacement.
- Shifted more heavily toward explicit namespacing and took the opportunity to
  shorten some function names in the matrix module.

[Unreleased]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.2.0...HEAD
