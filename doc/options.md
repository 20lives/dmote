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

#### Parameter `aliases`

A map of short names to specific keys by coordinate pair. Such aliases are for use elsewhere in the configuration.

### Section `thumb`

A cluster of keys just for the thumb.

#### Section `position`

The thumb cluster is positioned in relation to the finger cluster.

##### Parameter `key-alias`

A finger key coordinate pair as named under `aliases` above.

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

####### Parameter `column`

An integer column ID.

####### Parameter `row`

An integer row ID.

###### Section `separation`

Tweaks to control the systematic separation of keys. The parameters in this section will be multiplied by the difference between each affected key’s coordinates and the neutral column and row.

####### Parameter `column`

A distance in mm.

####### Parameter `row`

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

###### Parameter `progressive`

An angle in radians. This progressive roll factor bends rows lengthwise, which also gives the columns a lateral curvature.

##### Section `yaw`

Tait-Bryan yaw, meaning the rotation of keys around the z axis.

###### Parameter `base`

An angle in radians.

##### Section `translation`

Translation in the geometric sense, displacing keys in relation to each other. Depending on when this translation takes places, it may have a a cascading effect on other aspects of key placement. All measurements are in mm.

###### Parameter `early`

A 3-dimensional vector. ”Early” translation happens before other operations in key placement and therefore has the biggest knock-on effects.

###### Parameter `mid`

A 3-dimensional vector. This happens after columns are styled but before base pitch and roll. As such it is a good place to adjust whole columns for relative finger length.

###### Parameter `late`

A 3-dimensional vector. “Late” translation is the last step in key placement and therefore interacts very little with other steps. As a result, the z-coordinate, which is the last number in this vector, serves as a general vertical offset of the finger key cluster from the ground plane. If set at a high level, this controls the overall height of the keyboard, including the height of the case walls.

#### Section `channel`

Above each switch mount, there is a channel of negative space for the user’s finger and the keycap to move inside. This is only useful in those cases where nearby walls or webbing between mounts on the keyboard would otherwise obstruct movement.

##### Parameter `height`

The height in mm of the negative space, starting from the bottom edge of each keycap in its pressed (active) state.

##### Parameter `top-width`

The width in mm of the negative space at its top. Its width at the bottom is defined by the keycap.

##### Parameter `margin`

The width in mm of extra negative space around the edges of a keycap, on all sides.

#### Section `wall`

The walls of the keyboard case support the key mounts and protect the electronics. They are generated by an algorithm that walks around each key cluster.

This section determines the shape of the case wall, specifically the skirt around each key mount along the edges of the board. These skirts are made up of geometric hulls wrapping sets of corner posts.

There is one corner post at each actual corner of every key mount. More posts are displaced from it, going down the sides. Their placement is affected by the way the key mounts are rotated etc.

##### Parameter `thickness`

A distance in mm.

This is actually the distance between some pairs of corner posts, in the key mount’s frame of reference. It is therefore inaccurate as a measure of wall thickness on the x-y plane.

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

Undocumented.

###### Parameter `parallel`

Undocumented.

###### Parameter `perpendicular`

Undocumented.

##### Section `south`

See `north`.

###### Parameter `extent`

Undocumented.

###### Parameter `parallel`

Undocumented.

###### Parameter `perpendicular`

Undocumented.

##### Section `west`

See `north`.

###### Parameter `extent`

Undocumented.

###### Parameter `parallel`

Undocumented.

###### Parameter `perpendicular`

Undocumented.

### Section `clusters` ← overrides go in here

This is an anchor point for overrides of the `parameters` section described above. Overrides start at the key cluster level. This section therefore permits keys that identify specific key clusters.

For each such key, two subsections are permitted: A new, more specific `parameters` section and a `columns` section. Columns are indexed by their ordinal integers or the words “first” or “last”, which take priority.

A column can have its own `parameters` and its own `rows`, which are indexed in relation to the home row or again with “first” or “last”. Finally, each row can have its own `parameters`, which are specific to the full combination of cluster, column and row.

WARNING: Due to a peculiarity of the YAML parser, take care to quote your numeric column and row indices as strings.

