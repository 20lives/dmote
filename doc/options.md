# Configuration options

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant.

This documentation was generated from the application CLI.

## Section `wrist-rest`

### Parameter `include`

If `true`, include a wrist rest with the keyboard.

### Parameter `style`

The style of the wrist rest. Available styles are:

* `threaded`: threaded fastener(s) connect the case and wrist rest.
* `solid`: a printed plastic bridge along the ground as part of the model.

### Parameter `preview`

Preview mode. If `true`, this puts a model of the wrist rest in the same OpenSCAD file as the case. That model is simplified, intended for gauging distance, not for printing.

### Section `position`

#### Parameter `finger-key-column`

A finger key column ID. The wrist rest will be attached to the first key in that column.

#### Parameter `key-corner`

A corner for the first key in the column.

#### Parameter `offset`

An offset in mm from the corner of the finger key to the wrist rest.

### Parameter `plinth-base-size`

The size of the plinth up to but not including the narrowing upper lip and rubber parts.

### Parameter `lip-height`

The height of a narrowing, printed lip between the base of the plinth and the rubber part.

### Section `rubber`

#### Section `height`

##### Parameter `above-lip`

The height of the rubber wrist support, measured from the top of the lip.

##### Parameter `below-lip`

The depth of the rubber wrist support, measured from the top of the lip.

#### Section `shape`

##### Parameter `grid-size`

Undocumented.

### Section `fasteners`

#### Parameter `amount`

The number of fasteners connecting each case to its wrist rest.

#### Parameter `diameter`

The ISO metric diameter of each fastener.

#### Parameter `length`

The length in mm of each fastener.

#### Section `height`

##### Parameter `first`

The distance in mm from the bottom of the first fastener down to the ground level of the model.

##### Parameter `increment`

The vertical distance in mm from the center of each fastener to the center of the next.

#### Section `mounts`

##### Parameter `width`

The width in mm of the face or front bezel on each connecting block that will anchor a fastener.

##### Section `case-side`

###### Parameter `finger-key-column`

A finger key column ID. On the case side, fastener mounts will be attached at ground level near the first key in that column.

###### Parameter `key-corner`

A key corner to narrow down the position.

###### Parameter `offset`

An offset in mm from the corner of the finger key to the mount.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s).

##### Section `plinth-side`

###### Parameter `offset`

The offset in mm from the nearest corner of the plinth to the fastener mount attached to the plinth.

###### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s). This is typically larger than the case-side depth to allow adjustment.

### Section `solid-bridge`

#### Parameter `height`

The height in mm of the land bridge between the case and the plinth.

## Section `foot-plates`

### Parameter `include`

If `true`, include flat surfaces at ground level for adding silicone rubber feet or cork strips etc. to the bottom of the keyboard to increase friction and/or improve feel, sound and ground clearance.

### Parameter `height`

The height in mm of each mounting plate.

### Parameter `polygons`

A list describing the horizontal shape, size and position of each mounting plate as a polygon.
