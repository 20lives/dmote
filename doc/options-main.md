# General configuration options

Each heading in this document represents a recognized configuration key in the main body of a YAML file for a DMOTE variant. Other documents cover special sections of this one in more detail.

## Section `keycaps`

Keycaps are the plastic covers placed over the switches. Their shape will help determine the spacing between key mounts if the keyboard is curved. Negative space is also reserved for the caps.

### Parameter `preview`

If `true`, include models of the keycaps. This is intended for illustration in development. The models are not good enough for printing.

### Parameter `body-height`

The height in mm of each keycap, measured from top to bottom of the entire cap by itself.

An SA cap would be about 11.6 mm, DSA 7.3 mm.

### Parameter `resting-clearance`

The height in mm of the air gap between keycap and switch mount, in a resting state.

## Section `switches`

Electrical switches close a circuit when pressed. They cannot be printed. This section specifies how much space they need to be mounted.

### Parameter `style`

The switch type. One of:

- `alps`: ALPS style switches, including Matias.
- `mx`: Cherry MX style switches.

### Parameter `travel`

The distance in mm that a keycap can travel vertically when mounted on a switch.

## Special section `key-clusters`

This section describes the general size, shape and position of the clusters of keys on the keyboard, each in its own subsection. It is documented in detail [here](options-clusters.md).

## Special nesting section `by-key`

This section is built like an onion. Each layer of settings inside it is more specific to a smaller part of the keyboard, eventually reaching the level of individual keys. It’s all documented [here](options-nested.md).

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

#### Parameter `west-foot`

If `true`, add a foot plate at ground level by the far inward corner of the rear housing. The height of the plate is controlled by the `foot-plates` section below.

#### Parameter `height`

The height in mm of the roof, over the floor.

#### Section `fasteners`

Threaded bolts can run through the roof of the rear housing, making it a hardpoint for attachments like a stabilizer to connect the two halves of the keyboard.

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

Given that independent movement of each half of the keyboard is not useful, each half can include a mounting plate for a stabilizing ‘beam’. That is a straight piece of wood, aluminium, rigid plastic etc. to connect the two halves mechanically and possibly carry the wire that connects them electrically.

This option is similar to rear housing, but the back plate block provides no interior space for an MCU etc. It is solid, with holes for threaded fasteners including the option of nut bosses. Its footprint is not part of a `bottom-plate`.

#### Parameter `include`

If `true`, include a back plate block.

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

The block is positioned in relation to a key mount.

##### Parameter `key-alias`

A named key where the block will attach. The vertical component of its position will be ignored.

##### Parameter `offset`

An offset in mm from the middle of the north wall of the selected key, at ground level, to the middle of the base of the back plate block.

### Section `bottom-plate`

A bottom plate can be added to close the case. This is useful mainly to simplify transportation.

#### Overview

The bottom plate is largely two-dimensional. The application builds it from a set of polygons, trying to match the perimeter of the case at the ground level (i.e. z = 0).

Specifically, there is one polygon per key cluster, limited to `full` wall edges, one polygon for the rear housing, and one set of polygons for each of the first-level case `tweaks` that use `to-ground`, ignoring chunk size and almost ignoring nested tweaks.

This methodology is mentioned here because its results are not perfect.Pending future features in OpenSCAD, a future version may be based on a more exact projection of the case, but as of 2018, such a projection is hollow and cannot be convex-hulled without escaping the case, unless your case is convex to start with.

If you require an exact match for the case, do the projection, save it as DXF/SVG etc. and post-process that file to fill the interior gap.

#### Interaction with wrist rests

If you include both `bottom-plate` and `wrist-rest`, you will get plates for the wrist rests too. These plates have no ESDS electronics to protect but serve other purposes: Covering nut pockets, covering silicone mould-pour cavities, covering plaster or other dense material poured into plinths printed without a bottom layer, and balancing the height of the different parts (case and rest, which must be connected).

There are other ways to balance the height, such as adjusting other parameters for the connection (doing separate renders) or adding silicone feet.

