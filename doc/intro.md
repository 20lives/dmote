# Introduction to the DMOTE

Here’s how to use this code repository to build a keyboard case.
For a still-more general introduction to the larger project, see
[this](http://viktor.eikman.se/article/the-dmote/).

## From code to print

This repository is source code for a Clojure application. Clojure runs on
[the JVM](https://en.wikipedia.org/wiki/Java_(software_platform)). The
application produces an [OpenSCAD](http://www.openscad.org/) program which,
in turn, can be rendered to a portable geometric description like
[STL](https://en.wikipedia.org/wiki/STL_(file_format)). STL can be
[sliced](https://en.wikipedia.org/wiki/Slicer_(3D_printing)) to
[G-code](https://en.wikipedia.org/wiki/G-code) and the G-code can
steer a 3D printer.

OpenSCAD can represent the model visually, but there is no step in this process
where you point and click with a mouse to change the design. The shape of the
keyboard is determined by your written parameters to the Clojure application.

Roughly, the build chain looks like this:

> parameters through this app (compiled) → preview → rendering → slicing → printing

Equivalently, in terms of typical file name endings:

> .yaml through .clj (or .jar) → .scad → .stl → .gcode → tangible keyboard

If this repository includes STL files you will find them in the
[`things/stl`](../things/) directory. They should be ready to print. Otherwise,
here’s how to make your own.

### Setting up the build environment

* Install the [Clojure runtime](https://clojure.org)
* Install the [Leiningen project manager](http://leiningen.org/)
* Optional: Install [GNU make](https://www.gnu.org/software/make/)
* Install [OpenSCAD](http://www.openscad.org/)

On Debian GNU+Linux, the first three are accomplished with
`apt install clojure leiningen make`.

The necessary Clojure libraries will be pulled in by a `lein run`.

### Finding bundled configurations

The Clojure application combines configuration details from zero or more
[YAML](https://en.wikipedia.org/wiki/YAML) files. There are examples in
[`config`](config/). These files have a nested structure
[documented here](options-main.md).

The files are used when you pass them to the application, not automatically.
The ones you name are gently combined into a single master configuration. In
this process, files named later take precedence over those names earlier, in
case of direct conflicts.

### Producing OpenSCAD and STL files

* To produce OpenSCAD files for the default configuration, run `make`.
  * If you do not have Make, or if you want more control, run `lein run` with
    the arguments you see prepared in the makefile. For example,
    `lein run -c config/base.yaml -c config/dmote/base.yaml` will
    build a basic DMOTE, with the same results as `make`, just
    without an optional compilation step that speeds up each iteration.
* In OpenSCAD, open one of the `things/scad/*.scad` files for a preview.
  * To render a complex model in OpenSCAD you may need to go to Edit >>
    Preferences >> Advanced and raise the ceiling for when to “Turn off
    rendering”.
* When satisfied, render to STL by calling the Clojure application with the
  `--render` flag in addition to previous flags, i.e. those you typed yourself
  or got through Make. You can also render directly from OpenSCAD.

If you do have Make and you want to weave in more configuration files, you can
put more YAML paths on the command line or name an intermediate Make target, or
do both. However, this is not what Make is for, so you will need to finish with
a named target that actually calls the application, as in `make vis
dmote_62key`, where `dmote_62key` is the (otherwise implicit) default target.

There are [other ways to evaluate](http://stackoverflow.com/a/28213489) the
Clojure code, including the bundled `transpile.sh` shell script, which will
tail your changes with `inotify` if you have that.

## Customization

You probably want to customize the design for your own hands. You won’t need
to do any coding if all you want is a personal fit or additional keys.

You can change the bundled YAML files if you like. However, it is generally
easier to add your own files, maintaining them separately from the DMOTE
repository. That way, they will be safe if you upgrade the application with a
`git pull`.

Remember to put your own file(s) last in your list of arguments to the
application, to get the most power. For example, to override all the ALPS-style
key mounts with MX-style mounts on a DMOTE, run `lein run -c config/base.yaml
-c config/dmote/base.yaml -c config/dmote/mx.yaml`.

#### Nomenclature: Finding north

The parameter files and the code use the cardinal directions of the compass
to describe directions in the space of the keyboard model. To understand these,
imagine having (the right-hand side of) the keyboard in front of you, as you
would use it, while you face true north.

“North” in configuration thus refers to the direction away from the user: the
far side. “South” is the direction toward the user: the near side.

“West” and “east” vary on each half of a split keyboard because the left-hand
side is a mirror image of the right-hand side. The right-hand side is primary
for the purposes of nomenclature. On either half, the west is inward, toward
the space between the two halves of the keyboard. The east is outward, away
from the other half of the keyboard. If you are not building a split keyboard,
think of your case as you would the right-hand side: The west is on your left
and the east is on your right.

In Euclidean space, the x axis goes from west to east, the y axis from
south to north, and the z axis from earth to sky.

### Deeper changes

If you find that you cannot get what you want just by changing the parameters,
you need to edit the source code. If you are not familiar with OpenSCAD, start
by experimenting with its native format, writing `.scad` files from scratch.
Then consider starting in `src/dactyl_keyboard/sandbox.clj` to get familiar
with `scad-clj`. It writes OpenSCAD code for you with helpful abstractions.

If you want your changes to the source code to be merged upstream, please do
not remove or break existing features. There are already several `include` and
`style` parameters designed to support a variety of mutually incompatible
styles in the code base. Add yours instead of simply repurposing functions,
and test to make sure you have not damaged other styles.

## Printing tips

For printing prototypes and any printing with PLA-like materials that stiffen
quickly, build support from the base plate only. This simplifies the process
of removing the supports.

For accuracy problems, especially with threaded fasteners, consider tweaking
the DFM settings [documented here](options-main.md), particularly the
`error-general` parameter.

### Bottom plates

If you are using threaded fasteners to connect bottom plates directly to the
case (the `threads` style), please note that common FDM printers usually won’t
print threaded holes smaller than M3 with useful accuracy. M4 is a safer bet.

If you are having trouble with the fit and neither DFM settings nor larger
fasteners are helping, consider a greater `thickness` for the anchor points,
along with slicer settings that give you thinner walls and less infill. This
should give you a more yielding threaded hole, decreasing the risk of a
delaminating crack, but increasing the risk of threads deforming over time.

In any case you may want to use `foot-plates` to provide additional support for
the anchor points.

### Wrist rests

If you are including wrist rests, consider printing the plinths without a
bottom plate and with sparse or gradual infill. This makes it easy to pour
plaster or some other dense material into the plinths to add mass.

## After printing

Instructions specific to the DMOTE have yet to be written for hand-wiring the
switches with diodes and installing embedded microcontrollers. To get started
with that stuff, please refer to the original instructions for the
Dactyl-ManuForm or the Dactyl, or contact the maintainer of the fork you
are printing.

As for microcontroller firmware, QMK works great. In that project, the DMOTE
is filed as a version of the Dactyl-ManuForm
[here](https://github.com/qmk/qmk_firmware/tree/master/keyboards/handwired/dactyl_manuform/dmote).
