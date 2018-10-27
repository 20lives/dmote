# Change Log
This log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/). It picks up from DMOTE
version 0.2.0, thus covering only a fraction of the project’s history.

## [Unreleased]
### Changed
- Removed `create-models.sh`, adding equivalent functionality to the Clojure
  application itself (new flags: `--render`, `--renderer`).
- Added intermediate `scad` and `stl` folders under `things`.

### Added
- This log.
- Support for a filename whitelist in the CLI.

### Developer
- Started migration to docstring-first function definitions.

[Unreleased]: https://github.com/veikman/dactyl-keyboard/compare/dmote-v0.2.0...HEAD