#### Parameter `include`

If `true`, include a bottom plate for the case.

#### Parameter `preview`

Preview mode. If `true`, put a model of the plate in the same file as the case it closes. Not for printing.

#### Parameter `thickness`

The thickness (i.e. height) in mm of the bottom plate.

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

Additional shapes. This is usually needed to bridge gaps between the walls of the key clusters.

The expected value here is an arbitrarily nested structure starting with a list. Each item in the list can follow one of the following patterns:

- A leaf node. This is a 3- or 4-tuple with contents specified below.
- A map, representing an instruction to combine nested items in a specific way.
- A list of any combination of the other two types. This type exists at the top level and as the immediate child of each map node.

Each leaf node identifies a particular set of key mount corner posts. These are identical to the posts used to build the walls, but this section gives you greater freedom in how to combine them. A leaf node must contain:

- A key alias defined under `key-clusters`.
- A key corner ID, such as `NNE` for north by north-east.
- A wall segment ID, which is an integer from 0 to 4.

Together, these identify a starting segment. Optionally, a leaf node may contain a second segment ID trailing the first. In that case, the leaf will represent the convex hull of the indicated segments plus all segments between them.

By default, a map node will create a convex hull around its child nodes. However, this behaviour can be modified. The following keys are recognized:

- `to-ground`: If `true`, child nodes will be extended vertically down to the ground plane, as with a `full` wall. See also: `bottom-plate`.
- `chunk-size`: Any integer greater than 1. If this is set, child nodes will not share a single convex hull. Instead, there will be a sequence of smaller hulls, each encompassing this many items.
- `highlight`: If `true`, render the node in OpenSCAD’s highlighting style. This is convenient while you work.
- `hull-around`: The list of child nodes. Required.

In the following example, `A` and `B` are aliases that would be defined elsewhere. The example is interpreted to mean that a plate should be created stretching from the south-by-southeast corner of `A` to the north-by-northeast corner of `B`. Due to `chunk-size` 2, that first plate will be joined, not hulled, with a second plate from `B` back to a different corner of `A`, with a longer stretch of wall segments down the corner of `A`.

