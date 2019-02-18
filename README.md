## The Dactyl-ManuForm keyboard: Opposable Thumb Edition

This, the DMOTE, is a fork of the
[Dactyl-ManuForm](https://github.com/tshort/dactyl-keyboard), a parametrized,
split-hand, concave, columnar, ergonomic keyboard. In this fork, the thumb
cluster has been modified to minimize shearing forces.

[![Image of the second working DMOTE](http://viktor.eikman.se/image/dmote-2-top-down-view/display)](http://viktor.eikman.se/article/the-dmote/)

Parameters have been moved out of the application code itself, into separate
files that are safe and easy to edit. Use them to change:

- Switch type: ALPS or MX.
- Size and shape.
    - Row and column curvature and tilt (tenting).
    - Exceptions at any level, down to the position of individual keys.
- Minor features like LED strips and wrist rests.

For documentation see [doc/](doc/).

### License

Copyright © 2015-2019 Matthew Adereth, Tom Short, Viktor Eikman et al.

The source code for generating the models (everything excluding the [things/](things/) and [resources/](resources/) directories) is distributed under the [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](LICENSE). The generated models and PCB designs are distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike License Version 3.0](LICENSE-models).
