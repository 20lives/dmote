# Configuration options

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant.

This documentation was generated from the application CLI.

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

### Parameter `travel`

The distance in mm that a keycap can travel vertically when mounted on a switch.

## Section `key-clusters`

This section describes where to put keys on the keyboard.

### Section `finger`

The main cluster of keys, for “fingers” in a sense excluding the thumb.Everything else is placed in relation to the finger cluster.

#### Parameter `style`

Cluster layout style. One of:

* `standard`: Both columns and rows have the same type of curvature applied in a logically consistent manner.* `orthographic`: Rows are curved somewhat differently. This creates more space between columns and may prevent key mounts from fusing together if you have a broad matrix.

#### Parameter `matrix-columns`

A list of key columns. Columns are aligned with the user’s fingers. Each column will be known by its index in this list, starting at zero for the first item. Each item may contain:

* `rows-above-home`: An integer specifying the amount of keys on the far side of the home row in the column. If this parameter is omitted, the effective value will be zero.
* `rows-below-home`: An integer specifying the amount of keys on the near side of the home row in the column. If this parameter is omitted, the effective value will be zero.

For example, on a normal QWERTY keyboard, H is on the home row for purposes of touch typing, and you would probably want to use it as such here too, even though the matrix in this program has no necessary relationship with touch typing, nor with the matrix in your MCU firmware (TMK/QMK etc.). Your H key will then get the coordinates [0, 0] as the home-row key in the far left column on the right-hand side of the keyboard.

In that first column, to continue the QWERTY pattern, you will want `rows-above-home` set to 1, to make a Y key, or 2 to make a 6-and-^ key, or 3 to make a function key above the 6-and-^. Your Y key will have the coordinates [0, 1]. Your 6-and-^ key will have the coordinates [0, 2], etc.

Still in that first column, to finish the QWERTY pattern, you will want `rows-below-home` set to 2, where the two keys below H are N (coordinates [0, -1]) and Space (coordinates [0, -2]).

The next item in the list will be column 1, with J as [1, 0] and so on. On the left-hand side of a DMOTE, everything is mirrored so that [0, 0] will be G instead of H in QWERTY, [1, 0] will be F instead of J, and so on.

#### Parameter `aliases`

A map of short names to specific keys by coordinate pair. Such aliases are for use elsewhere in the configuration.

### Section `thumb`

A cluster of keys just for the thumb.

#### Section `position`

The thumb cluster is positioned in relation to the finger cluster.

##### Parameter `key-alias`

A finger key as named under `aliases` above.

##### Parameter `offset`

A 3-dimensional offset in mm from the indicated key.

#### Parameter `style`

As for the finger cluster.

#### Parameter `matrix-columns`

As for the finger cluster.

#### Parameter `aliases`

As for the finger cluster. Note, however, that aliases must be unique even between clusters.

## Section `by-key`

This section is special. It’s nested for all levels of specificity.

### Section `parameters`

This section, and everything in it, can be repeated at several levels: Here at the global level, for each key cluster, for each column, and at the row level. See below. Only the most specific option available for each key will be applied to that key.

#### Section `layout`

How to place keys.

##### Section `matrix`

Roughly how keys are spaced out to form a matrix.

###### Section `neutral`

The neutral point in a column or row is where any progressive curvature both starts and has no effect.

###### Parameter `column` at level 7

An integer column ID.

###### Parameter `row` at level 7

An integer row ID.

###### Section `separation`

Tweaks to control the systematic separation of keys. The parameters in this section will be multiplied by the difference between each affected key’s coordinates and the neutral column and row.

###### Parameter `column` at level 7

A distance in mm.

###### Parameter `row` at level 7

A distance in mm.

##### Section `pitch`

Tait-Bryan pitch, meaning the rotation of keys around the x axis.

###### Parameter `base`

An angle in radians. Set at a high level, this controls the general front-to-back incline of a key cluster.

###### Parameter `intrinsic`

An angle in radians. Intrinsic pitching occurs early in key placement. It is typically intended to produce a tactile break between two rows of keys, as in the typewriter-like terracing common on flat keyboards with OEM-profile or similarly angled caps.

