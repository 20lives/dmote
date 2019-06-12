;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Parameter Specification – Nestables                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.param.tree.nested
  (:require [clojure.spec.alpha :as spec]
            [scad-tarmi.core :as tarmi-core]
            [dactyl-keyboard.param.schema :as schema]))

(def raws
  "A flat version of a special part of a user configuration."
  [["# Nestable configuration options\n\n"
    "This document describes all those settings which can be made at any "
    "level of specificity, from the entire keyboard down to an individual "
    "key. These settings all go under `by-key` in a YAML file.\n"
    "\n"
    "## Conceptual overview\n"
    "\n"
    "Specificity is accomplished by nesting. The following "
    "levels of specificity are currently available. Each one branches out, "
    "containing the next:\n"
    "\n"
    "- The global level, directly under `by-key` (cf. the "
    "[main document](options-main.md)).\n"
    "- The key cluster level, at `by-key` → `clusters` → your cluster.\n"
    "- The column level, nested still further under your cluster → "
    "`columns` → column index.\n"
    "- The row level, nested at the bottom, under column index → `rows` → "
    "row index.\n"
    "\n"
    "A setting at the row level will only affect keys in the specific cluster "
    "and column selected along the way, i.e. only one key per row. Therefore, "
    "the row level is effectively the key level.\n"
    "\n"
    "At each level, two subsections are permitted: `parameters`, where you "
    "put the settings themselves, and a section for the next level of "
    "nesting: `clusters`, then `columns`, then `rows`. More specific settings "
    "take precedence.\n"
    "\n"
    "In the following hypothetical example, the parameter `P`, which is "
    "not really supported, is defined three times: Once at the global level "
    "and twice at the row (key) level.\n"
    "\n"
    "```by-key:\n"
    "  parameters:\n"
    "    P: true\n"
    "  clusters:\n"
    "    C:\n"
    "      columns:\n"
    "        \"1\":\n"
    "          rows:\n"
    "            first:\n"
    "              parameters:\n"
    "                P: false\n"
    "            \"3\":\n"
    "              parameters:\n"
    "                P: false\n```"
    "\n\n"
    "In this example, `P` will have the value “true” for all keys except two "
    "on each half of the keyboard. On the right-hand side, `P` will be false "
    "for the key closest to the user (“first” row) in the second column "
    "from the left (column “1”) in a cluster of keys here named `C`. `P` will "
    "also be false for the fourth key from the user (row “3”) in the same "
    "column.\n"
    "\n"
    "Columns and rows are indexed by their ordinal integers "
    "or the words “first” or “last”, which take priority.\n"
    "\n"
    "WARNING: Due to a peculiarity of the YAML parser, take care "
    "to quote your numeric column and row indices as strings. This is why "
    "there are quotation marks around column index 1 and row index 3 in the "
    "example."]
   [:section [:layout]
    "Settings for how to place keys."]
   [:section [:layout :matrix]
    "Roughly how keys are spaced out to form a matrix."]
   [:section [:layout :matrix :neutral]
    "The neutral point in a column or row is where any progressive curvature "
    "both starts and has no effect."]
   [:parameter [:layout :matrix :neutral :column]
    {:default 0 :parse-fn int}
    "An integer column ID."]
   [:parameter [:layout :matrix :neutral :row]
    {:default 0 :parse-fn int}
    "An integer row ID."]
   [:section [:layout :matrix :separation]
    "Tweaks to control the systematic separation of keys. The parameters in "
    "this section will be multiplied by the difference between each affected "
    "key’s coordinates and the neutral column and row."]
   [:parameter [:layout :matrix :separation :column]
    {:default 0 :parse-fn num}
    "A distance in mm."]
   [:parameter [:layout :matrix :separation :row]
    {:default 0 :parse-fn num}
    "A distance in mm."]
   [:section [:layout :pitch]
    "Tait-Bryan pitch, meaning the rotation of keys around the x axis."]
   [:parameter [:layout :pitch :base]
    {:default 0 :parse-fn num}
    "An angle in radians. Set at a high level, this controls the general "
    "front-to-back incline of a key cluster."]
   [:parameter [:layout :pitch :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians. Intrinsic pitching occurs early in key placement. "
    "It is typically intended to produce a tactile break between two rows of "
    "keys, as in the typewriter-like terracing common on flat keyboards with "
    "OEM-profile or similarly angled caps.\n\n"
    "The term “intrinsic” is used here because the key spins roughly around "
    "its own center. The term should not be confused with intrinsic rotations "
    "in the sense that each step is performed on a coordinate system "
    "resulting from previous operations."]
   [:parameter [:layout :pitch :progressive]
    {:default 0 :parse-fn num}
    "An angle in radians. This progressive pitch factor bends columns "
    "lengthwise. If set to zero, columns are flat."]
   [:section [:layout :roll]
    "Tait-Bryan roll, meaning the rotation of keys around the y axis."]
   [:parameter [:layout :roll :base]
    {:default 0 :parse-fn num}
    "An angle in radians. This is the “tenting” angle. Applied to your main "
    "cluster, it controls the overall left-to-right tilt of each half of the "
    "keyboard."]
   [:parameter [:layout :roll :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians, analogous to intrinsic pitching. Where more than "
    "one column of keys is devoted to a single finger at the edge of the "
    "keyboard, this can help make the edge column easier to reach, reducing "
    "the need to bend the finger (or thumb) sideways."]
   [:parameter [:layout :roll :progressive]
    {:default 0 :parse-fn num}
    "An angle in radians. This progressive roll factor bends rows "
    "lengthwise, which also gives the columns a lateral curvature."]
   [:section [:layout :yaw]
    "Tait-Bryan yaw, meaning the rotation of keys around the z axis."]
   [:parameter [:layout :yaw :base]
    {:default 0 :parse-fn num}
    "An angle in radians. Applied to your main key cluster, this serves the "
    "purpose of allowing the user to keep their wrists straight even if the "
    "two halves of the keyboard are closer together than the user’s shoulders."]
   [:parameter [:layout :yaw :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians, analogous to intrinsic pitching."]
   [:section [:layout :translation]
    "Translation in the geometric sense, displacing keys in relation to each "
    "other. Depending on when this translation takes places, it may have a "
    "a cascading effect on other aspects of key placement. All measurements "
    "are three-dimensional vectors in mm."]
   [:parameter [:layout :translation :early]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "”Early” translation happens before other operations in key placement and "
    "therefore has the biggest knock-on effects."]
   [:parameter [:layout :translation :mid]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "This happens after columns are styled but before base pitch and roll. "
    "As such it is a good place to adjust whole columns for relative finger "
    "length."]
   [:parameter [:layout :translation :late]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "“Late” translation is the last step in key placement and therefore "
    "interacts very little with other steps."]
   [:parameter [:key-style]
    {:default :default :parse-fn keyword}
    "The name of a key style defined in the [global](options-main.md) `keys` "
    "section. The default value for this setting is the name `default`."]
   [:section [:channel]
    "Above each switch mount, there is a channel of negative space for the "
    "user’s finger and the keycap to move inside. This is only useful in those "
    "cases where nearby walls or webbing between mounts on the keyboard would "
    "otherwise obstruct movement."]
   [:parameter [:channel :height]
    {:default 1 :parse-fn num}
    "The height in mm of the negative space, starting from the "
    "bottom edge of each keycap in its pressed (active) state."]
   [:parameter [:channel :top-width]
    {:default 0 :parse-fn num}
    "The width in mm of the negative space at its top. Its width at the "
    "bottom is defined by keycap geometry."]
   [:parameter [:channel :margin]
    {:default 0 :parse-fn num}
    "The width in mm of extra negative space around the edges of a keycap, on "
    "all sides. This is applied before the `error-general` DFM compensator."]
   [:section [:wall]
    "The walls of the keyboard case support the key mounts and protect the "
    "electronics. They are generated by an algorithm that walks around each "
    "key cluster.\n"
    "\n"
    "This section determines the shape of the case wall, specifically "
    "the skirt around each key mount along the edges of the board. These skirts "
    "are made up of convex hulls wrapping sets of corner posts.\n"
    "\n"
    "There is one corner post at each actual corner of every key mount. "
    "More posts are displaced from it, going down the sides. Their placement "
    "is affected by the way the key mounts are rotated etc."]
   [:parameter [:wall :thickness]
    {:default 0 :parse-fn num}
    "A distance in mm.\n"
    "\n"
    "This is actually the distance between some pairs of corner posts "
    "(cf. `key-mount-corner-margin`), in the key mount’s frame of reference. "
    "It is therefore inaccurate as a measure of wall thickness on the x-y plane."]
   [:parameter [:wall :bevel]
    {:default 0 :parse-fn num}
    "A distance in mm.\n"
    "\n"
    "This is applied at the very top of a wall, making up the difference "
    "between wall segments 0 and 1. It is applied again at the bottom, making "
    "up the difference between segments 3 and 4."]
   [:section [:wall :north]
    "As explained [elsewhere](intro.md), “north” refers to the side facing "
    "away from the user, barring yaw.\n\n"
    "This section describes the shape of the wall on the north side of the "
    "keyboard. There are identical sections for the other cardinal directions."]
   [:parameter [:wall :north :extent]
    {:default :full :parse-fn schema/keyword-or-integer
     :validate [::schema/wall-extent]}
    "Two types of values are permitted here:\n\n"
    "- The keyword `full`. This means a complete wall extending from the key "
    "mount all the way down to the ground via segments numbered 0 through 4 "
    "and a vertical drop thereafter.\n"
    "- An integer corresponding to the last wall segment to be included. A "
    "zero means there will be no wall. No matter the number, there will be no "
    "vertical drop to the floor."]
   [:parameter [:wall :north :parallel]
    {:default 0 :parse-fn num}
    "A distance in mm. The later wall segments extend this far "
    "away from the corners of their key mount, on its plane."]
   [:parameter [:wall :north :perpendicular]
    {:default 0 :parse-fn num}
    "A distance in mm. The later wall segments extend this far away from the "
    "corners of their key mount, away from its plane."]
   [:section [:wall :east] "See `north`."]
   [:parameter [:wall :east :extent]
    {:default :full :parse-fn schema/keyword-or-integer
     :validate [::schema/wall-extent]}]
   [:parameter [:wall :east :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:wall :east :perpendicular]
    {:default 0 :parse-fn num}]
   [:section [:wall :south] "See `north`."]
   [:parameter [:wall :south :extent]
    {:default :full :parse-fn schema/keyword-or-integer
     :validate [::schema/wall-extent]}]
   [:parameter [:wall :south :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:wall :south :perpendicular]
    {:default 0 :parse-fn num}]
   [:section [:wall :west] "See `north`."]
   [:parameter [:wall :west :extent]
    {:default :full :parse-fn schema/keyword-or-integer
     :validate [::schema/wall-extent]}]
   [:parameter [:wall :west :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:wall :west :perpendicular]
    {:default 0 :parse-fn num}]])
