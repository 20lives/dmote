# Introduction to the DMOTE

Here’s how to use this code repository to build a keyboard case.
The larger project is presented [here](http://viktor.eikman.se/article/the-dmote/).

## From code to printable STL

This repository is source code for a Clojure application. Clojure can be
packaged like Java to run on the JVM, on any platform. The application produces
an OpenSCAD program which, in turn, can be rendered to a portable geometric
description like STL. STL can be sliced to G-code and the G-code can steer a
3D printer.

OpenSCAD can represent the model visually, but there is no step in this process
where you point and click with a mouse to change the design. The shape of the
keyboard is determined by your written parameters to the Clojure application.

> parameters → this app → JVM → OpenSCAD → STL → G-code → printer → tangible keyboard

If this repository includes STL files you will find them in the
[things/](../things/) directory. They should be ready to print. Otherwise,
here’s how to make your own.

### Setting up the build environment

* Install the [Clojure runtime](https://clojure.org)
* Install the [Leiningen project manager](http://leiningen.org/)
* Optional: Install [GNU make](https://www.gnu.org/software/make/)
* Install [OpenSCAD](http://www.openscad.org/)
* Install an updated [unicode-math](https://github.com/veikman/unicode-math)

On Debian GNU+Linux, the first three are accomplished with `apt install clojure leiningen make`.

unicode-math breaks under clojure 1.9 and later, so a local copy needs to be installed.

### Producing OpenSCAD and STL files

* To produce OpenSCAD files for the default configuration, run `make`.
  * If you do not have `make`, run `lein run`.
  * To build a non-default, bundled configuration, run `make threaded` or name
    some other variant defined in the makefile.
* In OpenSCAD, open one of the `things/*.scad` files for a preview.
  * To render a complex model in OpenSCAD you may need to go to Edit >>
    Preferences >> Advanced and raise the ceiling for when to “Turn off rendering”.
* When satisfied, use OpenSCAD to render and export to STL format.
  * On Linux, you can run `create-models.sh` to export STL programmatically.

There are [other ways to evaluate](http://stackoverflow.com/a/28213489) the
Clojure code, including the bundled `transpile.sh` shell script, which will
tail your changes with `inotify` if you have that.

## Customization

You probably want to customize the design for your own hands. You won’t need
to do any coding if all you want is a personal fit or additional keys.

### Parameters in YAML

If you want to change what the default configuration looks like, edit
`resources/opt/default.yaml`. It contains a nested structure of parameters
[documented here](options.md).

You do not have to make all of your changes in `default.yaml`. As you can see
in the makefile, you can call the generating program with one or more `-c`
flags, each identifying a YAML configuration file. You can add your own,
maintaining it separately from the DMOTE repository. Each file will extend or,
as necessary, override the one before, so put your own file last in your list
of CLI flags to get the most power.

#### Nomenclature: Finding north

The parameter files and the code use the cardinal directions of the compass
to describe directions in the space of the keyboard model. To understand these,
imagine having the right-hand side of the keyboard in front of you, as you
would use it, while you face true north.

“North” in configuration thus refers to the direction away from the user: the
far side. “South” is the direction toward the user: the near side.

“West” and “east” vary on each half of the keyboard because the left-hand side
is purely a mirror image of the right-hand side. The right-hand side is primary
for the purposes of nomenclature. On either half, the west is inward, toward
the space between the two halves of the keyboard. The east is outward, away
from the other half of the keyboard.

In Euclidean space, the x axis goes from west to east, the y axis from
south to north, and the z axis from earth to sky.

### Deeper changes

Advanced changes require editing the source code. Consider starting in `src/dactyl_keyboard/sandbox.clj` if you are not familiar with `scad-clj`.

If you want your changes to the source code to be merged upstream, please do
not remove or break existing features. There are already several `include` and
`style` parameters designed to support a variety of mutually incompatible
styles in the code base. Add yours instead of simply repurposing functions,
and test to make sure you have not damaged other styles.

## Printing tips

For printing prototypes and any printing with PLA-like materials that stiffen
quickly, build support from the base plate only. This simplifies the process
of removing the supports.

If you are including wrist rests, consider printing the plinths without a
bottom plate and with sparse or gradual infill. This makes it easy to pour
plaster or some other dense material into the plinths to add mass.

## After printing

Instructions specific to the DMOTE have yet to be written for hand-wiring the
switches with diodes and building firmware to run on embedded microcontrollers.
To get started with that stuff, please refer to the original instructions for
the Dactyl-ManuForm or the Dactyl, or contact the maintainer of the fork you
are printing.