###### Parameter `progressive`

An angle in radians. This progressive pitch factor bends columns lengthwise. If set to zero, columns are flat.

##### Section `roll`

Tait-Bryan roll, meaning the rotation of keys around the y axis.

###### Parameter `base`

An angle in radians. This is the “tenting” angle. Applied to the finger cluster, it controls the overall left-to-right tilt of each half of the keyboard.

###### Parameter `intrinsic`

An angle in radians, analogous to intrinsic pitching. Where more than one column of keys is devoted to a single finger at the edge of the keyboard, this can help make the edge column easier to reach, reducing the need to bend the finger (or thumb) sideways.

###### Parameter `progressive`

An angle in radians. This progressive roll factor bends rows lengthwise, which also gives the columns a lateral curvature.

##### Section `yaw`

Tait-Bryan yaw, meaning the rotation of keys around the z axis.

###### Parameter `base`

An angle in radians. Applied to the finger key cluster, this serves the purpose of allowing the user to keep their wrists straight even if the two halves of the keyboard are closer together than the user’s shoulders.

###### Parameter `intrinsic`

An angle in radians, analogous to intrinsic pitching.

##### Section `translation`

Translation in the geometric sense, displacing keys in relation to each other. Depending on when this translation takes places, it may have a a cascading effect on other aspects of key placement. All measurements are three-dimensional vectors in mm.

###### Parameter `early`

”Early” translation happens before other operations in key placement and therefore has the biggest knock-on effects.

###### Parameter `mid`

This happens after columns are styled but before base pitch and roll. As such it is a good place to adjust whole columns for relative finger length.

###### Parameter `late`

“Late” translation is the last step in key placement and therefore interacts very little with other steps. As a result, the z-coordinate, which is the last number in this vector, serves as a general vertical offset of the finger key cluster from the ground plane. If set at a high level, this controls the overall height of the keyboard, including the height of the case walls.

#### Section `channel`

Above each switch mount, there is a channel of negative space for the user’s finger and the keycap to move inside. This is only useful in those cases where nearby walls or webbing between mounts on the keyboard would otherwise obstruct movement.

##### Parameter `height`

The height in mm of the negative space, starting from the bottom edge of each keycap in its pressed (active) state.

##### Parameter `top-width`

The width in mm of the negative space at its top. Its width at the bottom is defined by keycap geometry.

##### Parameter `margin`

The width in mm of extra negative space around the edges of a keycap, on all sides.

#### Section `wall`

The walls of the keyboard case support the key mounts and protect the electronics. They are generated by an algorithm that walks around each key cluster.

This section determines the shape of the case wall, specifically the skirt around each key mount along the edges of the board. These skirts are made up of convex hulls wrapping sets of corner posts.

There is one corner post at each actual corner of every key mount. More posts are displaced from it, going down the sides. Their placement is affected by the way the key mounts are rotated etc.

##### Parameter `thickness`

A distance in mm.

This is actually the distance between some pairs of corner posts (cf. `key-mount-corner-margin`), in the key mount’s frame of reference. It is therefore inaccurate as a measure of wall thickness on the x-y plane.

##### Parameter `bevel`

A distance in mm.

This is applied at the very top of a wall, making up the difference between wall segments 0 and 1. It is applied again at the bottom, making up the difference between segments 3 and 4.

##### Section `north`

Throughout the program, “north” refers to the side of a key facing directly away from the user, barring yaw.

This section describes the shape of the wall on the north side of the keyboard. There are identical sections for the other cardinal directions.

###### Parameter `extent`

Two types of values are permitted here:

* The keyword `full`. This means a complete wall extending from the key mount all the way down to the ground via segments numbered 0 through 4 and a vertical drop thereafter.
* An integer corresponding to the last wall segment to be included. A zero means there will be no wall. No matter the number, there will be no vertical drop to the floor.

###### Parameter `parallel`

A distance in mm. The later wall segments extend this far away from the corners of their key mount, on its plane.

###### Parameter `perpendicular`

A distance in mm. The later wall segments extend this far away from the corners of their key mount, away from its plane.

