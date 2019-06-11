;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Parameter Specification – Main                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.param.tree.main
  (:require [clojure.spec.alpha :as spec]
            [clojure.java.io :refer [file]]
            [scad-tarmi.core :as tarmi-core]
            [scad-tarmi.threaded :as threaded]
            [scad-app.core :as appdata]
            [dmote-keycap.data :as capdata]
            [dactyl-keyboard.param.base :as base]
            [dactyl-keyboard.param.schema :as schema]
            [dactyl-keyboard.param.tree.cluster :as cluster]
            [dactyl-keyboard.param.tree.nested :as nested]
            [dactyl-keyboard.param.tree.restmnt :as restmnt]))


;; Though this module describes the main body of parameters, it contains
;; within it certain sections specified elsewhere. Validators for these
;; sections are created from the detailed specifications by delegation.

(spec/def ::parameters (base/delegated-validation nested/raws))
(spec/def ::individual-row (spec/keys :opt-un [::parameters]))
(spec/def ::rows (spec/map-of ::schema/flexcoord ::individual-row))
(spec/def ::individual-column (spec/keys :opt-un [::rows ::parameters]))
(spec/def ::columns (spec/map-of ::schema/flexcoord ::individual-column))
(spec/def ::overrides (spec/keys :opt-un [::columns ::parameters]))