```case:
  tweaks:
    - chunk-size: 2
      hull-around:
      - [A, SSE, 0]
      - [B, NNE, 0]
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

### Parameter `preview`

If `true`, render a visualization of the MCU for use in development.

### Parameter `type`

A symbolic name for a commercial product. Currently, only `promicro` is supported, referring to any MCU with the dimensions of a SparkFun Pro Micro.

### Parameter `margin`

A general measurement in mm of extra space around each part of the MCU, including PCB and USB connector. This is applied to DMOTE components meant to hold the MCU in place, accounting for printing inaccuracy as well as inaccuracies in manufacturing the MCU.

### Section `position`

Where to place the MCU.

#### Parameter `prefer-rear-housing`

If `true` and `rear-housing` is included, place the MCU in relation to the rear housing. Otherwise, place the MCU in relation to a key mount identified by `key-alias`.

#### Parameter `key-alias`

The name of a key at which to place the MCU if `prefer-rear-housing` is `false` or rear housing is not included.

#### Parameter `corner`

A code for a corner of the rear housing or of `key-alias`. This determines both the location and facing of the MCU.

#### Parameter `offset`

A 3D offset in mm, measuring from the `corner`.

#### Parameter `rotation`

A vector of 3 angles in radians. This parameter governs the rotation of the MCU around its anchor point in the front. You would not normally need this for the MCU.

### Section `support`

The support structure that holds the MCU PCBA in place.

#### Parameter `style`

The style of the support. Available styles are:

- `lock`: A separate physical object that is bolted in place over the MCU. This style is appropriate only with a rear housing, and then only when the PCB aligns with a long wall of that housing. It has the advantage that it can hug the connector on the PCB tightly, thus preventing a fragile surface-mounted connector from breaking off.
- `stop`: A gripper that holds the MCU in place at its rear end. This gripper, in turn, is held up by key mount webbing and is thus integral to the keyboard, not printed separately like the lock. This style does not require rear housing.

#### Parameter `preview`

If `true`, render a visualization of the support in place. This applies only to those parts of the support that are not part of the case model.

#### Parameter `height-factor`

A multiplier for the width of the PCB, producing the height of the support actually touching the PCB.

#### Parameter `lateral-spacing`

A lateral 1D offset in mm. With rear housing, this creates space between the rear housing itself and the back of the PCB’s through-holes, so it should be roughly matched to the length of wire overshoot. Without rear housing, it isn’t so useful but it does work analogously.

#### Section `lock`

Parameters relevant only with a `lock`-style support.

##### Section `fastener`

Threaded fasteners—a nut and a bolt—connect the lock to the case.

###### Parameter `style`

A style of bolt head (cap) supported by `scad-tarmi`.

###### Parameter `diameter`

The ISO metric diameter of the fastener.

##### Section `socket`

A housing around the USB connector on the MCU.

###### Parameter `thickness`

The wall thickness of the socket.

##### Section `bolt`

The part of a `lock`-style support that does not print as part of the keyboard case. This bolt, named by analogy with a lock, is not to be confused with the threaded fasteners holding it in place.

###### Parameter `clearance`

The distance of the bolt from the populated side of the PCB. This distance should be slightly greater than the height of the tallest component on the PCB.

###### Parameter `overshoot`

The distance across which the bolt will touch the PCB at the mount end. Take care that this distance is free of components on the PCB.

###### Parameter `mount-length`

The length of the base that contains the threaded fasteners used to secure the bolt over the MCU. This is in addition to `overshoot` and goes in the opposite direction, away from the PCB.

###### Parameter `mount-thickness`

The thickness of the mount. You will need a threaded fastener slightly longer than this.

#### Section `stop`

Parameters relevant only with a `stop`-style support.

##### Parameter `key-alias`

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

Because the DMOTE is split, there must be a signalling connection between its two halves. This section adds a socket for that purpose. For example, this might be a type 616E female for a 4P4C “RJ9” plug.

### Parameter `socket-size`

The size of a hole in the case, for the female to fit into.

### Section `position`

Where to place the socket. Equivalent to `connection` → `mcu`.

#### Parameter `prefer-rear-housing`



#### Parameter `key-alias`



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

- `threaded`: threaded fasteners connect the case and wrist rest. This works with a great variety of keyboard shapes and will allow adjusting the position of the wrist rest for different hands.
- `solid`: a printed plastic bridge along the ground as part of the model. This has more limitations, both in manufacture and in use. It includes a hook on the near outward side of the case, which will only be useful if the case wall at that point is short and the wrist rest is attached to a key cluster whose third column (column 2) is positioned and walled in such a way that the solid bridge can be wedged between the hook and the column.

### Parameter `preview`

Preview mode. If `true`, this puts a model of the wrist rest in the same OpenSCAD file as the case. That model is simplified, intended for gauging distance, not for printing.

### Section `position`

The wrist rest is positioned in relation to a key mount.

#### Parameter `key-alias`

A named key where the wrist rest will attach. The vertical component of its position will be ignored.

#### Parameter `offset`

An offset in mm from the selected key to one corner of the base of the wrist rest. Specifically, it is the corner close to the keyboard case, on the right-hand side of the right-hand half.

### Section `shape`

The wrist rest needs to fit the user’s hand.

#### Parameter `plinth-base-size`

The size of the plinth up to but not including the narrowing upper lip and rubber parts.

#### Parameter `chamfer`

A distance in mm. The plinth is shrunk and then regrown by this much to chamfer its corners.

#### Parameter `lip-height`

The height of a narrowing, printed lip between the base of the plinth and the rubber part.

#### Section `pad`

The top of the wrist rest should be printed or cast in a soft material, such as silicone rubber.

##### Parameter `surface-heightmap`

A filepath. The path, and file, will be interpreted by OpenScad, using its [`surface()` function](https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/Other_Language_Features#Surface).

The file should contain a heightmap to describe the surface of the rubber pad.

##### Section `height`

The piece of rubber extends a certain distance up into the air and down into the plinth. All measurements in mm.

###### Parameter `surface-range`

The vertical range of the heightmap. Whatever values are in the heightmap will be normalized to this scale.

###### Parameter `lip-to-surface`

The part of the rubber pad between the top of the lip and the point where the heightmap comes into effect. This is useful if your heightmap itself has very low values at the edges, such that moulding and casting it without a base would be difficult.

###### Parameter `below-lip`

The depth of the rubber wrist support, measured from the top of the lip, going down into the plinth. This part of the pad just keeps it in place.

### Section `fasteners`

This is only relevant with the `threaded` style of wrist rest.

#### Parameter `amount`

The number of fasteners connecting each case to its wrist rest.

#### Parameter `diameter`

The ISO metric diameter of each fastener.

#### Parameter `length`

The length in mm of each fastener.

#### Section `height`

The vertical level of the fasteners.

##### Parameter `first`

The distance in mm from the bottom of the first fastener down to the ground level of the model.

##### Parameter `increment`

The vertical distance in mm from the center of each fastener to the center of the next.

#### Section `mounts`

The mounts, or anchor points, for each fastener on each side.

##### Parameter `width`

The width in mm of the face or front bezel on each connecting block that will anchor a fastener.

##### Section `case-side`

The side of the keyboard case.

###### Parameter `key-alias`

A named key. A mount point on the case side will be placed near this key.

###### Parameter `offset`

An two-dimensional vector offset in mm from the key to the mount.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s).

###### Section `nuts`

Extra features for threaded nuts on the case side.

###### Section `bosses` at level 7

Nut bosses on the rear (interior) of the mount. You may want this if the distance between case and plinth is big enough for a nut. If that distance is too small, bosses can be counterproductive.

###### Parameter `include` at level 8

If `true`, include bosses.

##### Section `plinth-side`

The side of the wrist rest.

###### Parameter `offset`

The offset in mm from the corner of the plinth to the fastener mount point attached to the plinth.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s). This is typically larger than the case-side depth to allow adjustment.

###### Parameter `pocket-height`

The height of the nut pocket inside the mounting plate, in mm.

With a large positive value, this will provide a chute for the nut(s) to go in from the top of the plinth, which allows you to hide the hole beneath the pad. With a large negative value, the pocket will instead open from the bottom, which is convenient if `depth` is small. With a small value or the default value of zero, it will be necessary to pause printing in order to insert the nut(s); this last option is therefore recommended for advanced users only.

### Section `solid-bridge`

This is only relevant with the `solid` style of wrist rest.

#### Parameter `width`

The width in mm of the land bridge between the case and the plinth.

On the right-hand side of the keyboard, the bridge starts from the wrist rest `key-alias` and extends this many mm to the left.

The value of this parameter, and the shape of the keyboard case, should be arranged in a such a way that the land bridge is wedged in place by a vertical wall on that left side.

#### Parameter `height`

The height in mm of the land bridge between the case and the plinth.

## Section `dfm`

Settings for design for manufacturability (DFM).

### Parameter `error`

A measurement in mm of errors introduced to negative space in the xy plane by slicer software and the printer you will use.

The default value is zero. An appropriate value for a typical slicer and FDM printer with a 0.5 mm nozzle would be about -0.5 mm.

This application will try to compensate for the error, though only for certain sensitive inserts, not for the case as a whole.

## Section `mask`

A box limits the entire shape, cutting off any projecting byproducts of the algorithms. By resizing and moving this box, you can select a subsection for printing. You might want this while you are printing prototypes for a new style of switch, MCU support etc.

### Parameter `size`

The size of the mask in mm. By default, `[1000, 1000, 1000]`.

### Parameter `center`

The position of the center point of the mask. By default, `[0, 0, 500]`, which is supposed to mask out everything below ground level.

⸻

This document was generated from the application CLI.
