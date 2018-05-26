# Introduction to the DMOTE

The larger project is presented [here](http://viktor.eikman.se/article/the-dmote/).

## Assembly

Here’s how to use this code repository to build a keyboard case.

Instructions specific to the DMOTE have yet to be written for the later stages,
hand-wiring the switches with diodes and building firmware to run on embedded
microcontrollers. To get started with that stuff, please refer to the
original instructions for the Dactyl-ManuForm or the Dactyl, or contact
the maintainer of the fork you are printing.

### Generating a design

**Setting up the build environment**

* [Install the Clojure runtime](https://clojure.org)
* [Install the Leiningen project manager](http://leiningen.org/)
* [Install GNU make](https://www.gnu.org/software/make/) (convenience)
* [Install OpenSCAD](http://www.openscad.org/)

On Debian, the first three are accomplished with `apt install clojure leiningen make`.

**Producing OpenSCAD and STL files**

* To produce OpenSCAD files for the default configuration, run `make`.
  If you do not have `make`, use `lein run`.
* In OpenSCAD, open one of the `things/*.scad` files for a preview.
* When satisified, use OpenSCAD to render and export to STL format.

To build a non-default, bundled configuration, run `make threaded` or name some
other variant defined in the makefile.

**Making changes**

If you want to change what the default configuration looks like, edit
`resources/opt/default.yaml`. It contains a nested structure of parameters.

You do not have to make all of your changes in `default.yaml`. As you can see
in the makefile, you can call the generating program with one or more `-c`
flags, each identifying a YAML configuration file. You can add your own,
maintaining it separately from the DMOTE repository. Each file will extend or,
as necessary, override the one before, so put your own file last in the list.

Advanced changes may require editing the source code. Start looking in
`src/dactyl_keyboard/params.clj` and `src/dactyl_keyboard/tweaks.clj`.

**Tips**

* To render a complex model in OpenSCAD you may need to go to Edit >>
  Preferences >> Advanced and raise the ceiling for when to “Turn off rendering”.
* On Linux, run `create-models.sh` to export STL programmatically.
* There are [other ways to evaluate](http://stackoverflow.com/a/28213489) the
  Clojure code, including the bundled `transpile.sh` shell script, which will
  tail your changes with inotify.

### Printing

Pregenerated STL files are available in the [things/](../things/) directory.

For printing prototypes and any printing with PLA-like materials that stiffen
quickly enough to produce viable switch mounts without support, in your
slicer, build support from the base plate only. This simplifies the process
of removing the supports.
