# Introduction to the DMOTE

Here’s how to use this code repository to build a keyboard case.
The larger project is presented [here](http://viktor.eikman.se/article/the-dmote/).

Instructions specific to the DMOTE have yet to be written for hand-wiring the
switches with diodes and building firmware to run on embedded microcontrollers.
To get started with that stuff, please refer to the original instructions for
the Dactyl-ManuForm or the Dactyl, or contact the maintainer of the fork you
are printing.

## From code to printable STL

This repository is source code for a Clojure application. Clojure compiles like
Java. The application produces an OpenSCAD program which, in turn, can be
rendered to STL. The STL can be printed.

If this repository includes STL files you will find them in the
[things/](../things/) directory. They should be ready to print. Otherwise,
here’s how to make your own.

### Setting up the build environment

* Install the [Clojure runtime](https://clojure.org)
* Install the [Leiningen project manager](http://leiningen.org/)
* Install [GNU make](https://www.gnu.org/software/make/) (convenience)
* Install [OpenSCAD](http://www.openscad.org/)

On Debian GNU+Linux, the first three are accomplished with `apt install clojure leiningen make`.

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

You probably want to customize the design for your own hands. You probably
won’t need to do any coding.

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

### Deeper changes

Advanced changes require editing the source code. Consider starting in `src/dactyl_keyboard/sandbox.clj` if you are not familiar with `scad-clj`.

If you want your changes to the source code to be merged upstream, please do
not remove or break existing features. There are already several `include` and
`style` parameters designed to support a variety of mutually incompatible
styles in the same code base. Add yours instead of simply repurposing
functions.

## Printing tips

For printing prototypes and any printing with PLA-like materials that stiffen
quickly enough to produce viable switch mounts without support, in your
slicer, build support from the base plate only. This simplifies the process
of removing the supports.