In the following example, the parameter `P`, which is not really supported, will have the value “true” for all keys except the one closest to the user (“first” row) in the second column from the left on the right-hand side of the keyboard (column 1; this is the second from the right on the left-hand side of the keyboard).

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
                P: false```

## Section `case`

The most important part of the keyboard case is generated from the `wall` parameters above. This section deals with lesser features of the case.

### Parameter `tweaks`

Additional shapes. This is usually needed to bridge gaps between the walls of the finger and key clusters.

The expected value here is an arbitrarily nested structure, starting with a list. Each item in the list can follow one of the following patterns:

* A leaf node. This is a 3- or 4-tuple list with contents specified below.
* A map, representing an instruction to combine nested items in a specific way.
* A list of any combination of the other two types. This type exists at the top level, and as the immediate child of each map node.

Each leaf node identifies particular set of key mount corner posts. These are identical to the posts used to build the walls (see above), but this section gives you greater freedom in how to combine them. A leaf node must contain:

* A key alias defined under `key-clusters`.
* A key corner ID, such as `NNE` for north by north-east.
* A wall segment ID (0 to 4).

Together, these identify a starting segment. Optionally, a leaf node may contain a second segment ID trailing the first. In that case, the leaf will represent the geometric hull of the first and second indicated segments, plus all in between.

By default, a map node will create a geometric hull around its child nodes. However, this behaviour can be modified. The following keys are recognized:

* `to-ground`: If `true`, child nodes will be extended vertically down to the ground plane, as with a `full` wall.
* `chunk-size`: Any integer greater than 1. If this is set, child nodes will not share a single geometric hull. Instead, there will be a sequence of smaller hulls, each encompassing this many items.
* `highlight`: If `true`, render the node in OpenSCAD’s highlighting style. This is convenient while you work.
* `hull-around`: The list of child nodes. Required.

In the following example, `A` and `B` are aliases that would be defined elsewhere. The example is interpreted to mean that a plate should be created stretching from the south-by-southeast corner of `A` to the north-by-northeast corner of `B`. Due to `chunk-size` 2, that first plate will be joined, not hulled, with a second plate from `B` back to a different corner of `A`, with a longer stretch of wall segments down the corner of `A`.

```case:
  tweaks:
    - chunk-size: 2
      hull-around:
      - [A, SSE, 0]
      - [B, NNE, 0]
      - [A, SSW, 0, 4]```


### Section `foot-plates`

Optional flat surfaces at ground level for adding silicone rubber feet or cork strips etc. to the bottom of the keyboard to increase friction and/or improve feel, sound and ground clearance.

#### Parameter `include`

If `true`, include foot plates.

#### Parameter `height`

The height in mm of each mounting plate.

#### Parameter `polygons`

A list describing the horizontal shape, size and position of each mounting plate as a polygon.

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

The wrist rest is positioned in relation to a specific key.

#### Parameter `finger-key-column`

A finger key column ID. The wrist rest will be attached to the first key in that column.

#### Parameter `key-corner`

A corner for the first key in the column.

#### Parameter `offset`

An offset in mm from the selected key.

### Parameter `plinth-base-size`

The size of the plinth up to but not including the narrowing upper lip and rubber parts.

### Parameter `lip-height`

The height of a narrowing, printed lip between the base of the plinth and the rubber part.

### Section `rubber`

The top of the wrist rest should be printed or cast in a soft material, such as silicone rubber.

#### Section `height`

The piece of rubber extends a certain distance up into the air and down into the plinth.

##### Parameter `above-lip`

The height of the rubber wrist support, measured from the top of the lip.

##### Parameter `below-lip`

The depth of the rubber wrist support, measured from the top of the lip.

#### Section `shape`

The piece of rubber should fit the user’s hand.

##### Parameter `grid-size`

Undocumented.

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

###### Parameter `finger-key-column`

A finger key column ID. On the case side, fastener mounts will be attached at ground level near the first key in that column.

###### Parameter `key-corner`

A corner of the key identified by `finger-key-column`.

###### Parameter `offset`

An offset in mm from the corner of the finger key to the mount.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s).

##### Section `plinth-side`

The side of the wrist rest.

###### Parameter `offset`

The offset in mm from the nearest corner of the plinth to the fastener mount attached to the plinth.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s). This is typically larger than the case-side depth to allow adjustment.

### Section `solid-bridge`

This is only relevant with the `solid` style of wrist rest.

#### Parameter `height`

The height in mm of the land bridge between the case and the plinth.
