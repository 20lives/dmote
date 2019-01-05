# Key cluster configuration options

Each heading in this document represents a recognized configuration key in YAML files for a DMOTE variant.

This specific document describes options for the general outline and position of any individual cluster of keys. One set of such options will exist for each entry in `key-clusters`, a parameter documented [here](options-main.md).

## Parameter `matrix-columns`

A list of key columns. Columns are aligned with the user’s fingers. Each column will be known by its index in this list, starting at zero for the first item. Each item may contain:

- `rows-above-home`: An integer specifying the amount of keys on the far side of the home row in the column. If this parameter is omitted, the effective value will be zero.
- `rows-below-home`: An integer specifying the amount of keys on the near side of the home row in the column. If this parameter is omitted, the effective value will be zero.

For example, on a normal QWERTY keyboard, H is on the home row for purposes of touch typing, and you would probably want to use it as such here too, even though the matrix in this program has no necessary relationship with touch typing, nor with the matrix in your MCU firmware (TMK/QMK etc.). Your H key will then get the coordinates [0, 0] as the home-row key in the far left column on the right-hand side of the keyboard.

In that first column, to continue the QWERTY pattern, you will want `rows-above-home` set to 1, to make a Y key, or 2 to make a 6-and-^ key, or 3 to make a function key above the 6-and-^. Your Y key will have the coordinates [0, 1]. Your 6-and-^ key will have the coordinates [0, 2], etc.

Still in that first column, to finish the QWERTY pattern, you will want `rows-below-home` set to 2, where the two keys below H are N (coordinates [0, -1]) and Space (coordinates [0, -2]).

The next item in the list will be column 1, with J as [1, 0] and so on. On the left-hand side of a DMOTE, everything is mirrored so that [0, 0] will be G instead of H in QWERTY, [1, 0] will be F instead of J, and so on.

## Parameter `style`

Cluster layout style. One of:

- `standard`: Both columns and rows have the same type of curvature applied in a logically consistent manner.
- `orthographic`: Rows are curved somewhat differently. This creates more space between columns and may prevent key mounts from fusing together if you have a broad matrix.

## Parameter `aliases`

A map of short names to specific keys by coordinate pair. Such aliases are for use elsewhere in the configuration.

## Section `position`

The position of the key cluster relative to something else.

### Parameter `anchor`

A named feature. More specifically, one of:

- `origin`: The origin of the coordinate system.
- A named key in some other cluster, as named in any of the `aliases` sections described above.

Take care not to create circular dependencies between clusters.

### Parameter `offset`

A 3-dimensional offset in mm from the indicated key or else from the origin of the coordinate system.

The z-coordinate, which is the last number in this offset, is vertical adjustment of the key cluster. Set for your main cluster, it controls the overall height of the keyboard, including the height of its case.

⸻

This document was generated from the application CLI.
