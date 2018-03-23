# The Dactyl-ManuForm Keyboard: Opposable Thumb Edition
This is a fork of the [Dactyl](https://github.com/adereth/dactyl-keyboard), a parametrized, split-hand, concave, columnar, ergonomic keyboard. In this fork,
the walls drop down as on the [ManuForm](https://github.com/jeffgran/ManuForm) ([geekhack](https://geekhack.org/index.php?topic=46015.0)).

![Imgur](http://i.imgur.com/LdjEhrR.jpg)

The keyboard is parametrized to allow adjusting the following:

* 4+ rows and 5+ columns of keys, irregularities permitted
* Row and column curvature
* Wrist rest curvature
* Row tilt (tenting)
* Column tilt
* Height
* Exceptions (tilt and placement) for columns and individual keys
* Minor features: LED holder etc.

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

On Linux, run `create-models.sh` to export all.

**Tips**
* To render a complex model in OpenSCAD you may need to go to Edit >> Preferences >> Advanced and raise the ceiling for when to “Turn off rendering”.
* [An example](http://adereth.github.io/blog/2014/04/09/3d-printing-with-clojure/) of designing with Clojure.
* There are [other ways to evaluate](http://stackoverflow.com/a/28213489) the Clojure code.

### Printing
Pregenerated STL files are available in the [things/](things/) directory.
Caution is advised when printing as the web of switch connectors presents a
series of complicated overhangs. For printing prototypes and any printing with
materials that stiffen quickly enough to produce viable switch mounts without
support, build support from the base plate only; this simplifies the process
of removing the supports.

### Thingiverse

[The 4x5 STL left/right pair](https://www.thingiverse.com/thing:2349390) from
Tom Short’s original Dactyl-ManuForm is in the thingiverse for public printing.

### Wiring

Here are the materials Short used for wiring his version.

* Two Arduino Pro Micros
* [Heat-set inserts](https://www.mcmaster.com/#94180a331/=16yfrx1)
* [M3 wafer-head screws, 5mm](http://www.metricscrews.us/index.php?main_page=product_info&cPath=155_185&products_id=455)
* [Copper tape](https://www.amazon.com/gp/product/B009KB86BU)
* [#32 magnet wire](https://www.amazon.com/gp/product/B00LV909HI)
* [#30 wire](https://www.amazon.com/gp/product/B00GWFECWO)
* [3-mm cast acrylic](http://www.mcmaster.com/#acrylic/=144mfom)
* [Veroboard stripboard](https://www.amazon.com/gp/product/B008CPVMMU)
* [1N4148 diodes](https://www.amazon.com/gp/product/B00LQPY0Y0)
* [Female RJ-9 connectors](https://www.amazon.com/gp/product/B01HU7BVDU/)

Short wired one half using the traditional approach of using the legs of a diode to form the row connections, with magnet wire for columns. That worked okay.
The magnet wire is small enough, it wants to move around, and it's hard to tell if you have a good connection.

![Imgur](http://i.imgur.com/7kPvSgg.jpg)

For another half, Short used stripboard for the row connections.
This allowed him to presolder all of the diodes.
Then, Short hot-glued this in place and finished the soldering of the other diode ends.
Connections for the diodes were much easier with one end fixed down.
On this half, Short also used copper tape to connect columns.
This worked a bit better than the magnet wire.
You may want bare tinned copper wire for columns (something like #20).
With the stripboard, it's pretty easy keeping row and column connections separate.

![Imgur](http://i.imgur.com/JOm5ElP.jpg)

Note that a telephone handset cable has leads that are reversed, so take this into account when connecting these leads to the controller.

The 3D printed part is the main keyboard.
You can attach a bottom plate with screws.
The case has holes for heat-set inserts designed to hold 3- to 6-mm long M3 screws.
Then, Short used wafer-head screws to connect a bottom plate.
If wires aren't dangling, a bottom plate may not be needed, so inserts won’t be either.
You do need something on the bottom to keep the keyboard from sliding around.
Without a plate, you could use a rubber pad, or you could dip the bottom of the keyboard in PlastiDip.

For more photos of the first complete wiring of v0.4, see [Imgur](http://imgur.com/a/v9eIO).

This is how the rows/columns wire to the keys and the Pro Micro
![Wire Diagram](https://docs.google.com/drawings/d/1s9aAg5bXBrhtb6Xw-sGOQQEndRNOqpBRyUyHkgpnSps/pub?w=1176&h=621)

#### Row-Driven Wiring

This alternative is also for the Pro Micro. Make sure the firmware is set up correctly (e.g. change row pins with col pins) if you are going to use this.

![Left Wire Diagram](/resources/dactyl_manuform_left_wire_diagram.png)

![Left Wire Diagram](/resources/dactyl_manuform_right_wire_diagram.png)

### Firmware

Firmware goes hand in hand with how you wire the circuit.
Short adapted the QMK firmware [here](https://github.com/tshort/qmk_firmware/tree/master/keyboards/dactyl-manuform).
This allows each side to work separately or together.
This site also shows connections for the Arduino Pro Micro controllers.

## License

Copyright © 2015-2018 Matthew Adereth, Tom Short, Viktor Eikman et al.

The source code for generating the models (everything excluding the [things/](things/) and [resources/](resources/) directories is distributed under the [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](LICENSE).  The generated models and PCB designs are distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike License Version 3.0](LICENSE-models).