##### Section `east`

See `north`.

###### Parameter `extent`



###### Parameter `parallel`



###### Parameter `perpendicular`



##### Section `south`

See `north`.

###### Parameter `extent`



###### Parameter `parallel`



###### Parameter `perpendicular`



##### Section `west`

See `north`.

###### Parameter `extent`



###### Parameter `parallel`



###### Parameter `perpendicular`



### Section `clusters` ← overrides go in here

This is an anchor point for overrides of the `parameters` section described above. Overrides start at the key cluster level. This section therefore permits keys that identify specific key clusters.

For each such key, two subsections are permitted: A new, more specific `parameters` section and a `columns` section. Columns are indexed by their ordinal integers or the words “first” or “last”, which take priority.

A column can have its own `parameters` and its own `rows`, which are indexed in relation to the home row or again with “first” or “last”. Finally, each row can have its own `parameters`, which are specific to the full combination of cluster, column and row.

WARNING: Due to a peculiarity of the YAML parser, take care to quote your numeric column and row indices as strings.

In the following example, the parameter `P`, which is not really supported, will have the value “true” for all keys except the one closest to the user (“first” row) in the second column from the left on the right-hand side of the keyboard (column 1; this is the second from the right on the left-hand side of the keyboard; note quotation marks).

```by-key:
  parameters:
    P: true
  clusters:
    finger:
      columns:
        "1":
          rows:
            first:
              parameters:
                P: false
```

## Section `case`

Much of the keyboard case is generated from the `wall` parameters above. This section deals with lesser features of the case.

### Parameter `key-mount-thickness`

The thickness in mm of each switch key mounting plate.

### Parameter `key-mount-corner-margin`

The thickness in mm of an imaginary “post” at each corner of each key mount. Copies of such posts project from the key mounts to form the main walls of the case.

`key-mount-thickness` is similarly the height of each post.

### Parameter `web-thickness`

The thickness in mm of the webbing between switch key mounting plates and of the rear housing’s walls and roof.

### Section `rear-housing`

The furthest row of the key cluster can be extended into a rear housing for the MCU and various other features.

#### Parameter `include`

If `true`, add a rear housing. Please arrange case walls so as not to interfere, by removing them along the far side of the last row of key mounts in the finger cluster.

#### Parameter `distance`

The horizontal distance in mm between the furthest key in the row and the roof of the rear housing.

#### Parameter `height`

The height in mm of the roof, over the floor.

#### Section `offsets`

Modifiers for the size of the roof. All are in mm.

##### Parameter `north`

The extent of the roof on the y axis; its horizontal depth.

##### Parameter `west`

The extent on the x axis past the first key in the row.

##### Parameter `east`

The extent on the x axis past the last key in the row.

### Section `back-plate`

Given that independent movement of each half of the keyboard is not useful, each half can include a mounting plate for a stabilizing ‘beam’. That is a straight piece of wood, aluminium, rigid plastic etc. to connect the two halves mechanically and possibly carry the wire that connects them electrically.

This option is similar to rear housing, but the back plate block provides no interior space for an MCU etc. It is solid, with holes for threaded fasteners and the option of nut bosses.

#### Parameter `include`

If `true`, include a back plate block.

#### Parameter `beam-height`

The nominal vertical extent of the back plate in mm. Because the plate is bottom-hulled to the floor, the effect of this setting is on the area of the plate above its holes.

#### Section `fasteners`

Two threaded fasteners run through the back plate.

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

### Section `leds`

Support for light-emitting diodes in the case walls.

#### Parameter `include`

If `true`, cut slots for LEDs out of the case wall, facing the space between the two halves.

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

Additional shapes. This is usually needed to bridge gaps between the walls of the finger and key clusters.

The expected value here is an arbitrarily nested structure starting with a list. Each item in the list can follow one of the following patterns:

* A leaf node. This is a 3- or 4-tuple list with contents specified below.
* A map, representing an instruction to combine nested items in a specific way.
* A list of any combination of the other two types. This type exists at the top level and as the immediate child of each map node.

