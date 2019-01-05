# Options for connecting a wrist rest

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant.

This specific document describes options for each “mount”, a pair of cuboid blocks used to anchor threaded fasteners for the `threaded` style of wrist rest, a parameter documented [here](options-main.md).

## Section `fasteners`

Threaded fasteners in the mount.

### Parameter `amount`

The number of vertically stacked screws in the mount. 1 by default.

### Parameter `diameter`

The ISO metric diameter of each fastener.

### Parameter `length`

The length in mm of each fastener.

### Section `height`

The vertical level of the fasteners.

#### Parameter `first`

The distance in mm from the bottom of the first fastener down to the ground level of the model.

#### Parameter `increment`

The vertical distance in mm from the center of each fastener to the center of the next.

## Parameter `anchoring`

One of:

- `case-side`: The `angle` parameter in this section determines the angle of the blocks and threaded fasteners in the mount. In effect, the plinth-side block is placed by `angle` and `distance`, while its own explicit `position` section of parameters is ignored.- `mutual`: The `angle` and `distance` parameters are ignored. Each block is anchored to a separate and independent feature. The angle and distance between these two features determines the angle of the fasteners and the distance between the blocks.

## Parameter `angle`

The angle in radians of the mount, on the xy plane, counter-clockwise from the y axis. This parameter is only used with `case-side` anchoring.

## Section `blocks`

Blocks for anchoring threaded fasteners.

### Parameter `distance`

The distance in mm between the two posts in a mount. This parameter is only used with `case-side` anchoring.

### Parameter `width`

The width in mm of the face or front bezel on each block that will anchor a fastener.

### Section `case-side`

A block on the side of the keyboard case is mandatory.

#### Section `position`

Where to place the block.

##### Parameter `anchor`

An alias referring to a feature that anchors the block.

##### Parameter `corner`

A corner of the anchor. By default: `SSE` for south-by-southeast.

##### Parameter `offset`

A two-dimensional vector offset in mm from the anchor to the block.

#### Parameter `depth`

The thickness of the block in mm along the axis of the fastener(s).

#### Section `nuts`

Extra features for threaded nuts on the case side.

##### Section `bosses`

Nut bosses on the rear (interior) of the mount. You may want this if the distance between case and plinth is big enough for a nut. If that distance is too small, bosses can be counterproductive.

###### Parameter `include`

If `true`, include bosses.

### Section `plinth-side`

A block on the side of the wrist rest.

#### Section `position`

Where to place the block. This entire section is ignored in the `case-side` style of anchoring.

##### Parameter `anchor`

An alias referring to a feature that anchors the block. Whereas the case-side mount is typically anchored to a key, the plinth-side mount is typically anchored to a named point on the plinth.

##### Parameter `offset`

An offset in mm from the named feature to the block.

#### Parameter `depth`

The thickness of the mount in mm along the axis of the fastener(s). This is typically larger than the case-side depth to allow adjustment.

#### Parameter `pocket-height`

The height of the nut pocket inside the mounting plate, in mm.

With a large positive value, this will provide a chute for the nut(s) to go in from the top of the plinth, which allows you to hide the hole beneath the pad. With a large negative value, the pocket will instead open from the bottom, which is convenient if `depth` is small. With a small value or the default value of zero, it will be necessary to pause printing in order to insert the nut(s); this last option is therefore recommended for advanced users only.

### Parameter `aliases`

A map of short names to specific blocks, i.e. `case-side` or `plinth-side`. Such aliases are for use elsewhere in the configuration.

⸻

This document was generated from the application CLI.
