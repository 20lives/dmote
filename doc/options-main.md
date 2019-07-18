# General configuration options

Each heading in this document represents a recognized configuration key in the main body of a YAML file for a DMOTE variant. Other documents cover special sections of this one in more detail.

## Parameter `split`

If `true`, build two versions of the case: One for the right hand and a mirror image for the left hand. Threaded holes and other chiral components of the case are exempted from mirroring.

## Section `keys`

Keys, that is keycaps and electrical switches, are not the main focus of this application, but they influence the shape of the case.

### Parameter `preview`

If `true`, include models of the keycaps in place on the keyboard. This is intended for illustration as you work on a design, not for printing.

### Parameter `styles`

Here you name all the types of keys on the keyboard. For each type, describe the properties of the style using the parameters of the [`dmote-keycap`](https://github.com/veikman/dmote-keycap) library, documented in that project. The names of the styles are then used elsewhere, as described [here](options-nested.md).

Key styles determine the size of key mounting plates on the keyboard and what kind of holes are cut into those plates for the switches to fit into. Negative space is also reserved above the plate, for the movement of the keycap: A function of switch height, switch travel, and keycap shape. In addition, if the keyboard is curved, key styles help determine the spacing between key mounts.

## Special section `key-clusters`

This section describes the general size, shape and position of the clusters of keys on the keyboard, each in its own subsection. It is documented in detail [here](options-clusters.md).

## Section `by-key`

This section repeats. Each level of settings inside it is more specific to a smaller part of the keyboard, eventually reaching the level of individual keys. It’s all documented [here](options-nested.md).

### Special recurring section `parameters`

Default values at the global level.

### Special section `clusters` ← overrides go in here

Starting here, you gradually descend from the global level toward the key level.

## Parameter `secondaries`

A map where each item provides a name for a position in space. Such positions exist in relation to other named features of the keyboard and can themselves be used as named features: Typically as supplementary targets for `tweaks`, which are defined below.

An example:

```secondaries:
  s0:
    anchor: f0
    corner: NNE
    segment: 3
    offset: [0, 0, 10]
```
This example gives the name `s0` to a point 10 mm above a key or some other feature named `f0`, which must be defined elsewhere.

A `corner` and `segment` are useful mainly with key aliases. An `offset` is applied late, i.e. in the overall coordinate system, following any transformations inherent to the anchor.

## Section `case`

Much of the keyboard case is generated from the `wall` parameters described [here](options-nested.md). This section deals with lesser features of the case.

### Parameter `key-mount-thickness`

The thickness in mm of each switch key mounting plate.

### Parameter `key-mount-corner-margin`

The thickness in mm of an imaginary “post” at each corner of each key mount. Copies of such posts project from the key mounts to form the main walls of the case.

`key-mount-thickness` is similarly the height of each post.

### Parameter `web-thickness`

The thickness in mm of the webbing between switch key mounting plates, and of the rear housing’s walls and roof.

### Section `rear-housing`

The furthest row of a key cluster can be extended into a rear housing for the MCU and various other features.

#### Parameter `include`

If `true`, add a rear housing. Please arrange case walls so as not to interfere, by removing them along the far side of the last row of key mounts in the indicated cluster.

#### Parameter `wall-thickness`

The horizontal thickness in mm of the walls.

#### Parameter `roof-thickness`

The vertical thickness in mm of the flat top.

#### Section `position`

Where to put the rear housing. By default, it sits all along the far side of the `main` cluster but has no depth.

##### Parameter `cluster`

The key cluster at which to anchor the housing.

##### Section `offsets`

Modifiers for where to put the four sides of the roof. All are in mm.

###### Parameter `north`

The extent of the roof on the y axis; its horizontal depth.

###### Parameter `west`

The extent on the x axis past the first key in the row.

###### Parameter `east`

The extent on the x axis past the last key in the row.

###### Parameter `south`

The horizontal distance in mm, on the y axis, between the furthest key in the row and the roof of the rear housing.

#### Parameter `height`

The height in mm of the roof, over the floor.

#### Section `fasteners`

Threaded bolts can run through the roof of the rear housing, making it a hardpoint for attachments like a stabilizer to connect the two halves of a split keyboard.

##### Parameter `diameter`

The ISO metric diameter of each fastener.

##### Parameter `bosses`

If `true`, add nut bosses to the ceiling of the rear housing for each fastener. Space permitting, these bosses will have some play on the north-south axis, to permit adjustment of the angle of the keyboard halves under a stabilizer.

##### Section `west`

A fastener on the inward-facing end of the rear housing.

###### Parameter `include`

If `true`, include this fastener.

###### Parameter `offset`

A one-dimensional offset in mm from the inward edge of the rear housing to the fastener. You probably want a negative number if any.

##### Section `east`

A fastener on the outward-facing end of the rear housing. All parameters are analogous to those for `west`.

###### Parameter `include`



###### Parameter `offset`



### Section `back-plate`

Given that independent movement of each half of a split keyboard is not useful, each half can include a mounting plate for a stabilizing ‘beam’. That is a straight piece of wood, aluminium, rigid plastic etc. to connect the two halves mechanically and possibly carry the wire that connects them electrically.

This option is similar to rear housing, but the back plate block provides no interior space for an MCU etc. It is solid, with holes for threaded fasteners including the option of nut bosses. Its footprint is not part of a `bottom-plate`.

#### Parameter `include`

If `true`, include a back plate block. This is not contingent upon `split`.

#### Parameter `beam-height`

The nominal vertical extent of the back plate in mm. Because the plate is bottom-hulled to the floor, the effect of this setting is on the area of the plate above its holes.

#### Section `fasteners`

Two threaded bolts run through the back plate.

##### Parameter `diameter`

The ISO metric diameter of each fastener.

##### Parameter `distance`

The horizontal distance between the fasteners.

##### Parameter `bosses`

If `true`, cut nut bosses into the inside wall of the block.

#### Section `position`

The block is positioned in relation to a named feature.

##### Parameter `anchor`

The name of a feature where the block will attach.

##### Parameter `offset`

An offset in mm from the named feature to the middle of the base of the back-plate block.

### Section `bottom-plate`

A bottom plate can be added to close the case. This is useful mainly to simplify transportation.

#### Overview

The bottom plate is largely two-dimensional. The application builds most of it from a set of polygons, trying to match the perimeter of the case at the ground level (i.e. z = 0).

Specifically, there is one polygon per key cluster, limited to `full` wall edges, one polygon for the rear housing, and one set of polygons for each of the first-level case `tweaks` that use `at-ground`, ignoring chunk size and almost ignoring tweaks nested within lists of tweaks.

This methodology is mentioned here because its results are not perfect. Pending future features in OpenSCAD, a future version may be based on a more exact projection of the case, but as of 2018, such a projection is hollow and cannot be convex-hulled without escaping the case, unless your case is convex to start with.

For this reason, while the polygons fill the interior, the perimeter of the bottom plate is extended by key walls and case `tweaks` as they would appear at the height of the bottom plate. Even this brutality may be inadequate. If you require a more exact match, do a projection of the case without a bottom plate, save it as DXF/SVG etc. and post-process that file to fill the interior gap.


#### Parameter `include`

If `true`, include a bottom plate for the case.

#### Parameter `preview`

Preview mode. If `true`, put a model of the plate in the same file as the case it closes. Not for printing.

#### Parameter `combine`

If `true`, combine wrist rests for the case and the bottom plate into a single model, when both are enabled. This is typically used with the `solid` style of wrest rest.

#### Parameter `thickness`

The thickness (i.e. height) in mm of all bottom plates you choose to include. This covers plates for the case and for the wrist rest.

The case will not be raised to compensate for this. Instead, the height of the bottom plate will be removed from the bottom of the main model so that it does not extend to z = 0.

#### Section `installation`

How your bottom plate is attached to the rest of your case.

##### Parameter `style`

The general means of installation. All currently available styles use threaded fasteners with countersunk heads. The styles differ only in how these fasteners attach to the case.

One of:

- `threads`: Threaded holes in the case.
- `inserts`: Unthreaded holes for threaded heat-set inserts.

##### Parameter `thickness`

The thickness in mm of each wall of the anchor points.

##### Section `inserts`

Properties of heat-set inserts for the `inserts` style.

###### Parameter `length`

The length in mm of each insert.

###### Section `diameter`

It is assumed that, as in Tom Short’s Dactyl-ManuForm, the inserts are largely cylindrical but vary in diameter across their length.

###### Parameter `top` at level 7

Top diameter in m.

###### Parameter `bottom` at level 7

Bottom diameter in mm. This needs to be at least as large as the top diameter since the mounts for the inserts only open from the bottom.

##### Section `fasteners`

The type and positions of the threaded fasteners used to secure each bottom plate.

###### Parameter `diameter`

The ISO metric diameter of each fastener.

###### Parameter `length`

The length in mm of each fastener. In the `threads` style, this refers to the part of the screw that is itself threaded: It excludes the head.

###### Parameter `positions`

A list of places where threaded fasteners will connect the bottom plate to the rest of the case.

### Section `leds`

Support for light-emitting diodes in the case walls.

#### Parameter `include`

If `true`, cut slots for LEDs out of the case wall, facing the space between the two halves.

#### Section `position`

Where to attach the LED strip.

##### Parameter `cluster`

The key cluster at which to anchor the strip.

#### Parameter `amount`

The number of LEDs.

#### Parameter `housing-size`

The length of the side on a square profile used to create negative space for the housings on a LED strip. This assumes the housings are squarish, as on a WS2818.

The negative space is not supposed to penetrate the wall, just make it easier to hold the LED strip in place with tape, and direct its light. With that in mind, feel free to exaggerate by 10%.

#### Parameter `emitter-diameter`

The diameter of a round hole for the light of an LED.

#### Parameter `interval`

The distance between LEDs on the strip. You may want to apply a setting slightly shorter than the real distance, since the algorithm carving the holes does not account for wall curvature.

### Parameter `tweaks`

Additional shapes. This is usually needed to bridge gaps between the walls of the key clusters. The expected value here is an arbitrarily nested structure starting with a map of names to lists.

The names at the top level are arbitrary but should be distinct and descriptive. Their only technical significance lies in the fact that when you combine multiple configuration files, a later tweak will override a previous tweak if and only if they share the same name.

Below the names, each item in each list can follow one of the following patterns:

- A leaf node. This is a tuple of 1 to 4 elements specified below.
- A map, representing an instruction to combine nested items in a specific way.
- A list of any combination of the other two types. This type exists at the second level from the top and as the immediate child of each map node.

Each leaf node identifies a particular named feature of the keyboard. It’s usually a set of corner posts on a named (aliased) key mount. These are identical to the posts used to build the walls, but this section gives you greater freedom in how to combine them. The elements of a leaf are, in order:

1. Mandatory: The name of a feature, such as a key alias.
2. Optional: A corner ID, such as `NNE` for north by north-east. If this is omitted, i.e. if only the mandatory element is given, the tweak will use the middle of the named feature.
3. Optional: A starting wall segment ID, which is an integer from 0 to 4 inclusive. If this is omitted, but a corner is named, the default value is 0.
4. Optional: A second wall segment ID. If this is provided, the leaf will represent the convex hull of the two indicated segments plus all segments between them. If this is omitted, only one wall post will be placed.

By default, a map node will create a convex hull around its child nodes. However, this behaviour can be modified. The following keys are recognized:

- `at-ground`: If `true`, child nodes will be extended vertically down to the ground plane, as with a `full` wall. The default value for this key is `false`. See also: `bottom-plate`.
- `above-ground`: If `true`, child nodes will be visible as part of the case. The default value for this key is `true`.
- `chunk-size`: Any integer greater than 1. If this is set, child nodes will not share a single convex hull. Instead, there will be a sequence of smaller hulls, each encompassing this many items.
- `highlight`: If `true`, render the node in OpenSCAD’s highlighting style. This is convenient while you work.
- `hull-around`: The list of child nodes. Required.

In the following example, `A` and `B` are key aliases that would be defined elsewhere. The example is interpreted to mean that a plate should be created stretching from the south-by-southeast corner of `A` to the north-by-northeast corner of `B`. Due to `chunk-size` 2, that first plate will be joined, not hulled, with a second plate from `B` back to a different corner of `A`, with a longer stretch of (all) wall segments down the corner of `A`.

```case:
  tweaks:
    bridge-between-A-and-B:
      - chunk-size: 2
        hull-around:
        - [A, SSE]
        - [B, NNE]
        - [A, SSW, 0, 4]
```

### Section `foot-plates`

Optional flat surfaces at ground level for adding silicone rubber feet or cork strips etc. to the bottom of the keyboard to increase friction and/or improve feel, sound and ground clearance.

#### Parameter `include`

If `true`, include foot plates.

#### Parameter `height`

The height in mm of each mounting plate.

#### Parameter `polygons`

A list describing the horizontal shape, size and position of each mounting plate as a polygon.

## Section `mcu`

This is short for ”micro-controller unit”. Each half has one.

### Parameter `include`

If `true`, build support for the MCU PCBA.

### Parameter `preview`

If `true`, render a visualization of the MCU PCBA. For use in development.

### Parameter `type`

A symbolic name for a commercial product. Currently, only `promicro` is supported, referring to any MCU PCBA with the dimensions of a SparkFun Pro Micro, including That-Canadian’s Elite-C.

### Parameter `margin`

A general measurement in mm of extra space around each part of the PCBA, including PCB and USB connector. This is applied to DMOTE components meant to hold the PCBA in place, accounting for printing inaccuracy as well as inaccuracies in manufacturing the PCBA.

### Section `position`

Where to place the MCU PCBA.

#### Parameter `prefer-rear-housing`

If `true` and `rear-housing` is included, place the PCBA in relation to the rear housing. Otherwise, place the PCBA in relation to a named feature identified by `anchor`.

#### Parameter `anchor`

The name of a key at which to place the PCBA if `prefer-rear-housing` is `false` or rear housing is not included.

#### Parameter `corner`

A code for a corner of the `anchor` feature. This determines both the location and facing of the PCBA.

#### Parameter `offset`

A 3D offset in mm, measuring from the `corner`.

#### Parameter `rotation`

A vector of 3 angles in radians. This parameter governs the rotation of the PCBA around its anchor point in the front. You would not normally need this for the PCBA.

### Section `support`

The support structure that holds the MCU PCBA in place.

#### Parameter `style`

The style of the support. Available styles are:

- `lock`: A separate physical object that is bolted in place over the MCU. This style is appropriate only with a rear housing, and then only when the PCB aligns with a long wall of that housing. It has the advantage that it can hug the connector on the PCB tightly, thus preventing a fragile surface-mounted connector from breaking off.
- `stop`: A gripper that holds the PCBA in place at its rear end. This gripper, in turn, is held up by key mount webbing and is thus integral to the keyboard, not printed separately like the lock. This style does not require rear housing.

#### Parameter `preview`

If `true`, render a visualization of the support in place. This applies only to those parts of the support that are not part of the case model.

#### Parameter `height-factor`

A multiplier for the width of the PCB, producing the height of the support actually touching the PCB.

#### Parameter `lateral-spacing`

A lateral 1D offset in mm. With rear housing, this creates space between the rear housing itself and the back of the PCB’s through-holes, so it should be roughly matched to the length of wire overshoot. Without rear housing, it isn’t so useful but it does work analogously.

#### Section `lock`

Parameters relevant only with a `lock`-style support.

##### Section `fastener`

A threaded bolt connects the lock to the case.

###### Parameter `style`

A style of bolt head (cap) supported by `scad-tarmi`.

###### Parameter `diameter`

The ISO metric diameter of the fastener.

##### Section `socket`

A housing around the USB connector on the MCU PCBA.

###### Parameter `thickness`

The wall thickness of the socket.

##### Section `bolt`

The part of a `lock`-style support that does not print with the keyboard case. This bolt, named by analogy with a lock, is not to be confused with the threaded fastener (also a bolt) holding it in place.

###### Parameter `clearance`

The distance of the bolt from the populated side of the PCB. This distance should be slightly greater than the height of the tallest component on the PCB.

###### Parameter `overshoot`

The distance across which the bolt will touch the PCB at the mount end. Take care that this distance is free of components on the PCB.

###### Parameter `mount-length`

The length of the base containing a threaded channel used to secure the bolt over the MCU. This is in addition to `overshoot` and goes in the opposite direction, away from the PCB.

###### Parameter `mount-thickness`

The thickness of the mount. This should have some rough correspondence to the threaded portion of your fastener, which should not have a shank.

#### Section `stop`

Parameters relevant only with a `stop`-style support.

##### Parameter `anchor`

The name of a key where a stop will start to attach itself.

##### Parameter `direction`

A direction in the matrix from the named key. The stop will attach to a hull of four neighbouring key mount corners in this direction.

##### Section `gripper`

The shape of the part that grips the PCB.

###### Parameter `notch-depth`

The horizontal depth of the notch in the gripper that holds the PCB. The larger this number, the more flexible the case has to be to allow assembly.

Note that while this is similar in effect to `lock`-style `overshoot`, it is a separate parameter because of the flexion limit.

###### Parameter `total-depth`

The horizontal depth of the gripper as a whole in line with the PCB.

###### Parameter `grip-width`

The width of a protrusion on each side of the notch.

## Section `connection`

There must be a signalling connection between the two halves of a split keyboard.

### Parameter `include`

If `true`, inclue a “metasocket”, i.e. physical support for a socket where you plug in a cable that will, in turn, provide the signalling connection between the two halves.

### Parameter `socket-size`

The size in mm of a hole in the case, for the female to fit into. For example, the female might be a type 616E socket for a (male) 4P4C “RJ9” plug, in which case the metasocket has to fit around the entire 616E.

This parameter assumes a cuboid socket. For a socket of a different shape, get as close as possible, then make your own adapter and/or widen the metasocket with a soldering iron or similar tools.

### Parameter `socket-thickness`

The thickness in mm of the roof, walls and floor of the metasocket, i.e. around the hole in the case.

### Section `position`

Where to place the socket. Equivalent to `mcu` → `position`.

#### Parameter `prefer-rear-housing`



#### Parameter `anchor`



#### Parameter `corner`



#### Parameter `raise`

If `true`, and the socket is being placed in relation to the rear housing, put it directly under the ceiling, instead of directly over the floor.

#### Parameter `offset`



#### Parameter `rotation`



## Section `wrist-rest`

An optional extension to support the user’s wrist.

### Parameter `include`

If `true`, include a wrist rest with the keyboard.

### Parameter `style`

The style of the wrist rest. Available styles are:

- `threaded`: threaded fasteners connect the case and wrist rest.
- `solid`: the case and wrist rest are joined together by `tweaks` as a single piece of plastic.

### Parameter `preview`

Preview mode. If `true`, this puts a model of the wrist rest in the same OpenSCAD file as the case. That model is simplified, intended for gauging distance, not for printing.

### Section `position`

The wrist rest is positioned in relation to a named feature.

#### Parameter `anchor`

The name of a feature where the wrist rest will attach. The vertical component of its position will be ignored.

#### Parameter `corner`

A corner of the feature named in `anchor`.

#### Parameter `offset`

An offset in mm from the feature named in `anchor`.

### Parameter `plinth-height`

The average height of the plastic plinth in mm, at its upper lip.

### Section `shape`

The wrist rest needs to fit the user’s hand.

#### Section `spline`

The horizontal outline of the wrist rest is a closed spline.

##### Parameter `main-points`

A list of nameable points, in clockwise order. The spline will pass through all of these and then return to the first one. Each point can have two properties:

- Mandatory: `position`. A pair of coordinates, in mm, relative to other points in the list.
- Optional: `alias`. A name given to the specific point, for the purpose of placing yet more things in relation to it.

##### Parameter `resolution`

The amount of vertices per main point. The default is 1. If 1, only the main points themselves will be used, giving you full control. A higher number gives you smoother curves.

If you want the closing part of the curve to look smooth in high resolution, position your main points carefully.

Resolution parameters, including this one, can be disabled in the main `resolution` section.

#### Section `lip`

The lip is the uppermost part of the plinth, lining and supporting the edge of the pad. Its dimensions are described here in mm away from the pad.

##### Parameter `height`

The vertical extent of the lip.

##### Parameter `width`

The horizontal width of the lip at its top.

##### Parameter `inset`

The difference in width between the top and bottom of the lip. A small negative value will make the lip thicker at the bottom. This is recommended for fitting a silicone mould.

#### Section `pad`

The top of the wrist rest should be printed or cast in a soft material, such as silicone rubber.

##### Section `surface`

The upper surface of the pad, which will be in direct contact with the user’s palm or wrist.

###### Section `edge`

The edge of the pad can be rounded.

###### Parameter `inset` at level 7

The horizontal extent of softening. This cannot be more than half the width of the outline, as determined by `main-points`, at its narrowest part.

###### Parameter `resolution` at level 7

The number of faces on the edge between horizontal points.

Resolution parameters, including this one, can be disabled in the main `resolution` section.

###### Section `heightmap`

The surface can optionally be modified by the [`surface()` function](https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Other_Language_Features#Surface), which requires a heightmap file.

###### Parameter `include` at level 7

If `true`, use a heightmap. The map will intersect the basic pad polyhedron.

###### Parameter `filepath` at level 7

The file identified here should contain a heightmap in a format OpenSCAD can understand. The path should also be resolvable by OpenSCAD.

##### Section `height`

The piece of rubber extends a certain distance up into the air and down into the plinth. All measurements in mm.

###### Parameter `surface-range`

The vertical range of the upper surface. Whatever values are in a heightmap will be normalized to this scale.

###### Parameter `lip-to-surface`

The part of the rubber pad between the top of the lip and the point where the heightmap comes into effect. This is useful if your heightmap itself has very low values at the edges, such that moulding and casting it without a base would be difficult.

###### Parameter `below-lip`

The depth of the rubber wrist support, measured from the top of the lip, going down into the plinth. This part of the pad just keeps it in place.

### Section `rotation`

The wrist rest can be rotated to align its pad with the user’s palm.

#### Parameter `pitch`

Tait-Bryan pitch.

#### Parameter `roll`

Tait-Bryan roll.

### Special section `mounts`

A list of mounts for threaded fasteners. Each such mount will include at least one cuboid block for at least one screw that connects the wrist rest to the case. This section is used only with the `threaded` style of wrist rest.

### Section `sprues`

Holes in the bottom of the plinth. You pour liquid rubber through these holes when you make the rubber pad. Sprues are optional, but the general recommendation is to have two of them if you’re going to be casting your own pads. That way, air can escape even if you accidentally block one sprue with a low-viscosity silicone.

#### Parameter `include`

If `true`, include sprues.

#### Parameter `inset`

The horizontal distance between the perimeter of the wrist rest and the default position of each sprue.

#### Parameter `diameter`

The diameter of each sprue.

#### Parameter `positions`

The positions of all sprues. This is a list where each item needs an `anchor` naming a main point in the spline. You can add an optional two-dimensional `offset`.

### Section `bottom-plate`

The equivalent of the case `bottom-plate` parameter. If included, a bottom plate for a wrist rest uses the `thickness` configured for the bottom of the case.

Bottom plates for the wrist rests have no ESDS electronics to protect but serve other purposes: Covering nut pockets, silicone mould-pour cavities, and plaster or other dense material poured into plinths printed without a bottom shell.

#### Parameter `include`

Whether to include a bottom plate for each wrist rest.

#### Parameter `inset`

The horizontal distance between the perimeter of the wrist rest and the default position of each threaded fastener connecting it to its bottom plate.

#### Parameter `fastener-positions`

The positions of threaded fasteners used to attach the bottom plate to its wrist rest. The syntax of this parameter is precisely the same as for the case’s bottom-plate fasteners. Corners are ignored and the starting position is inset from the perimeter of the wrist rest by the `inset` parameter above, before any offset stated here is applied.

Other properties of these fasteners are determined by settings for the case.

### Parameter `mould-thickness`

The thickness in mm of the walls and floor of the mould to be used for casting the rubber pad.

## Section `resolution`

Settings for the amount of detail on curved surfaces. More specific resolution parameters are available in other sections.

### Parameter `include`

If `true`, apply resolution parameters found throughout the configuration. Otherwise, use defaults built into this application, its libraries and OpenSCAD. The defaults are generally conservative, providing quick renders for previews.

### Parameter `minimum-face-size`

File-wide OpenSCAD minimum face size in mm.

## Section `dfm`

Settings for design for manufacturability (DFM).

### Parameter `error-general`

A measurement in mm of errors introduced to negative space in the xy plane by slicer software and the printer you will use.

The default value is zero. An appropriate value for a typical slicer and FDM printer with a 0.5 mm nozzle would be about -0.5 mm.

This application will try to compensate for the error, though only for certain sensitive inserts, not for the case as a whole.

### Section `keycaps`

Measurements of error, in mm, for parts of keycap models. This is separate from `error-general` because it’s especially important to have a tight fit between switch sliders and cap stems, and the size of these details is usually comparable to an FDM printer nozzle.

If you will not be printing caps, ignore this section.

#### Parameter `error-stem-positive`

Error on the positive components of stems on keycaps, such as the entire stem on an ALPS-compatible cap.

#### Parameter `error-stem-negative`

Error on the negative components of stems on keycaps, such as the cross on an MX-compatible cap.

### Section `bottom-plate`

DFM for bottom plates.

#### Parameter `fastener-plate-offset`

A vertical offset in mm for the placement of screw holes in bottom plates. Without a slight negative offset, slicers will tend to make the holes too wide for screw heads to grip the plate securely.

Notice this will not affect how screw holes are cut into the case.

## Section `mask`

A box limits the entire shape, cutting off any projecting by-products of the algorithms. By resizing and moving this box, you can select a subsection for printing. You might want this while you are printing prototypes for a new style of switch, MCU support etc.

### Parameter `size`

The size of the mask in mm. By default, `[1000, 1000, 1000]`.

### Parameter `center`

The position of the center point of the mask. By default, `[0, 0, 500]`, which is supposed to mask out everything below ground level. If you include bottom plates, their thickness will automatically affect the placement of the mask beyond what you specify here.

⸻

This document was generated from the application CLI.
