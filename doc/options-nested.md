# Configuration options

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant. The document was generated from the application CLI.

This document describes all those settings which can be made at any level of specificity, from the entire keyboard down to an individual key.

## Conceptual overview

Variable specificity is accomplished by nesting. The following levels of specificity are currently available. Each one branches out, containing the next:

* The global level, at `by-key` (cf. the [main document](options-main.md)).
* The key cluster level, at `by-key` → `clusters` → your cluster.
* The column level, nested still further under your cluster → `columns` → column index.
* The row level, nested under column index → `rows` → row index.

Each setting takes precedence over any copies of that specific setting at less specific levels. For example, any parameter at the row level is specific not only to a row but to the full combination of cluster, column and row in the chain that leads down to the row. The same setting made at any of those higher levels will be ignored in favour of the most specific setting. Conversely, a setting made on a specific row will not affect the same row in other columns.

At each level, two subsections are permitted: `parameters`, where you put the settings themselves, and a section for the next level of nesting: `clusters`, then `columns`, then `rows`. In effect, the row level is the key level and forms an exception, in that there are no further levels below it.

In the following hypothetical example, the parameter `P`, which is not really supported, will have the value “true” for all keys except the one closest to the user (“first” row) in the second column from the left on the right-hand side of the keyboard (column 1; this is the second from the right on the left-hand side of the keyboard).

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

Columns and rows are indexed by their ordinal integers or the words “first” or “last”, which take priority.

WARNING: Due to a peculiarity of the YAML parser, take care to quote your numeric column and row indices as strings. This is why there are quotation marks around column index 1 in the example.

## Section `parameters`

This section, and everything in it, can be repeated at each level of specificity.

### Section `layout`

Settings for how to place keys.

#### Section `matrix`

Roughly how keys are spaced out to form a matrix.

##### Section `neutral`

The neutral point in a column or row is where any progressive curvature both starts and has no effect.

###### Parameter `column`

An integer column ID.

###### Parameter `row`

An integer row ID.

##### Section `separation`

Tweaks to control the systematic separation of keys. The parameters in this section will be multiplied by the difference between each affected key’s coordinates and the neutral column and row.

###### Parameter `column`

A distance in mm.

###### Parameter `row`

A distance in mm.

#### Section `pitch`

Tait-Bryan pitch, meaning the rotation of keys around the x axis.

##### Parameter `base`

An angle in radians. Set at a high level, this controls the general front-to-back incline of a key cluster.

##### Parameter `intrinsic`

An angle in radians. Intrinsic pitching occurs early in key placement. It is typically intended to produce a tactile break between two rows of keys, as in the typewriter-like terracing common on flat keyboards with OEM-profile or similarly angled caps.

The term “intrinsic” is used here because the key spins roughly around its own center. The term should not be confused with intrinsic rotations in the sense that each step is performed on a coordinate system resulting from previous operations.

##### Parameter `progressive`

An angle in radians. This progressive pitch factor bends columns lengthwise. If set to zero, columns are flat.

#### Section `roll`

Tait-Bryan roll, meaning the rotation of keys around the y axis.

##### Parameter `base`

An angle in radians. This is the “tenting” angle. Applied to the finger cluster, it controls the overall left-to-right tilt of each half of the keyboard.

##### Parameter `intrinsic`

An angle in radians, analogous to intrinsic pitching. Where more than one column of keys is devoted to a single finger at the edge of the keyboard, this can help make the edge column easier to reach, reducing the need to bend the finger (or thumb) sideways.

##### Parameter `progressive`

An angle in radians. This progressive roll factor bends rows lengthwise, which also gives the columns a lateral curvature.

#### Section `yaw`

Tait-Bryan yaw, meaning the rotation of keys around the z axis.

##### Parameter `base`

An angle in radians. Applied to the finger key cluster, this serves the purpose of allowing the user to keep their wrists straight even if the two halves of the keyboard are closer together than the user’s shoulders.

##### Parameter `intrinsic`

An angle in radians, analogous to intrinsic pitching.

#### Section `translation`

Translation in the geometric sense, displacing keys in relation to each other. Depending on when this translation takes places, it may have a a cascading effect on other aspects of key placement. All measurements are three-dimensional vectors in mm.

##### Parameter `early`

”Early” translation happens before other operations in key placement and therefore has the biggest knock-on effects.

##### Parameter `mid`

This happens after columns are styled but before base pitch and roll. As such it is a good place to adjust whole columns for relative finger length.

##### Parameter `late`

“Late” translation is the last step in key placement and therefore interacts very little with other steps. As a result, the z-coordinate, which is the last number in this vector, serves as a general vertical offset of the finger key cluster from the ground plane. If set at a high level, this controls the overall height of the keyboard, including the height of the case walls.

### Section `channel`

Above each switch mount, there is a channel of negative space for the user’s finger and the keycap to move inside. This is only useful in those cases where nearby walls or webbing between mounts on the keyboard would otherwise obstruct movement.

#### Parameter `height`

The height in mm of the negative space, starting from the bottom edge of each keycap in its pressed (active) state.

#### Parameter `top-width`

The width in mm of the negative space at its top. Its width at the bottom is defined by keycap geometry.

#### Parameter `margin`

The width in mm of extra negative space around the edges of a keycap, on all sides.

### Section `wall`

The walls of the keyboard case support the key mounts and protect the electronics. They are generated by an algorithm that walks around each key cluster.

This section determines the shape of the case wall, specifically the skirt around each key mount along the edges of the board. These skirts are made up of convex hulls wrapping sets of corner posts.

There is one corner post at each actual corner of every key mount. More posts are displaced from it, going down the sides. Their placement is affected by the way the key mounts are rotated etc.

#### Parameter `thickness`

A distance in mm.

This is actually the distance between some pairs of corner posts (cf. `key-mount-corner-margin`), in the key mount’s frame of reference. It is therefore inaccurate as a measure of wall thickness on the x-y plane.

#### Parameter `bevel`

A distance in mm.

This is applied at the very top of a wall, making up the difference between wall segments 0 and 1. It is applied again at the bottom, making up the difference between segments 3 and 4.

#### Section `north`

As explained [elsewhere](intro.md), “north” refers to the side facing away from the user, barring yaw.

This section describes the shape of the wall on the north side of the keyboard. There are identical sections for the other cardinal directions.

##### Parameter `extent`

Two types of values are permitted here:

* The keyword `full`. This means a complete wall extending from the key mount all the way down to the ground via segments numbered 0 through 4 and a vertical drop thereafter.
* An integer corresponding to the last wall segment to be included. A zero means there will be no wall. No matter the number, there will be no vertical drop to the floor.

##### Parameter `parallel`

A distance in mm. The later wall segments extend this far away from the corners of their key mount, on its plane.

##### Parameter `perpendicular`

A distance in mm. The later wall segments extend this far away from the corners of their key mount, away from its plane.

#### Section `east`

See `north`.

##### Parameter `extent`



##### Parameter `parallel`



##### Parameter `perpendicular`



#### Section `south`

See `north`.

##### Parameter `extent`



##### Parameter `parallel`



##### Parameter `perpendicular`



#### Section `west`

See `north`.

##### Parameter `extent`



##### Parameter `parallel`



##### Parameter `perpendicular`



## Parameter `clusters` ← overrides go in here

Starting here, you gradually descend from the global level toward the key level.
