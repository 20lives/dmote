# Change log
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
    - Renamed the `key-corner` input of `case` → `foot-plates` → `polygons`
      to `corner`.
    - Removed the option `case` → `rear-housing` → `west-foot` in favour of
      more general `foot-plates` functionality.
- As a side effect of outsourcing the design of threaded fasteners to
  `scad-tarmi`, the `flat` style of bolt head has been renamed to
  the more specific `countersunk`.
- Removed the option `wrist-rest` → `fasteners` → `mounts` → `plinth-side` →
  `pocket-scale`, obviated by a new generic `dfm` feature.
- Renamed the ‘finger’ key cluster to ‘main‘.
- Removed `create-models.sh`, adding equivalent functionality to the Clojure
  application itself (new flags: `--render`, `--renderer`).
- Added intermediate `scad` and `stl` folders under `things`.
- Split generated documentation (`options.md`) into three separate documents
  (`options-*.md`).

### Added
- This log.
- Support for generating a bottom plate that closes the case.
- Support for naming your key clusters much more freely, and/or adding
  additional clusters. Even the new ‘main’ cluster is optional.
    - Support for a `cluster` parameter to `case` → `rear-housing` →
      `position`. The rear housing would previously be attached to the finger
      cluster.
    - Support for a `cluster` parameter to `case` → `leds` → `position`.
      LEDs would previously be attached to the finger cluster.
    - Support for anchoring any cluster to any other, within logical limits.
- Support for a filename whitelist in the CLI.
- Support for placing `treaded`-style wrist rests in relation to their point
  of attachment to the case.
- Support for placing `foot-plates` in relation to objects other than keys.
- Support for generic compensation for slicer and printer inaccuracies in the
  xy plane through a new option, `dfm` → `error`.

### Fixed
- Improved support for Windows by using `clojure.java.io/file` instead of
  string literals with Unix-style file-path separators.

### Developer
- Significant restructuring of the code base for separation of concerns.
    - Switched to docstring-first function definitions.
    - Shifted more heavily toward explicit namespacing and took the opportunity
      to shorten some function names in the matrix module and elsewhere.
- Added a dependency on `scad-tarmi` for shorter OpenSCAD code and more
  capable models of threaded fasteners.
- Rearranged derived parameter structure somewhat to support arbitrary key
  clusters.
- Removed the `new-scad` function without replacement.
- Removed a dependency on `unicode-math`. The requisite version of the library
  had not been deployed to Clojars and its use was cosmetic.

[Unreleased]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.2.0...HEAD
