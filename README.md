# The Dactyl-ManuForm Keyboard:
# Opposable Thumb Edition

This, the DMOTE, is a fork of the [Dactyl-ManuForm](https://github.com/tshort/dactyl-keyboard),
a parametrized, split-hand, concave, columnar, ergonomic keyboard. In this fork,
the thumb cluster has been modified to minimize shearing forces.

![Image of the first working DMOTE](http://viktor.eikman.se/image/dmote-1-glamour-shot/display)

By cloning this repository and editing fairly simple parameter files you can
adjust the following to taste:

* The size and shape of the key matrix
* Row and column curvature
* Wrist rest curvature
* Row tilt (tenting)
* Column tilt
* Height
* Exceptions (tilt and placement) for columns and individual keys
* Whether to include minor features: LED strips etc.

## Assembly

### Generating a Design

**Setting up the Clojure environment**

* [Install the Clojure runtime](https://clojure.org)
* [Install the Leiningen project manager](http://leiningen.org/)
* [Install OpenSCAD](http://www.openscad.org/)

On Debian, the first two are accomplished with `apt install clojure leiningen`.

**Generating models**

* Run `lein repl`. This will generate `things/*.scad` files.
* Use OpenSCAD to open a `.scad` file for a preview.
* Make personal changes to `src/dactyl_keyboard/params.clj`.
* Treat changes by calling `(new-scad)` in the REPL. OpenSCAD will rerender.
* When done, use OpenSCAD to export STL files.

**Tips**

* To render a complex model in OpenSCAD you may need to go to Edit >> Preferences >> Advanced and raise the ceiling for when to “Turn off rendering”.
* On Linux, run `create-models.sh` to export all.
* There are [other ways to evaluate](http://stackoverflow.com/a/28213489) the Clojure code, including the bundled `transpile.sh` shell script, which will tail your changes with inotify.

### Printing

Pregenerated STL files are available in the [things/](things/) directory.
Caution is advised when printing as the web of switch connectors presents a
series of complicated overhangs. For printing prototypes and any printing with
materials that stiffen quickly enough to produce viable switch mounts without
support, build support from the base plate only; this simplifies the process
of removing the supports.

### Wiring and Flashing Firmware

Instructions specific to the DMOTE have yet to be written. Please refer to the
original instructions for the Dactyl-ManuForm or contact Viktor Eikman with a
request.

## License

Copyright © 2015-2018 Matthew Adereth, Tom Short, Viktor Eikman et al.

The source code for generating the models (everything excluding the [things/](things/) and [resources/](resources/) directories is distributed under the [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](LICENSE).  The generated models and PCB designs are distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike License Version 3.0](LICENSE-models).
