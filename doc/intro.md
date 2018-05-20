# Introduction to the DMOTE

The larger project is presented [here](http://viktor.eikman.se/article/the-dmote/).

## Assembly

Here’s how to use this code repository to build a keyboard case.

Instructions specific to the DMOTE have yet to be written for the later stages,
hand-wiring the switches with diodes and building firmware to run on embedded
microcontrollers. To get started with that stuff, please refer to the
original instructions for the Dactyl-ManuForm or the Dactyl, or contact
the maintainer of the fork you are printing.

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

Pregenerated STL files are available in the [things/](../things/) directory.
Caution is advised when printing as the web of switch connectors presents a
series of complicated overhangs. For printing prototypes and any printing with
materials that stiffen quickly enough to produce viable switch mounts without
support, build support from the base plate only; this simplifies the process
of removing the supports.