Each leaf node identifies particular set of key mount corner posts. These are identical to the posts used to build the walls (see above), but this section gives you greater freedom in how to combine them. A leaf node must contain:

* A key alias defined under `key-clusters`.
* A key corner ID, such as `NNE` for north by north-east.
* A wall segment ID, which is an integer from 0 to 4.

Together, these identify a starting segment. Optionally, a leaf node may contain a second segment ID trailing the first. In that case, the leaf will represent the convex hull of the first and second indicated segments, plus all in between.

By default, a map node will create a convex hull around its child nodes. However, this behaviour can be modified. The following keys are recognized:

* `to-ground`: If `true`, child nodes will be extended vertically down to the ground plane, as with a `full` wall.
* `chunk-size`: Any integer greater than 1. If this is set, child nodes will not share a single convex hull. Instead, there will be a sequence of smaller hulls, each encompassing this many items.
* `highlight`: If `true`, render the node in OpenSCAD’s highlighting style. This is convenient while you work.
* `hull-around`: The list of child nodes. Required.

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

* `lock`: A separate physical object that is screwed in place over the MCU. This style is appropriate only with a rear housing, and then only when the PCB aligns with a long wall of that housing. It has the that it can hug the connector on the PCB tightly.
* `stop`: A gripper that holds the MCU in place at its rear end. This gripper, in turn, is held up by key mount webbing and is thus integral to the keyboard, not printed separately like the lock.

#### Parameter `preview`

If `true`, render a visualization of the support in place. This applies only to those parts of the support that are not part of the case model.

#### Parameter `height-factor`

A multiplier for the width of the PCB, producing the height of the support actually touching the PCB.

#### Parameter `lateral-spacing`

A lateral 1D offset in mm. With rear housing, this creates space between the rear housing itself and the back of the PCB’s through-holes, so it should be roughly matched to the length of wire overshoot. Without rear housing, it isn’t so useful but it does work analogously.

#### Section `lock`

Parameters relevant only with a `lock`-style support.

##### Parameter `fastener-diameter`

The diameter in mm of the flat-head fastener closing the lock.

##### Section `socket`

A housing around the USB connector on the MCU.

###### Parameter `thickness`

The wall thickness of the socket.

##### Section `bolt`

The part of a `lock`-style support that does not print as part of the keyboard case.

###### Parameter `clearance`

The distance of the bolt from the populated side of the PCB. This distance should be slightly greater than the height of the tallest component on the PCB.

###### Parameter `overshoot`

The distance across which the bolt will touch the PCB at the mount end. Take care that this distance is free of components on the PCB.

###### Parameter `mount-length`

The length of the part of the bolt that is screwed into place against the case. This is in addition to `overshoot` and goes in the opposite direction.

###### Parameter `mount-thickness`

The thickness of the mount. This is the major determinant of the length of d you will need.

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

* `threaded`: threaded fastener(s) connect the case and wrist rest.
* `solid`: a printed plastic bridge along the ground as part of the model.

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

##### Section `plinth-side`

The side of the wrist rest.

###### Parameter `offset`

The offset in mm from the corner of the plinth to the fastener mount point attached to the plinth.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s). This is typically larger than the case-side depth to allow adjustment.

### Section `solid-bridge`

This is only relevant with the `solid` style of wrist rest.

#### Parameter `width`

The width in mm of the land bridge between the case and the plinth.

On the right-hand side of the keyboard, the bridge starts from the wrist rest `key-alias` and extends this many mm to the left.

The value of this parameter, and the shape of the keyboard case, should be arranged in a such a way that the land bridge is wedged in place by a vertical wall on that left side.

#### Parameter `height`

The height in mm of the land bridge between the case and the plinth.

## Section `mask`

A box limits the entire shape, cutting off any projecting byproducts of the algorithms. By resizing and moving this box, you can select a subsection for printing. You might want this while you are printing prototypes for a new style of switch, MCU support etc.

### Parameter `size`

The size of the mask in mm. By default, `[1000, 1000, 1000]`.

### Parameter `center`

The position of the center point of the mask. By default, `[0, 0, 500]`, which is supposed to mask out everything below ground level.
