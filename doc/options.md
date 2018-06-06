# Configuration options

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant.

This documentation was generated from the application CLI.

## Section `key-clusters`

This section describes where to put keys on the keyboard.

### Section `finger`

The main cluster of keys, for “fingers” in a sense excluding the thumb.

#### Parameter `preview`

If `true`, include models of the keycaps. This is intended for illustration in development, not for printing.

#### Parameter `matrix-columns`

A list of key columns. Columns are aligned with the user’s fingers. Each column will be known by its index in this list, starting at zero for the first item. Each item may contain:

* `rows-above-home`: An integer specifying the amount of keys on the far side of the home row in the column. If this parameter is omitted, the effective value will be zero.
* `rows-below-home`: An integer specifying the amount of keys on the near side of the home row in the column. If this parameter is omitted, the effective value will be zero.

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

## Section `foot-plates`

Optional flat surfaces at ground level for adding silicone rubber feet or cork strips etc. to the bottom of the keyboard to increase friction and/or improve feel, sound and ground clearance.

### Parameter `include`

If `true`, include foot plates.

### Parameter `height`

The height in mm of each mounting plate.

### Parameter `polygons`

A list describing the horizontal shape, size and position of each mounting plate as a polygon.