(def raws
  "A flat version of the specification for a complete user configuration.
  This absorbs major subsections from elsewhere."
  [["# General configuration options\n\n"
    "Each heading in this document represents a recognized configuration key "
    "in the main body of a YAML file for a DMOTE variant. Other documents "
    "cover special sections of this one in more detail."]
   [:section [:keys]
    "Keys, that is keycaps and electrical switches, are not the main focus of "
    "this application, but they influence the shape of the case."]
   [:parameter [:keys :preview]
    {:default false :parse-fn boolean}
    "If `true`, include models of the keycaps in place on the keyboard. This "
    "is intended for illustration as you work on a design, not for printing."]
   [:parameter [:keys :styles]
    {:default {:default {}} :parse-fn schema/keycap-map
     :validate [(spec/map-of keyword? ::capdata/keycap-parameters)]}
    "Here you name all the types of keys on the keyboard, including their "
    "switches, keycaps, and other properties. These names are then used "
    "elsewhere, as described [here](options-nested.md).\n"
    "\n"
    "Key properties determine what kind of holes are cut out of the "
    "mounting plate, for the switches. If the keyboard is curved, these "
    "properties also help determine the spacing between key mounts. In "
    "addition, negative space is reserved for the movement of the keycap: "
    "A function of switch height, switch travel, and keycap shape.\n"
    "\n"
    "The properties correspond to the parameters of the "
    "[`dmote-keycap`](https://github.com/veikman/dmote-keycap) library "
    "and are documented in that project."]
   [:parameter [:key-clusters]
    {:heading-template "Special section `%s`"
     :default {:main {:matrix-columns [{:rows-below-home 0}]
                      :aliases {}}}
     :parse-fn (schema/map-of keyword
                 (base/parser-with-defaults cluster/raws))
     :validate [(spec/map-of
                  ::schema/key-cluster
                  (base/delegated-validation cluster/raws))]}
    "This section describes the general size, shape and position of "
    "the clusters of keys on the keyboard, each in its own subsection. "
    "It is documented in detail [here](options-clusters.md)."]
   [:section [:by-key]
    "This section repeats. Each level of settings inside it "
    "is more specific to a smaller part of the keyboard, eventually reaching "
    "the level of individual keys. It’s all documented "
    "[here](options-nested.md)."]
   [:parameter [:by-key :parameters]
    {:heading-template "Special recurring section `%s`"
     :default (base/extract-defaults nested/raws)
     :parse-fn (base/parser-with-defaults nested/raws)
     :validate [(base/delegated-validation nested/raws)]}
    "Default values at the global level."]
   [:parameter [:by-key :clusters]
    (let [rep (base/parser-wo-defaults nested/raws)]
      {:heading-template "Special section `%s` ← overrides go in here"
       :default {}
       :parse-fn (schema/map-of
                   keyword
                   (schema/map-like
                     {:parameters rep
                      :columns
                       (schema/map-of
                         schema/keyword-or-integer
                         (schema/map-like
                           {:parameters rep
                            :rows
                              (schema/map-of
                                schema/keyword-or-integer
                                (schema/map-like {:parameters rep}))}))}))
       :validate [(spec/map-of ::schema/key-cluster ::overrides)]})
    "Starting here, you gradually descend from the global level "
    "toward the key level."]
   [:parameter [:secondaries]
    {:default []
     :parse-fn schema/named-secondary-positions,
     :validate [(spec/coll-of ::schema/named-secondary-position)]}
    "A list where each item provides a name for a position in space. "
    "Such positions exist in relation to other named features of the keyboard "
    "and can themselves be used as named features: Typically as supplementary "
    "targets for `tweaks`, which are defined below.\n"
    "\n"
    "An example:\n\n"
    "```secondaries:\n"
    "  - alias: N\n"
    "    anchor: K\n"
    "    corner: NNE\n"
    "    segment: 3\n"
    "    offset: [0, 0, 10]\n```"
    "\n"
    "This example gives the name `N` to a point 10 mm above a key or "
    "some other feature named `K`, which must be defined elsewhere.\n"
    "\n"
    "A `corner` and `segment` are useful mainly with key aliases. "
    "An `offset` is applied late, i.e. in the overall coordinate system, "
    "following any transformations inherent to the anchor."]
   [:section [:case]
    "Much of the keyboard case is generated from the `wall` parameters "
    "described [here](options-nested.md). "
    "This section deals with lesser features of the case."]
   [:parameter [:case :key-mount-thickness]
    {:default 1 :parse-fn num}
    "The thickness in mm of each switch key mounting plate."]
   [:parameter [:case :key-mount-corner-margin]
    {:default 1 :parse-fn num}
    "The thickness in mm of an imaginary “post” at each corner of each key "
    "mount. Copies of such posts project from the key mounts to form the main "
    "walls of the case.\n"
    "\n"
    "`key-mount-thickness` is similarly the height of each post."]
   [:parameter [:case :web-thickness]
    {:default 1 :parse-fn num}
    "The thickness in mm of the webbing between switch key "
    "mounting plates, and of the rear housing’s walls and roof."]
   [:section [:case :rear-housing]
    "The furthest row of a key cluster can be extended into a rear housing "
    "for the MCU and various other features."]
   [:parameter [:case :rear-housing :include]
    {:default false :parse-fn boolean}
    "If `true`, add a rear housing. Please arrange case walls so as not to "
    "interfere, by removing them along the far side of the last row of key "
    "mounts in the indicated cluster."]
   [:section [:case :rear-housing :position]
    "Where to put the rear housing. By default, it sits all along the far "
    "side of the `main` cluster but has no depth."]
   [:parameter [:case :rear-housing :position :cluster]
    {:default :main :parse-fn keyword :validate [::schema/key-cluster]}
    "The key cluster at which to anchor the housing."]
   [:section [:case :rear-housing :position :offsets]
    "Modifiers for where to put the four sides of the roof. All are in mm."]
   [:parameter [:case :rear-housing :position :offsets :north]
    {:default 0 :parse-fn num}
    "The extent of the roof on the y axis; its horizontal depth."]
   [:parameter [:case :rear-housing :position :offsets :west]
    {:default 0 :parse-fn num}
    "The extent on the x axis past the first key in the row."]
   [:parameter [:case :rear-housing :position :offsets :east]
    {:default 0 :parse-fn num}
    "The extent on the x axis past the last key in the row."]
   [:parameter [:case :rear-housing :position :offsets :south]
    {:default 0 :parse-fn num}
    "The horizontal distance in mm, on the y axis, between the furthest key "
    "in the row and the roof of the rear housing."]
   [:parameter [:case :rear-housing :height]
    {:default 0 :parse-fn num}
    "The height in mm of the roof, over the floor."]
   [:section [:case :rear-housing :fasteners]
    "Threaded bolts can run through the roof of the rear housing, making it a "
    "hardpoint for attachments like a stabilizer to connect the two halves of "
    "the keyboard."]
   [:parameter [:case :rear-housing :fasteners :diameter]
    {:default 6 :parse-fn num :validate [::threaded/iso-nominal]}
    "The ISO metric diameter of each fastener."]
   [:parameter [:case :rear-housing :fasteners :bosses]
    {:default false :parse-fn boolean}
    "If `true`, add nut bosses to the ceiling of the rear housing for each "
    "fastener. Space permitting, these bosses will have some play on the "
    "north-south axis, to permit adjustment of the angle of the keyboard "
    "halves under a stabilizer."]
   [:section [:case :rear-housing :fasteners :west]
    "A fastener on the inward-facing end of the rear housing."]
   [:parameter [:case :rear-housing :fasteners :west :include]
    {:default false :parse-fn boolean}
    "If `true`, include this fastener."]
   [:parameter [:case :rear-housing :fasteners :west :offset]
    {:default 0 :parse-fn num}
    "A one-dimensional offset in mm from the inward edge of the rear "
    "housing to the fastener. You probably want a negative number if any."]
   [:section [:case :rear-housing :fasteners :east]
    "A fastener on the outward-facing end of the rear housing. All parameters "
    "are analogous to those for `west`."]
   [:parameter [:case :rear-housing :fasteners :east :include]
    {:default false :parse-fn boolean}]
   [:parameter [:case :rear-housing :fasteners :east :offset]
    {:default 0 :parse-fn num}]
   [:section [:case :back-plate]
    "Given that independent movement of each half of the keyboard is not "
    "useful, each half can include a mounting plate for a stabilizing ‘beam’. "
    "That is a straight piece of wood, aluminium, rigid plastic etc. to "
    "connect the two halves mechanically and possibly carry the wire that "
    "connects them electrically.\n\n"
    "This option is similar to rear housing, but the back plate block "
    "provides no interior space for an MCU etc. It is solid, with holes "
    "for threaded fasteners including the option of nut bosses. "
    "Its footprint is not part of a `bottom-plate`."]
   [:parameter [:case :back-plate :include]
    {:default false :parse-fn boolean}
    "If `true`, include a back plate block."]
   [:parameter [:case :back-plate :beam-height]
    {:default 1 :parse-fn num}
    "The nominal vertical extent of the back plate in mm. "
    "Because the plate is bottom-hulled to the floor, the effect "
    "of this setting is on the area of the plate above its holes."]
   [:section [:case :back-plate :fasteners]
    "Two threaded bolts run through the back plate."]
   [:parameter [:case :back-plate :fasteners :diameter]
    {:default 6 :parse-fn num :validate [::threaded/iso-nominal]}
    "The ISO metric diameter of each fastener."]
   [:parameter [:case :back-plate :fasteners :distance]
    {:default 1 :parse-fn num}
    "The horizontal distance between the fasteners."]
   [:parameter [:case :back-plate :fasteners :bosses]
    {:default false :parse-fn boolean}
    "If `true`, cut nut bosses into the inside wall of the block."]
   [:section [:case :back-plate :position]
    "The block is positioned in relation to a named feature."]
   [:parameter [:case :back-plate :position :anchor]
    {:default :origin :parse-fn keyword :validate [::schema/anchor]}
    "The name of a feature where the block will attach."]
   [:parameter [:case :back-plate :position :offset]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "An offset in mm from the named feature to the middle of the base of the "
    "back-plate block."]
   [:section [:case :bottom-plate]
    "A bottom plate can be added to close the case. This is useful mainly to "
    "simplify transportation.\n"
    "\n"
    "#### Overview\n"
    "\n"
    "The bottom plate is largely two-dimensional. The application builds most "
    "of it from a set of polygons, trying to match the perimeter of the case "
    "at the ground level (i.e. z = 0).\n"
    "\n"
    "Specifically, there is one polygon per key cluster, limited to `full` "
    "wall edges, one polygon for the rear housing, and one set of polygons "
    "for each of the first-level case `tweaks` that use `at-ground`, ignoring "
    "chunk size and almost ignoring nested tweaks.\n"
    "\n"
    "This methodology is mentioned here because its results are not perfect. "
    "Pending future features in OpenSCAD, a future version may be based on a "
    "more exact projection of the case, but as of 2018, such a projection is "
    "hollow and cannot be convex-hulled without escaping the case, unless "
    "your case is convex to start with.\n"
    "\n"
    "For this reason, while the polygons fill the interior, the perimeter of "
    "the bottom plate is extended by key walls and case `tweaks` as they "
    "would appear at the height of the bottom plate. Even this brutality may "
    "be inadequate. If you require a more exact match, do a projection of the "
    "case without a bottom plate, save it as DXF/SVG etc. and post-process "
    "that file to fill the interior gap.\n"]
   [:parameter [:case :bottom-plate :include]
    {:default false :parse-fn boolean}
    "If `true`, include a bottom plate for the case."]
   [:parameter [:case :bottom-plate :preview]
    {:default false :parse-fn boolean}
    "Preview mode. If `true`, put a model of the plate in the same file as "
    "the case it closes. Not for printing."]
   [:parameter [:case :bottom-plate :combine]
    {:default false :parse-fn boolean}
    "If `true`, combine wrist rests for the case and the bottom plate into a "
    "single model, when both are enabled."]
   [:parameter [:case :bottom-plate :thickness]
    {:default 1 :parse-fn num}
    "The thickness (i.e. height) in mm of all bottom plates you choose to "
    "include. This covers plates for the case and for the wrist rest.\n"
    "\n"
    "The case will not be raised to compensate for this. Instead, the height "
    "of the bottom plate will be removed from the bottom of the main model so "
    "that it does not extend to z = 0."]
   [:section [:case :bottom-plate :installation]
    "How your bottom plate is attached to the rest of your case."]
   [:parameter [:case :bottom-plate :installation :style]
    {:default :threads :parse-fn keyword
     :validate [::schema/plate-installation-style]}
    "The general means of installation. All currently available styles "
    "use threaded fasteners with countersunk heads. The styles differ only "
    "in how these fasteners attach to the case.\n\n"
    "One of:\n\n"
    "- `threads`: Threaded holes in the case.\n"
    "- `inserts`: Unthreaded holes for threaded heat-set inserts."]
   [:parameter [:case :bottom-plate :installation :thickness]
    {:default 1 :parse-fn num}
    "The thickness in mm of each wall of the anchor points."]
   [:section [:case :bottom-plate :installation :inserts]
    "Properties of heat-set inserts for the `inserts` style."]
   [:parameter [:case :bottom-plate :installation :inserts :length]
    {:default 1 :parse-fn num}
    "The length in mm of each insert."]
   [:section [:case :bottom-plate :installation :inserts :diameter]
    "It is assumed that, as in Tom Short’s Dactyl-ManuForm, the inserts are "
    "largely cylindrical but vary in diameter across their length."]
   [:parameter [:case :bottom-plate :installation :inserts :diameter :top]
    {:default 1 :parse-fn num}
    "Top diameter in m."]
   [:parameter [:case :bottom-plate :installation :inserts :diameter :bottom]
    {:default 1 :parse-fn num}
    "Bottom diameter in mm. This needs to be at least as large as the top "
    "diameter since the mounts for the inserts only open from the bottom."]
   [:section [:case :bottom-plate :installation :fasteners]
    "The type and positions of the threaded fasteners used to secure each "
    "bottom plate."]
   [:parameter [:case :bottom-plate :installation :fasteners :diameter]
    {:default 6 :parse-fn num :validate [::threaded/iso-nominal]}
    "The ISO metric diameter of each fastener."]
   [:parameter [:case :bottom-plate :installation :fasteners :length]
    {:default 1 :parse-fn num}
    "The length in mm of each fastener. In the `threads` style, this refers "
    "to the part of the screw that is itself threaded: It excludes the head."]
   [:parameter [:case :bottom-plate :installation :fasteners :positions]
    {:default []
     :parse-fn schema/anchored-2d-positions
     :validate [::schema/anchored-2d-list]}
    "A list of places where threaded fasteners will connect the bottom plate "
    "to the rest of the case."]
   [:section [:case :leds]
    "Support for light-emitting diodes in the case walls."]
   [:parameter [:case :leds :include]
    {:default false :parse-fn boolean}
    "If `true`, cut slots for LEDs out of the case wall, facing "
    "the space between the two halves."]
   [:section [:case :leds :position]
    "Where to attach the LED strip."]
   [:parameter [:case :leds :position :cluster]
    {:default :main :parse-fn keyword :validate [::schema/key-cluster]}
    "The key cluster at which to anchor the strip."]
   [:parameter [:case :leds :amount]
    {:default 1 :parse-fn int} "The number of LEDs."]
   [:parameter [:case :leds :housing-size]
    {:default 1 :parse-fn num}
    "The length of the side on a square profile used to create negative space "
    "for the housings on a LED strip. This assumes the housings are squarish, "
    "as on a WS2818.\n"
    "\n"
    "The negative space is not supposed to penetrate the wall, just make it "
    "easier to hold the LED strip in place with tape, and direct its light. "
    "With that in mind, feel free to exaggerate by 10%."]
   [:parameter [:case :leds :emitter-diameter]
    {:default 1 :parse-fn num}
    "The diameter of a round hole for the light of an LED."]
   [:parameter [:case :leds :interval]
    {:default 1 :parse-fn num}
    "The distance between LEDs on the strip. You may want to apply a setting "
    "slightly shorter than the real distance, since the algorithm carving the "
    "holes does not account for wall curvature."]
   [:parameter [:case :tweaks]
    {:default [] :parse-fn schema/case-tweaks :validate [::schema/hull-around]}
    "Additional shapes. This is usually needed to bridge gaps between the "
    "walls of the key clusters.\n"
    "\n"
    "The expected value here is an arbitrarily nested structure starting with "
    "a list. Each item in the list can follow one of the following patterns:\n"
    "\n"
    "- A leaf node. This is a tuple of 1 to 4 elements specified below.\n"
    "- A map, representing an instruction to combine nested items in a "
    "specific way.\n"
    "- A list of any combination of the other two types. This type exists at "
    "the top level and as the immediate child of each map node.\n"
    "\n"
    "Each leaf node identifies a particular named feature of the keyboard. "
    "It’s usually a set of corner posts on a named (aliased) key mount. "
    "These are identical to the posts used to build the walls, "
    "but this section gives you greater freedom in how to combine them. "
    "The elements of a leaf are, in order:\n"
    "\n"
    "1. Mandatory: The name of a feature, such as a key alias.\n"
    "2. Optional: A corner ID, such as `NNE` for north by north-east. "
    "If this is omitted, i.e. if only the mandatory element is given, the "
    "tweak will use the middle of the named feature.\n"
    "3. Optional: A starting wall segment ID, which is an integer from 0 to "
    "4 inclusive. If this is omitted, but a corner is named, the default "
    "value is 0.\n"
    "4. Optional: A second wall segment ID. If this is provided, the leaf "
    "will represent the convex hull of the two indicated segments plus all "
    "segments between them. If this is omitted, only one wall post will be "
    "placed.\n"
    "\n"
    "By default, a map node will create a convex hull around its child "
    "nodes. However, this behaviour can be modified. The following keys are "
    "recognized:\n"
    "\n"
    "- `at-ground`: If `true`, child nodes will be extended vertically down "
    "to the ground plane, as with a `full` wall. The default value for this "
    "key is `false`. See also: `bottom-plate`.\n"
    "- `above-ground`: If `true`, child nodes will be visible as part of the "
    "case. The default value for this key is `true`.\n"
    "- `chunk-size`: Any integer greater than 1. If this is set, child nodes "
    "will not share a single convex hull. Instead, there will be a "
    "sequence of smaller hulls, each encompassing this many items.\n"
    "- `highlight`: If `true`, render the node in OpenSCAD’s "
    "highlighting style. This is convenient while you work.\n"
    "- `hull-around`: The list of child nodes. Required.\n"
    "\n"
    "In the following example, `A` and `B` are key aliases that would be "
    "defined elsewhere. "
    "The example is interpreted to mean that a plate should be "
    "created stretching from the south-by-southeast corner of `A` to the "
    "north-by-northeast corner of `B`. Due to `chunk-size` 2, that first "
    "plate will be joined, not hulled, with a second plate from `B` back to a "
    "different corner of `A`, with a longer stretch of (all) wall "
    "segments down the corner of `A`.\n"
    "\n"
    "```case:\n"
    "  tweaks:\n"
    "    - chunk-size: 2\n"
    "      hull-around:\n"
    "      - [A, SSE]\n"
    "      - [B, NNE]\n"
    "      - [A, SSW, 0, 4]\n```"]
   [:section [:case :foot-plates]
    "Optional flat surfaces at ground level for adding silicone rubber feet "
    "or cork strips etc. to the bottom of the keyboard to increase friction "
    "and/or improve feel, sound and ground clearance."]
   [:parameter [:case :foot-plates :include]
    {:default false :parse-fn boolean} "If `true`, include foot plates."]
   [:parameter [:case :foot-plates :height]
    {:default 4 :parse-fn num} "The height in mm of each mounting plate."]
   [:parameter [:case :foot-plates :polygons]
    {:default []
     :parse-fn schema/anchored-polygons
     :validate [::schema/foot-plate-polygons]}
    "A list describing the horizontal shape, size and "
    "position of each mounting plate as a polygon."]
   [:section [:mcu]
    "This is short for ”micro-controller unit”. Each half has one."]
   [:parameter [:mcu :preview]
    {:default false :parse-fn boolean}
    "If `true`, render a visualization of the MCU for use in development."]
   [:parameter [:mcu :type]
    {:default :promicro :parse-fn keyword :validate [::schema/mcu-type]}
    "A symbolic name for a commercial product. Currently, only "
    "`promicro` is supported, referring to any MCU with the dimensions of a "
    "SparkFun Pro Micro."]
   [:parameter [:mcu :margin]
    {:default 0 :parse-fn num}
    "A general measurement in mm of extra space around each part of the MCU, "
    "including PCB and USB connector. This is applied to DMOTE components "
    "meant to hold the MCU in place, accounting for printing inaccuracy as "
    "well as inaccuracies in manufacturing the MCU."]
   [:section [:mcu :position]
    "Where to place the MCU."]
   [:parameter [:mcu :position :prefer-rear-housing]
    {:default true :parse-fn boolean}
    "If `true` and `rear-housing` is included, place the MCU in relation to "
    "the rear housing. Otherwise, place the MCU in relation to a named feature "
    "identified by `anchor`."]
   [:parameter [:mcu :position :anchor]
    {:default :origin :parse-fn keyword :validate [::schema/anchor]}
    "The name of a key at which to place the MCU if `prefer-rear-housing` "
    "is `false` or rear housing is not included."]
    ;; NOTE: The default value here, :origin, is intentionally invalid.
    ;; The origin of the coordinate system cannot be used as an anchor for an
    ;; MCU stop. A key alias is required but none are defined by default.
   [:parameter [:mcu :position :corner]
    {:default "ENE" :parse-fn schema/string-corner :validate [::schema/corner]}
    "A code for a corner of the `anchor` feature. "
    "This determines both the location and facing of the MCU."]
   [:parameter [:mcu :position :offset]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "A 3D offset in mm, measuring from the `corner`."]
   [:parameter [:mcu :position :rotation]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "A vector of 3 angles in radians. This parameter governs the rotation of "
    "the MCU around its anchor point in the front. You would not normally "
    "need this for the MCU."]
   [:section [:mcu :support]
    "The support structure that holds the MCU PCBA in place."]
   [:parameter [:mcu :support :style]
    {:default :lock :parse-fn keyword :validate [::schema/mcu-support-style]}
    "The style of the support. Available styles are:\n\n"
    "- `lock`: A separate physical object that is bolted in place over the "
    "MCU. This style is appropriate only with a rear housing, and then only "
    "when the PCB aligns with a long wall of that housing. It has the "
    "advantage that it can hug the connector on the PCB tightly, thus "
    "preventing a fragile surface-mounted connector from breaking off.\n"
    "- `stop`: A gripper that holds the MCU in place at its rear end. "
    "This gripper, in turn, is held up by key mount webbing and is thus "
    "integral to the keyboard, not printed separately like the lock. "
    "This style does not require rear housing."]
   [:parameter [:mcu :support :preview]
    {:default false :parse-fn boolean}
    "If `true`, render a visualization of the support in place. This applies "
    "only to those parts of the support that are not part of the case model."]
   [:parameter [:mcu :support :height-factor]
    {:default 1 :parse-fn num}
    "A multiplier for the width of the PCB, producing the height of the "
    "support actually touching the PCB."]
   [:parameter [:mcu :support :lateral-spacing]
    {:default 0 :parse-fn num}
    "A lateral 1D offset in mm. With rear housing, this creates space between "
    "the rear housing itself and the back of the PCB’s through-holes, so it "
    "should be roughly matched to the length of wire overshoot. Without rear "
    "housing, it isn’t so useful but it does work analogously."]
   [:section [:mcu :support :lock]
    "Parameters relevant only with a `lock`-style support."]
   [:section [:mcu :support :lock :fastener]
    "A threaded bolt connects the lock to the case."]
   [:parameter [:mcu :support :lock :fastener :style]
    {:default :countersunk :parse-fn keyword :validate [::threaded/head-type]}
    "A style of bolt head (cap) supported by `scad-tarmi`."]
   [:parameter [:mcu :support :lock :fastener :diameter]
    {:default 6 :parse-fn num :validate [::threaded/iso-nominal]}
    "The ISO metric diameter of the fastener."]
   [:section [:mcu :support :lock :socket]
    "A housing around the USB connector on the MCU."]
   [:parameter [:mcu :support :lock :socket :thickness]
    {:default 1 :parse-fn num}
    "The wall thickness of the socket."]
   [:section [:mcu :support :lock :bolt]
    "The part of a `lock`-style support that does not print with the "
    "keyboard case. This bolt, named by analogy with a lock, is not to be "
    "confused with the threaded fastener (also a bolt) holding it in place."]
   [:parameter [:mcu :support :lock :bolt :clearance]
    {:default 1 :parse-fn num}
    "The distance of the bolt from the populated side of the PCB. "
    "This distance should be slightly greater than the height of the tallest "
    "component on the PCB."]
   [:parameter [:mcu :support :lock :bolt :overshoot]
    {:default 1 :parse-fn num}
    "The distance across which the bolt will touch the PCB at the mount end. "
    "Take care that this distance is free of components on the PCB."]
   [:parameter [:mcu :support :lock :bolt :mount-length]
    {:default 1 :parse-fn num}
    "The length of the base containing a threaded channel used to secure the "
    "bolt over the MCU. This is in addition to `overshoot` and "
    "goes in the opposite direction, away from the PCB."]
   [:parameter [:mcu :support :lock :bolt :mount-thickness]
    {:default 1 :parse-fn num}
    "The thickness of the mount. This should have some rough correspondence "
    "to the threaded portion of your fastener, which should not have a shank."]
   [:section [:mcu :support :stop]
    "Parameters relevant only with a `stop`-style support."]
   [:parameter [:mcu :support :stop :anchor]
    {:default :origin :parse-fn keyword :validate [::schema/anchor]}
    "The name of a key where a stop will start to attach itself."]
    ;; NOTE: The default value here, :origin, is intentionally invalid.
    ;; A key alias is required but none are defined by default.
   [:parameter [:mcu :support :stop :direction]
    {:default :south :parse-fn keyword :validate [::schema/direction]}
    "A direction in the matrix from the named key. The stop will attach "
    "to a hull of four neighbouring key mount corners in this direction."]
   [:section [:mcu :support :stop :gripper]
    "The shape of the part that grips the PCB."]
   [:parameter [:mcu :support :stop :gripper :notch-depth]
    {:default 1 :parse-fn num}
    "The horizontal depth of the notch in the gripper that holds the PCB. "
    "The larger this number, the more flexible the case has to be to allow "
    "assembly.\n\n"
    "Note that while this is similar in effect to `lock`-style `overshoot`, "
    "it is a separate parameter because of the flexion limit."]
   [:parameter [:mcu :support :stop :gripper :total-depth]
    {:default 1 :parse-fn num}
    "The horizontal depth of the gripper as a whole in line with the PCB."]
   [:parameter [:mcu :support :stop :gripper :grip-width]
    {:default 1 :parse-fn num}
    "The width of a protrusion on each side of the notch."]
   [:section [:connection]
    "Because the DMOTE is split, there must be a signalling connection "
    "between its two halves. This section adds a socket for that purpose. "
    "For example, this might be a type 616E female for a 4P4C “RJ9” plug."]
   [:parameter [:connection :socket-size]
    {:default [1 1 1] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "The size of a hole in the case, for the female to fit into."]
   [:section [:connection :position]
    "Where to place the socket. Equivalent to `connection` → `mcu`."]
   [:parameter [:connection :position :prefer-rear-housing]
    {:default true :parse-fn boolean}]
   [:parameter [:connection :position :anchor]
    {:default :origin :parse-fn keyword :validate [::schema/anchor]}]
   [:parameter [:connection :position :corner]
    {:default "ENE" :parse-fn schema/string-corner :validate [::schema/corner]}]
   [:parameter [:connection :position :raise]
    {:default false :parse-fn boolean}
    "If `true`, and the socket is being placed in relation to the rear "
    "housing, put it directly under the ceiling, instead of directly over "
    "the floor."]
   [:parameter [:connection :position :offset]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}]
   [:parameter [:connection :position :rotation]
    {:default [0 0 0] :parse-fn vec :validate [::tarmi-core/point-3d]}]
   [:section [:wrist-rest]
    "An optional extension to support the user’s wrist."]
   [:parameter [:wrist-rest :include]
    {:default false :parse-fn boolean}
    "If `true`, include a wrist rest with the keyboard."]
   [:parameter [:wrist-rest :style]
    {:default :threaded :parse-fn keyword :validate [::schema/wrist-rest-style]}
    "The style of the wrist rest. Available styles are:\n\n"
    "- `threaded`: threaded fasteners connect the case and wrist rest.\n"
    "- `solid`: the case and wrist rest are one piece. This option is a work "
    "in progress."]
   [:parameter [:wrist-rest :preview]
    {:default false :parse-fn boolean}
    "Preview mode. If `true`, this puts a model of the wrist rest in the same "
    "OpenSCAD file as the case. That model is simplified, intended for gauging "
    "distance, not for printing."]
   [:section [:wrist-rest :position]
    "The wrist rest is positioned in relation to a named feature."]
   [:parameter [:wrist-rest :position :anchor]
    {:default :origin :parse-fn keyword :validate [::schema/anchor]}
    "The name of a feature where the wrist rest will attach. "
    "The vertical component of its position will be ignored."]
   [:parameter [:wrist-rest :position :corner]
    {:default "ENE" :parse-fn schema/string-corner :validate [::schema/corner]}
    "A corner of the feature named in `anchor`."]
   [:parameter [:wrist-rest :position :offset]
    {:default [0 0] :parse-fn vec :validate [::tarmi-core/point-2d]}
    "An offset in mm from the feature named in `anchor`."]
   [:parameter [:wrist-rest :plinth-height]
    {:default 1 :parse-fn num}
    "The average height of the plastic plinth in mm, at its upper lip."]
   [:section [:wrist-rest :shape]
    "The wrist rest needs to fit the user’s hand."]
   [:section [:wrist-rest :shape :spline]
    "The horizontal outline of the wrist rest is a closed spline."]
   [:parameter [:wrist-rest :shape :spline :main-points]
    {:default [{:position [0 0]} {:position [1 0]} {:position [1 1]}]
     :parse-fn schema/nameable-spline
     :validate [::schema/nameable-spline]}
    "A list of nameable points, in clockwise order. The spline will pass "
    "through all of these and then return to the first one. Each point can "
    "have two properties:\n\n"
    "- Mandatory: `position`. A pair of coordinates, in mm, relative to other "
    "points in the list.\n"
    "- Optional: `alias`. A name given to the specific point, for the purpose "
    "of placing yet more things in relation to it."]
   [:parameter [:wrist-rest :shape :spline :resolution]
    {:default 1 :parse-fn num}
    "The amount of vertices per main point. The default is 1. If 1, only the "
    "main points themselves will be used, giving you full control. A higher "
    "number gives you smoother curves.\n\n"
    "If you want the closing part of the curve to look smooth in high "
    "resolution, position your main points carefully.\n\n"
    "Resolution parameters, including this one, can be disabled in the main "
    "`resolution` section."]
   [:section [:wrist-rest :shape :lip]
    "The lip is the uppermost part of the plinth, lining and supporting the "
    "edge of the pad. Its dimensions are described here in mm away from the "
    "pad."]
   [:parameter [:wrist-rest :shape :lip :height]
    {:default 1 :parse-fn num} "The vertical extent of the lip."]
   [:parameter [:wrist-rest :shape :lip :width]
    {:default 1 :parse-fn num} "The horizontal width of the lip at its top."]
   [:parameter [:wrist-rest :shape :lip :inset]
    {:default 0 :parse-fn num}
    "The difference in width between the top and bottom of the lip. "
    "A small negative value will make the lip thicker at the bottom. This is "
    "recommended for fitting a silicone mould."]
   [:section [:wrist-rest :shape :pad]
    "The top of the wrist rest should be printed or cast in a soft material, "
    "such as silicone rubber."]
   [:section [:wrist-rest :shape :pad :surface]
    "The upper surface of the pad, which will be in direct contact with "
    "the user’s palm or wrist."]
   [:section [:wrist-rest :shape :pad :height]
    "The piece of rubber extends a certain distance up into the air and down "
    "into the plinth. All measurements in mm."]
   [:parameter [:wrist-rest :shape :pad :height :surface-range]
    {:default 1 :parse-fn num}
    "The vertical range of the upper surface. Whatever values are in "
    "a heightmap will be normalized to this scale."]
   [:parameter [:wrist-rest :shape :pad :height :lip-to-surface]
    {:default 1 :parse-fn num}
    "The part of the rubber pad between the top of the lip and the point "
    "where the heightmap comes into effect. This is useful if your heightmap "
    "itself has very low values at the edges, such that moulding and casting "
    "it without a base would be difficult."]
   [:parameter [:wrist-rest :shape :pad :height :below-lip]
    {:default 1 :parse-fn num}
    "The depth of the rubber wrist support, measured from the top of the lip, "
    "going down into the plinth. This part of the pad just keeps it in place."]
   [:section [:wrist-rest :shape :pad :surface :edge]
    "The edge of the pad can be rounded."]
   [:parameter [:wrist-rest :shape :pad :surface :edge :inset]
    {:default 0 :parse-fn num}
    "The horizontal extent of softening. This cannot be more than half the "
    "width of the outline, as determined by `main-points`, at its narrowest "
    "part."]
   [:parameter [:wrist-rest :shape :pad :surface :edge :resolution]
    {:default 1 :parse-fn num}
    "The number of faces on the edge between horizontal points.\n\n"
    "Resolution parameters, including this one, can be disabled in the main "
    "`resolution` section."]
   [:section [:wrist-rest :shape :pad :surface :heightmap]
    "The surface can optionally be modified by the [`surface()` function]"
    "(https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/"
    "Other_Language_Features#Surface), which requires a heightmap file."]
   [:parameter [:wrist-rest :shape :pad :surface :heightmap :include]
    {:default false :parse-fn boolean}
    "If `true`, use a heightmap. The map will intersect the basic pad "
    "polyhedron."]
   [:parameter [:wrist-rest :shape :pad :surface :heightmap :filepath]
    {:default (file ".." ".." "resources" "heightmap" "default.dat")}
    "The file identified here should contain a heightmap in a format OpenSCAD "
    "can understand. The path should also be resolvable by OpenSCAD."]
   [:section [:wrist-rest :rotation]
    "The wrist rest can be rotated to align its pad with the user’s palm."]
   [:parameter [:wrist-rest :rotation :pitch]
    {:default 0 :parse-fn num}
    "Tait-Bryan pitch."]
   [:parameter [:wrist-rest :rotation :roll]
    {:default 0 :parse-fn num}
    "Tait-Bryan roll."]
   [:parameter [:wrist-rest :mounts]
    {:heading-template "Special section `%s`"
     :default []
     :parse-fn (schema/tuple-of (base/parser-with-defaults restmnt/raws))
     :validate [(spec/coll-of (base/delegated-validation restmnt/raws))]}
    "A list of mounts for threaded fasteners. Each such mount will include at "
    "least one cuboid block for at least one screw that connects the wrist "
    "rest to the case. "
    "This section is used only with the `threaded` style of wrist rest."]
   [:section [:wrist-rest :sprues]
    "Holes in the bottom of the plinth. You pour liquid rubber through these "
    "holes when you make the rubber pad. Sprues are optional, but the general "
    "recommendation is to have two of them if you’re going to be casting your "
    "own pads. That way, air can escape even if you accidentally block one "
    "sprue with a low-viscosity silicone."]
   [:parameter [:wrist-rest :sprues :include]
    {:default false :parse-fn boolean}
    "If `true`, include sprues."]
   [:parameter [:wrist-rest :sprues :inset]
    {:default 0 :parse-fn num}
    "The horizontal distance between the perimeter of the wrist rest and the "
    "default position of each sprue."]
   [:parameter [:wrist-rest :sprues :diameter]
    {:default 1 :parse-fn num}
    "The diameter of each sprue."]
   [:parameter [:wrist-rest :sprues :positions]
    {:default [] :parse-fn schema/anchored-2d-positions
     :validate [::schema/anchored-2d-list]}
    "The positions of all sprues. This is a list where each item needs an "
    "`anchor` naming a main point in the spline. You can add an optional "
    "two-dimensional `offset`."]
   [:section [:wrist-rest :bottom-plate]
    "The equivalent of the case `bottom-plate` parameter. If included, "
    "a bottom plate for a wrist rest uses the `thickness` configured for "
    "the bottom of the case.\n"
    "\n"
    "Bottom plates for the wrist rests have no ESDS electronics to "
    "protect but serve other purposes: Covering nut pockets, silicone "
    "mould-pour cavities, and plaster or other dense material poured into "
    "plinths printed without a bottom shell."]
   [:parameter [:wrist-rest :bottom-plate :include]
    {:default false :parse-fn boolean}
    "Whether to include a bottom plate for each wrist rest."]
   [:parameter [:wrist-rest :bottom-plate :inset]
    {:default 0 :parse-fn num}
    "The horizontal distance between the perimeter of the wrist rest and the "
    "default position of each threaded fastener connecting it to its "
    "bottom plate."]
   [:parameter [:wrist-rest :bottom-plate :fastener-positions]
    {:default [] :parse-fn schema/anchored-2d-positions
     :validate [::schema/anchored-2d-list]}
    "The positions of threaded fasteners used to attach the bottom plate to "
    "its wrist rest. The syntax of this parameter is precisely the same as "
    "for the case’s bottom-plate fasteners. Corners are ignored and the "
    "starting position is inset from the perimeter of the wrist rest by the "
    "`inset` parameter above, before any offset stated here is applied.\n\n"
    "Other properties of these fasteners are determined by settings for the "
    "case."]
   [:parameter [:wrist-rest :mould-thickness]
    {:default 1 :parse-fn num}
    "The thickness in mm of the walls and floor of the mould to be used for "
    "casting the rubber pad."]
   [:section [:resolution]
    "Settings for the amount of detail on curved surfaces. More specific "
    "resolution parameters are available in other sections."]
   [:parameter [:resolution :include]
    {:default false :parse-fn boolean}
    "If `true`, apply resolution parameters found throughout the "
    "configuration. Otherwise, use defaults built into this application, "
    "its libraries and OpenSCAD. The defaults are generally conservative, "
    "providing quick renders for previews."]
   [:parameter [:resolution :minimum-face-size]
    {:default 1, :parse-fn num, :validate [::appdata/minimum-face-size]}
    "File-wide OpenSCAD minimum face size in mm."]
   [:section [:dfm]
    "Settings for design for manufacturability (DFM)."]
   [:parameter [:dfm :error-general]
    {:default 0 :parse-fn num}
    "A measurement in mm of errors introduced to negative space in the xy "
    "plane by slicer software and the printer you will use.\n"
    "\n"
    "The default value is zero. An appropriate value for a typical slicer "
    "and FDM printer with a 0.5 mm nozzle would be about -0.5 mm.\n"
    "\n"
    "This application will try to compensate for the error, though only for "
    "certain sensitive inserts, not for the case as a whole."]
   [:section [:dfm :keycaps]
    "Measurements of error, in mm, for parts of keycap models. "
    "This is separate from `error-general` because it’s especially important "
    "to have a tight fit between switch sliders and cap stems, and the "
    "size of these details is usually comparable to an FDM printer nozzle.\n"
    "\n"
    "If you will not be printing caps, ignore this section."]
   [:parameter [:dfm :keycaps :error-stem-positive]
    {:default (:error-stem-positive capdata/option-defaults) :parse-fn num}
    "Error on the positive components of stems on keycaps, such as the "
    "entire stem on an ALPS-compatible cap."]
   [:parameter [:dfm :keycaps :error-stem-negative]
    {:default (:error-stem-negative capdata/option-defaults) :parse-fn num}
    "Error on the negative components of stems on keycaps, such as the "
    "cross on an MX-compatible cap."]
   [:section [:dfm :bottom-plate]
    "DFM for bottom plates."]
   [:parameter [:dfm :bottom-plate :fastener-plate-offset]
    {:default 0 :parse-fn num}
    "A vertical offset in mm for the placement of screw holes in bottom "
    "plates. Without a slight negative offset, slicers will tend to make the "
    "holes too wide for screw heads to grip the plate securely.\n"
    "\n"
    "Notice this will not affect how screw holes are cut into the case."]
   [:section [:mask]
    "A box limits the entire shape, cutting off any projecting by-products of "
    "the algorithms. By resizing and moving this box, you can select a "
    "subsection for printing. You might want this while you are printing "
    "prototypes for a new style of switch, MCU support etc."]
   [:parameter [:mask :size]
    {:default [1000 1000 1000] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "The size of the mask in mm. By default, `[1000, 1000, 1000]`."]
   [:parameter [:mask :center]
    {:default [0 0 500] :parse-fn vec :validate [::tarmi-core/point-3d]}
    "The position of the center point of the mask. By default, `[0, 0, 500]`, "
    "which is supposed to mask out everything below ground level. If you "
    "include bottom plates, their thickness will automatically affect the "
    "placement of the mask beyond what you specify here."]])
