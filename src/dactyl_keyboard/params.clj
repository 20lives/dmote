;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameters                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This file describes the interpretation of configuration files for the
;;; application. It parses and validates serialized data.

(ns dactyl-keyboard.params
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.generics :as generics]))

(defn- coalesce
  "Assemble one branch in a tree structure from flat specifications."
  [coll [type path & metadata]]
  (case type
    :section
      (assoc-in coll path
        (ordered-map :metadata {:help (apply str metadata)}))
    :parameter
      (assoc-in coll path
        (assoc (first metadata) :help (apply str (rest metadata))))
    (throw (Exception. "Bad type in configuration master."))))

(defn inflate
  "Recursively assemble a tree from flat specifications.
  Skip the first entry (an introduction)."
  [flat]
  (reduce coalesce (ordered-map) (rest flat)))

;; Parsers:

(defn string-corner
  "For use with YAML, where string values are not automatically converted."
  [string]
  ((keyword string) generics/keyword-to-directions))

(defn tuple-of
  "A maker of parsers for vectors."
  [item-parser]
  (fn [candidate] (into [] (map item-parser candidate))))

(defn map-like
  "Return a parser of a map where the exact keys are known."
  [key-value-parsers]
  (letfn [(parse-item [[key value]]
            (if-let [value-parser (get key-value-parsers key)]
              [key (value-parser value)]
              (throw (Exception. (format "Invalid key: %s" key)))))]
    (fn [candidate] (into {} (map parse-item candidate)))))

(defn map-of [key-parser value-parser]
  "Return a parser of a map where the general type of key is known."
  (letfn [(parse-item [[key value]]
            [(key-parser key) (value-parser value)])]
    (fn [candidate] (into {} (map parse-item candidate)))))

(defn keyword-or-integer [candidate]
  "A parser that takes a number as an integer or a string as a keyword.
  This works around a peculiar facet of clj-yaml, wherein integer keys to
  maps are parsed as keywords."
  (try
    (int candidate)  ; Input like “1”.
    (catch ClassCastException _
      (try
        (Integer/parseInt (name candidate))  ; Input like “:1” (clj-yaml key).
        (catch java.lang.NumberFormatException _
          (keyword candidate))))))           ; Input like “:first” or “"first"”.

(def parse-key-clusters
  "A function to parse input for the [:key-clusters] parameter."
  (map-of
    keyword
    (map-like
      {:matrix-columns
         (tuple-of
           (map-like
             {:rows-below-home int
              :rows-above-home int}))
       :position
         (map-like
           {:key-alias keyword
            :offset (tuple-of num)})
       :style keyword
       :aliases (map-of keyword (tuple-of keyword-or-integer))})))

(def parse-by-key-overrides
  "A function to parse input for the [:by-key :clusters] section."
  (map-of
    keyword
    (map-like
      {:parameters identity
       :columns
        (map-of
          keyword-or-integer
          (map-like
            {:parameters identity
             :rows
               (map-of
                 keyword-or-integer
                 (map-like {:parameters identity}))}))})))

(defn case-tweak-corner
  "Parse notation for a range of wall segments off a specific key
  corner."
  ([alias corner s0] (case-tweak-corner alias corner s0 s0))
  ([alias corner s0 s1]
   [(keyword alias) (string-corner corner) (int s0) (int s1)]))

(defn case-tweaks [candidate]
  "Parse a tweak. This can be a lazy sequence describing a single
  corner, a lazy sequence of such sequences, or a map. If it is a
  map, it may contain a similar nested structure."
  (if (string? (first candidate))
    (apply case-tweak-corner candidate)
    (if (map? candidate)
      ((map-like {:chunk-size int
                  :to-ground boolean
                  :highlight boolean
                  :hull-around case-tweaks})
       candidate)
      (map case-tweaks candidate))))

(def key-based-polygons
  (tuple-of
    (map-like
      {:points (tuple-of
                 (map-like
                   {:key-alias keyword
                    :key-corner string-corner
                    :offset vec}))})))

;; Validators:

;; Used with spec/keys, making the names sensitive:
(spec/def ::key-alias keyword)
(spec/def ::points (spec/coll-of ::foot-plate-point))
(spec/def ::highlight boolean?)
(spec/def ::to-ground boolean?)
(spec/def ::chunk-size (spec/and int? #(> % 1)))
(spec/def ::hull-around (spec/coll-of (spec/or :leaf ::tweak-plate-leaf
                                               :map ::tweak-plate-map)))

;; Users thereof:
(spec/def ::foot-plate (spec/keys :req-un [::points]))
(spec/def ::foot-plate-point (spec/keys :req-un [::key-alias]))
(spec/def ::tweak-plate-map
  (spec/keys :req-un [::hull-around]
             :opt-un [::highlight ::chunk-size ::to-ground]))

;; Other:
(spec/def ::supported-key-cluster #(not (= :derived %)))
(spec/def ::supported-switch-style #{:alps :mx})
(spec/def ::supported-cluster-style #{:standard :orthographic})
(spec/def ::supported-cap-style #{:flat :socket :button})
(spec/def ::supported-mcu-type #{:promicro})
(spec/def ::supported-mcu-support-style #{:lock :stop})
(spec/def ::supported-wrist-rest-style #{:threaded :solid})
(spec/def ::column-disposition
  (spec/keys ::opt-un [::rows-below-home ::rows-above-home]))
(spec/def ::flexcoord (spec/or :absolute int? :extreme #{:first :last}))
(spec/def ::2d-flexcoord (spec/coll-of ::flexcoord :count 2))
(spec/def ::key-coordinates ::2d-flexcoord)  ; Exposed for unit testing.
(spec/def ::3d-point (spec/coll-of number? :count 3))
(spec/def ::corner (set (vals generics/keyword-to-directions)))
(spec/def ::direction (set (map first (vals generics/keyword-to-directions))))
(spec/def ::wall-segment (spec/int-in 0 5))
(spec/def ::wall-extent (spec/or :partial ::wall-segment :full #{:full}))
(spec/def ::tweak-plate-leaf
  (spec/or :short (spec/tuple keyword? ::corner ::wall-segment)
           :long (spec/tuple keyword? ::corner ::wall-segment ::wall-segment)))
(spec/def ::foot-plate-polygons (spec/coll-of ::foot-plate))

;; Composition of parsing and validation:

;; Leaf metadata imitates clojure.tools.cli with extras.
(spec/def ::parameter-descriptor
  #{:heading-template :help :default :parse-fn :validate :resolve-fn})
(spec/def ::parameter-spec (spec/map-of ::parameter-descriptor some?))

(defn- hard-defaults
  "Pick a user-supplied value over a default value.
  This is the default method for resolving overlap between built-in defaults
  and the user configuration, at the leaf level."
  [nominal candidate]
  (or candidate (:default nominal)))

(defn- soft-defaults [nominal candidate]
  "Prioritize a user-supplied value over a default value, but make it spongy.
  This is an alternate method for resolving overlap, intended for use with
  defaults that are so complicated the user will not want to write a complete,
  explicit replacement every time."
  (generics/soft-merge (:default nominal) candidate))

(defn parse-leaf
  "Resolve differences between default values and user-supplied values.
  Run the result through a specified parsing function and return it."
  [nominal candidate]
  (let [resolve-fn (get nominal :resolve-fn hard-defaults)
        parse-fn (get nominal :parse-fn identity)
        merged (resolve-fn nominal candidate)]
   (try
     (parse-fn merged)
     (catch Exception e
       (throw (ex-info "Could not cast value to correct data type"
                        {:type :parsing-error
                         :raw-value candidate
                         :merged-value merged
                         :original-exception e}))))))

(declare validate-leaf validate-branch)

(defn validate-node
  "Validate a fragment of a configuration received through the UI."
  [nominal candidate key]
  (assert (not (spec/valid? ::parameter-descriptor key)))
  (if (contains? nominal key)
    (if (spec/valid? ::parameter-spec (key nominal))
      (try
        (assoc candidate key (validate-leaf (key nominal) (key candidate)))
        (catch clojure.lang.ExceptionInfo e
          ;; Add the current key for richer logging at a higher level.
          ;; This would work better if the call stack were deep.
          (let [data (ex-data e)
                keys (get data :keys ())
                new-data (assoc data :keys (conj keys key))]
           (throw (ex-info (.getMessage e) new-data)))))
      (assoc candidate key (validate-branch (key nominal) (key candidate))))
    (throw (ex-info "Superfluous configuration key"
                    {:type :superfluous-key
                     :keys (list key)
                     :accepted-keys (keys nominal)}))))

(defn validate-branch
  "Validate a section of a configuration received through the UI."
  [nominal candidate]
  (reduce (partial validate-node nominal)
          candidate
          (remove #(= :metadata %)
            (distinct (apply concat (map keys [nominal candidate]))))))

(defn extract-defaults
  "Fetch default values for a broad section of the configuration."
  [flat]
  (validate-branch (inflate flat) {}))

(defn validate-leaf
  "Validate a specific parameter received through the UI."
  [nominal candidate]
  (assert (spec/valid? ::parameter-spec nominal))
  (reduce
    (fn [unvalidated validator]
      (if (spec/valid? validator unvalidated)
        unvalidated
        (throw (ex-info "Value out of range"
                        {:type :validation-error
                         :parsed-value unvalidated
                         :raw-value candidate
                         :spec-explanation (spec/explain-str validator unvalidated)}))))
    (parse-leaf nominal candidate)
    (get nominal :validate [some?])))

;; Specification:

(def cluster-raws
  "A flat version of a special part of a user configuration.
  Default values and parsers here are secondary. Validators are used."
  [["This specific document describes options for the general outline "
    "and position of any individual cluster of keys. One set of such "
    "options will exist for each entry in `key-clusters`, a parameter "
    "documented [here](options-main.md)."]
   [:parameter [:matrix-columns]
    {:default [{}]
     :parse-fn vec
     :validate [(spec/coll-of ::column-disposition)]}
    "A list of key columns. Columns are aligned with the user’s fingers. "
    "Each column will be known by its index in this list, starting at zero "
    "for the first item. Each item may contain:\n"
    "\n"
    "* `rows-above-home`: An integer specifying the amount of keys "
    "on the far side of the home row in the column. If "
    "this parameter is omitted, the effective value will be zero.\n"
    "* `rows-below-home`: An integer specifying the amount of keys "
    "on the near side of the home row in the column. If this "
    "parameter is omitted, the effective value will be zero.\n"
    "\n"
    "For example, on a normal QWERTY keyboard, H is on the home row for "
    "purposes of touch typing, and you would probably want to use it as such "
    "here too, even though the matrix in this program has no necessary "
    "relationship with touch typing, nor with the matrix in your MCU firmware "
    "(TMK/QMK etc.). Your H key will then get the coordinates [0, 0] as the "
    "home-row key in the far left column on the right-hand side of the "
    "keyboard.\n"
    "\n"
    "In that first column, to continue the QWERTY pattern, you will want "
    "`rows-above-home` set to 1, to make a Y key, or 2 to make a 6-and-^ key, "
    "or 3 to make a function key above the 6-and-^. Your Y key will have the "
    "coordinates [0, 1]. Your 6-and-^ key will have the coordinates [0, 2], etc.\n"
    "\n"
    "Still in that first column, to finish the QWERTY pattern, you will want "
    "`rows-below-home` set to 2, where the two keys below H are N "
    "(coordinates [0, -1]) and Space (coordinates [0, -2]).\n"
    "\n"
    "The next item in the list will be column 1, with J as [1, 0] and so on. "
    "On the left-hand side of a DMOTE, everything is mirrored so that [0, 0] "
    "will be G instead of H in QWERTY, [1, 0] will be F instead of J, and so on."]
   [:parameter [:style]
    {:default :standard
     :parse-fn keyword
     :validate [::supported-cluster-style]}
    "Cluster layout style. One of:\n\n"
    "* `standard`: Both columns and rows have the same type of curvature "
    "applied in a logically consistent manner.\n"
    "* `orthographic`: Rows are curved somewhat differently. This creates "
    "more space between columns and may prevent key mounts from fusing "
    "together if you have a broad matrix."]
   [:parameter [:aliases]
    {:default {:origin [0 0]}
     :parse-fn (map-of keyword (tuple-of keyword-or-integer))
     :validate [(spec/map-of keyword? ::2d-flexcoord)]}
    "A map of short names to specific keys by coordinate pair. "
    "Such aliases are for use elsewhere in the configuration."]
   [:section [:position]
    "If this section is omitted, the key clusters will be positioned at the "
    "origin of the coordinate system."]
   [:parameter [:position :key-alias]
    {:default :origin
     :parse-fn keyword
     :validate [::key-alias]}
    "A key as named under any of the `aliases` sections described above. "
    "Take care to name a key in a different cluster, and don’t create "
    "circular dependencies between clusters."]
   [:parameter [:position :offset]
    {:default [0 0 0]
     :parse-fn (tuple-of num)
     :validate [::3d-point]}
    "A 3-dimensional offset in mm from the indicated key or the origin."]])

(def nested-raws
  "A flat version of another special part of a user configuration."
  [["This document describes all those settings which can be made at "
    "any level of specificity, from the entire keyboard down to an "
    "individual key.\n"
    "\n"
    "## Conceptual overview\n"
    "\n"
    "Variable specificity is accomplished by nesting. The following "
    "levels of specificity are currently available. Each one branches out, "
    "containing the next:\n"
    "\n"
    "* The global level, at `by-key` (cf. the "
    "[main document](options-main.md)).\n"
    "* The key cluster level, at `by-key` → `clusters` → your cluster.\n"
    "* The column level, nested still further under your cluster → "
    "`columns` → column index.\n"
    "* The row level, nested under column index → `rows` → row index.\n"
    "\n"
    "Each setting takes precedence over any copies of that specific setting "
    "at less specific levels. For example, any parameter at the row level is "
    "specific not only to a row but to the full combination of cluster, "
    "column and row in the chain that leads down to the row. The same "
    "setting made at any of those higher levels will be ignored in favour "
    "of the most specific setting. Conversely, a setting made on a specific "
    "row will not affect the same row in other columns.\n"
    "\n"
    "At each level, two subsections are permitted: `parameters`, where you "
    "put the settings themselves, and a section for the next level of "
    "nesting: `clusters`, then `columns`, then `rows`. In effect, the row "
    "level is the key level and forms an exception, in that there are no "
    "further levels below it.\n"
    "\n"
    "In the following hypothetical example, the parameter `P`, which is "
    "not really supported, will have the value “true” for all keys "
    "except the one closest to the user (“first” row) in the second "
    "column from the left on the right-hand side of the keyboard "
    "(column 1; this is the second from the right on the left-hand side "
    "of the keyboard).\n"
    "\n"
    "```by-key:\n"
    "  parameters:\n"
    "    P: true\n"
    "  clusters:\n"
    "    finger:\n"
    "      columns:\n"
    "        \"1\":\n"
    "          rows:\n"
    "            first:\n"
    "              parameters:\n"
    "                P: false\n```"
    "\n\n"
    "Columns and rows are indexed by their ordinal integers "
    "or the words “first” or “last”, which take priority.\n"
    "\n"
    "WARNING: Due to a peculiarity of the YAML parser, take care "
    "to quote your numeric column and row indices as strings. This is why "
    "there are quotation marks around column index 1 in the example."]
   [:section [:parameters]
    "This section, and everything in it, can be repeated at each level "
    "of specificity."]
   [:section [:parameters :layout]
    "Settings for how to place keys."]
   [:section [:parameters :layout :matrix]
    "Roughly how keys are spaced out to form a matrix."]
   [:section [:parameters :layout :matrix :neutral]
    "The neutral point in a column or row is where any progressive curvature "
    "both starts and has no effect."]
   [:parameter [:parameters :layout :matrix :neutral :column]
    {:default 0 :parse-fn int}
    "An integer column ID."]
   [:parameter [:parameters :layout :matrix :neutral :row]
    {:default 0 :parse-fn int}
    "An integer row ID."]
   [:section [:parameters :layout :matrix :separation]
    "Tweaks to control the systematic separation of keys. The parameters in "
    "this section will be multiplied by the difference between each affected "
    "key’s coordinates and the neutral column and row."]
   [:parameter [:parameters :layout :matrix :separation :column]
    {:default 0 :parse-fn num}
    "A distance in mm."]
   [:parameter [:parameters :layout :matrix :separation :row]
    {:default 0 :parse-fn num}
    "A distance in mm."]
   [:section [:parameters :layout :pitch]
    "Tait-Bryan pitch, meaning the rotation of keys around the x axis."]
   [:parameter [:parameters :layout :pitch :base]
    {:default 0 :parse-fn num}
    "An angle in radians. Set at a high level, this controls the general "
    "front-to-back incline of a key cluster."]
   [:parameter [:parameters :layout :pitch :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians. Intrinsic pitching occurs early in key placement. "
    "It is typically intended to produce a tactile break between two rows of "
    "keys, as in the typewriter-like terracing common on flat keyboards with "
    "OEM-profile or similarly angled caps.\n\n"
    "The term “intrinsic” is used here because the key spins roughly around "
    "its own center. The term should not be confused with intrinsic rotations "
    "in the sense that each step is performed on a coordinate system "
    "resulting from previous operations."]
   [:parameter [:parameters :layout :pitch :progressive]
    {:default 0 :parse-fn num}
    "An angle in radians. This progressive pitch factor bends columns "
    "lengthwise. If set to zero, columns are flat."]
   [:section [:parameters :layout :roll]
    "Tait-Bryan roll, meaning the rotation of keys around the y axis."]
   [:parameter [:parameters :layout :roll :base]
    {:default 0 :parse-fn num}
    "An angle in radians. This is the “tenting” angle. Applied to the finger "
    "cluster, it controls the overall left-to-right tilt of each half of the "
    "keyboard."]
   [:parameter [:parameters :layout :roll :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians, analogous to intrinsic pitching. Where more than "
    "one column of keys is devoted to a single finger at the edge of the "
    "keyboard, this can help make the edge column easier to reach, reducing "
    "the need to bend the finger (or thumb) sideways."]
   [:parameter [:parameters :layout :roll :progressive]
    {:default 0 :parse-fn num}
    "An angle in radians. This progressive roll factor bends rows "
    "lengthwise, which also gives the columns a lateral curvature."]
   [:section [:parameters :layout :yaw]
    "Tait-Bryan yaw, meaning the rotation of keys around the z axis."]
   [:parameter [:parameters :layout :yaw :base]
    {:default 0 :parse-fn num}
    "An angle in radians. Applied to the finger key cluster, this serves the "
    "purpose of allowing the user to keep their wrists straight even if the "
    "two halves of the keyboard are closer together than the user’s shoulders."]
   [:parameter [:parameters :layout :yaw :intrinsic]
    {:default 0 :parse-fn num}
    "An angle in radians, analogous to intrinsic pitching."]
   [:section [:parameters :layout :translation]
    "Translation in the geometric sense, displacing keys in relation to each "
    "other. Depending on when this translation takes places, it may have a "
    "a cascading effect on other aspects of key placement. All measurements "
    "are three-dimensional vectors in mm."]
   [:parameter [:parameters :layout :translation :early]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}
    "”Early” translation happens before other operations in key placement and "
    "therefore has the biggest knock-on effects."]
   [:parameter [:parameters :layout :translation :mid]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}
    "This happens after columns are styled but before base pitch and roll. "
    "As such it is a good place to adjust whole columns for relative finger "
    "length."]
   [:parameter [:parameters :layout :translation :late]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}
    "“Late” translation is the last step in key placement and therefore "
    "interacts very little with other steps. As a result, the z-coordinate, "
    "which is the last number in this vector, serves as a general vertical "
    "offset of the finger key cluster from the ground plane. If set at a "
    "high level, this controls the overall height of the keyboard, "
    "including the height of the case walls."]
   [:section [:parameters :channel]
    "Above each switch mount, there is a channel of negative space for the "
    "user’s finger and the keycap to move inside. This is only useful in those "
    "cases where nearby walls or webbing between mounts on the keyboard would "
    "otherwise obstruct movement."]
   [:parameter [:parameters :channel :height]
    {:default 1 :parse-fn num}
    "The height in mm of the negative space, starting from the "
    "bottom edge of each keycap in its pressed (active) state."]
   [:parameter [:parameters :channel :top-width]
    {:default 0 :parse-fn num}
    "The width in mm of the negative space at its top. Its width at the "
    "bottom is defined by keycap geometry."]
   [:parameter [:parameters :channel :margin]
    {:default 0 :parse-fn num}
    "The width in mm of extra negative space around the edges of a keycap, on "
    "all sides."]
   [:section [:parameters :wall]
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
   [:parameter [:parameters :wall :thickness]
    {:default 0 :parse-fn num}
    "A distance in mm.\n"
    "\n"
    "This is actually the distance between some pairs of corner posts "
    "(cf. `key-mount-corner-margin`), in the key mount’s frame of reference. "
    "It is therefore inaccurate as a measure of wall thickness on the x-y plane."]
   [:parameter [:parameters :wall :bevel]
    {:default 0 :parse-fn num}
    "A distance in mm.\n"
    "\n"
    "This is applied at the very top of a wall, making up the difference "
    "between wall segments 0 and 1. It is applied again at the bottom, making "
    "up the difference between segments 3 and 4."]
   [:section [:parameters :wall :north]
    "As explained [elsewhere](intro.md), “north” refers to the side facing "
    "away from the user, barring yaw.\n\n"
    "This section describes the shape of the wall on the north side of the "
    "keyboard. There are identical sections for the other cardinal directions."]
   [:parameter [:parameters :wall :north :extent]
    {:default :full :parse-fn keyword-or-integer :validate [::wall-extent]}
    "Two types of values are permitted here:\n\n"
    "* The keyword `full`. This means a complete wall extending from the key "
    "mount all the way down to the ground via segments numbered 0 through 4 "
    "and a vertical drop thereafter.\n"
    "* An integer corresponding to the last wall segment to be included. A "
    "zero means there will be no wall. No matter the number, there will be no "
    "vertical drop to the floor."]
   [:parameter [:parameters :wall :north :parallel]
    {:default 0 :parse-fn num}
    "A distance in mm. The later wall segments extend this far "
    "away from the corners of their key mount, on its plane."]
   [:parameter [:parameters :wall :north :perpendicular]
    {:default 0 :parse-fn num}
    "A distance in mm. The later wall segments extend this far away from the "
    "corners of their key mount, away from its plane."]
   [:section [:parameters :wall :east] "See `north`."]
   [:parameter [:parameters :wall :east :extent]
    {:default :full :parse-fn keyword-or-integer :validate [::wall-extent]}]
   [:parameter [:parameters :wall :east :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:parameters :wall :east :perpendicular]
    {:default 0 :parse-fn num}]
   [:section [:parameters :wall :south] "See `north`."]
   [:parameter [:parameters :wall :south :extent]
    {:default :full :parse-fn keyword-or-integer :validate [::wall-extent]}]
   [:parameter [:parameters :wall :south :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:parameters :wall :south :perpendicular]
    {:default 0 :parse-fn num}]
   [:section [:parameters :wall :west] "See `north`."]
   [:parameter [:parameters :wall :west :extent]
    {:default :full :parse-fn keyword-or-integer :validate [::wall-extent]}]
   [:parameter [:parameters :wall :west :parallel]
    {:default 0 :parse-fn num}]
   [:parameter [:parameters :wall :west :perpendicular]
    {:default 0 :parse-fn num}]
   [:parameter [:clusters]
    {:heading-template "Parameter `%s` ← overrides go in here"
     :default {}
     :parse-fn parse-by-key-overrides
     :validate [(spec/map-of ::supported-key-cluster
                             ::individual-cluster-overrides)]}
    "Starting here, you gradually descend from the global level "
    "toward the key level."]])

;; A predicate made from nested-raws is applied for validation below.
;; It is defined here because it uses nested-raws.
(spec/def ::parameters
  #(some? (validate-branch (:parameters (inflate nested-raws)) %)))
(spec/def ::individual-row (spec/keys :opt-un [::parameters]))
(spec/def ::rows (spec/map-of ::flexcoord ::individual-row))
(spec/def ::individual-column (spec/keys :opt-un [::rows ::parameters]))
(spec/def ::columns (spec/map-of ::flexcoord ::individual-column))
(spec/def ::individual-cluster-overrides
  (spec/keys :opt-un [::columns ::parameters]))
(spec/def ::clusters ::individual-cluster-overrides)

(def main-raws
  "A flat version of the specification for a user configuration.
  This excludes some major subsections."
  [["This is the main body of available options. As such, it starts "
    "from the top level of a YAML file. Other documents cover sections "
    "of this one in more detail."]
   [:section [:keycaps]
    "Keycaps are the plastic covers placed over the switches. Their shape will "
    "help determine the spacing between key mounts if the keyboard is curved. "
    "Negative space is also reserved for the caps."]
   [:parameter [:keycaps :preview]
    {:default false :parse-fn boolean}
    "If `true`, include models of the keycaps. This is intended for "
    "illustration in development. The models are not good enough for "
    "printing."]
   [:parameter [:keycaps :body-height]
    {:default 1 :parse-fn num}
    "The height in mm of each keycap, measured from top to bottom of the "
    "entire cap by itself.\n\n"
    "An SA cap would be about 11.6 mm, DSA 7.3 mm."]
   [:parameter [:keycaps :resting-clearance]
    {:default 1 :parse-fn num}
    "The height in mm of the air gap between keycap and switch mount, "
    "in a resting state."]
   [:section [:switches]
    "Electrical switches close a circuit when pressed. They cannot be "
    "printed. This section specifies how much space they need to be "
    "mounted."]
   [:parameter [:switches :style]
    {:default :alps
     :parse-fn keyword
     :validate [::supported-switch-style]}
    "The switch type. One of:\n\n "
    "* `alps`: ALPS style switches, including Matias.\n"
    "* `mx`: Cherry MX style switches."]
   [:parameter [:switches :travel]
    {:default 1 :parse-fn num}
    "The distance in mm that a keycap can travel vertically when "
    "mounted on a switch."]
   [:parameter [:key-clusters]
    {:heading-template "Special section `%s`"
     :default {:finger {:matrix-columns [{:rows-below-home 0}]
                        :aliases {:origin [0 0]}}}
     :parse-fn parse-key-clusters
     :validate [(spec/map-of
                  ::supported-key-cluster
                  #(some? (validate-branch (inflate cluster-raws) %)))]}
    "This section describes the general size, shape and position of "
    "the clusters of keys on the keyboard, each in its own subsection. "
    "It is documented in detail [here](options-clusters.md)."]
   [:parameter [:by-key]
    {:heading-template "Special nesting section `%s`"
     :default (extract-defaults nested-raws)
     :parse-fn (map-like {:parameters identity
                          :clusters parse-by-key-overrides})
     :resolve-fn soft-defaults
     :validate [(spec/keys :opt-un [::parameters ::clusters])]}
    "This section is built like an onion. Each layer of settings inside it "
    "is more specific to a smaller part of the keyboard, eventually reaching "
    "the level of individual keys. It’s all documented "
    "[here](options-nested.md)."]
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
    "mounting plates and of the rear housing’s walls and roof."]
   [:section [:case :rear-housing]
    "The furthest row of the key cluster can be extended into a rear housing "
    "for the MCU and various other features."]
   [:parameter [:case :rear-housing :include]
    {:default false :parse-fn boolean}
    "If `true`, add a rear housing. Please arrange case walls so as not to "
    "interfere, by removing them along the far side of the last row of key "
    "mounts in the finger cluster."]
   [:parameter [:case :rear-housing :west-foot]
    {:default false :parse-fn boolean}
    "If `true`, add a foot plate at ground level by the far inward corner "
    "of the rear housing. The height of the plate is controlled by the "
    "`foot-plates` section below."]
   [:parameter [:case :rear-housing :distance]
    {:default 0 :parse-fn num}
    "The horizontal distance in mm between the furthest key in the row and "
    "the roof of the rear housing."]
   [:parameter [:case :rear-housing :height]
    {:default 0 :parse-fn num}
    "The height in mm of the roof, over the floor."]
   [:section [:case :rear-housing :offsets]
    "Modifiers for the size of the roof. All are in mm."]
   [:parameter [:case :rear-housing :offsets :north]
    {:default 0 :parse-fn num}
    "The extent of the roof on the y axis; its horizontal depth."]
   [:parameter [:case :rear-housing :offsets :west]
    {:default 0 :parse-fn num}
    "The extent on the x axis past the first key in the row."]
   [:parameter [:case :rear-housing :offsets :east]
    {:default 0 :parse-fn num}
    "The extent on the x axis past the last key in the row."]
   [:section [:case :rear-housing :fasteners]
    "Threaded bolts can run through the roof of the rear housing, making it a "
    "hardpoint for attachments like a stabilizer to connect the two halves of "
    "the keyboard."]
   [:parameter [:case :rear-housing :fasteners :diameter]
    {:default 1 :parse-fn int}
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
    "for threaded fasteners including the option of nut bosses."]
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
    {:default 1 :parse-fn int}
    "The ISO metric diameter of each fastener."]
   [:parameter [:case :back-plate :fasteners :distance]
    {:default 1 :parse-fn num}
    "The horizontal distance between the fasteners."]
   [:parameter [:case :back-plate :fasteners :bosses]
    {:default false :parse-fn boolean}
    "If `true`, cut nut bosses into the inside wall of the block."]
   [:section [:case :back-plate :position]
    "The block is positioned in relation to a key mount."]
   [:parameter [:case :back-plate :position :key-alias]
    {:default :origin :parse-fn keyword}
    "A named key where the block will attach. The vertical component of its "
    "position will be ignored."]
   [:parameter [:case :back-plate :position :offset]
    {:default [0 0 0] :parse-fn vec}
    "An offset in mm from the middle of the north wall of the selected key, "
    "at ground level, to the middle of the base of the back plate block."]
   [:section [:case :leds]
    "Support for light-emitting diodes in the case walls."]
   [:parameter [:case :leds :include]
    {:default false :parse-fn boolean}
    "If `true`, cut slots for LEDs out of the case wall, facing "
    "the space between the two halves."]
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
    {:default [] :parse-fn case-tweaks :validate [::hull-around]}
    "Additional shapes. This is usually needed to bridge gaps between the "
    "walls of the key clusters.\n"
    "\n"
    "The expected value here is an arbitrarily nested structure starting with "
    "a list. Each item in the list can follow one of the following patterns:\n"
    "\n"
    "* A leaf node. This is a 3- or 4-tuple list with contents specified below.\n"
    "* A map, representing an instruction to combine nested items in a specific way.\n"
    "* A list of any combination of the other two types. This type exists at "
    "the top level and as the immediate child of each map node.\n"
    "\n"
    "Each leaf node identifies a particular set of key mount corner posts. "
    "These are identical to the posts used to build the walls, "
    "but this section gives you greater freedom in how to combine them. "
    "A leaf node must contain:\n"
    "\n"
    "* A key alias defined under `key-clusters`.\n"
    "* A key corner ID, such as `NNE` for north by north-east.\n"
    "* A wall segment ID, which is an integer from 0 to 4.\n"
    "\n"
    "Together, these identify a starting segment. Optionally, a leaf node may "
    "contain a second segment ID trailing the first. In that case, the leaf "
    "will represent the convex hull of the first and second indicated "
    "segments, plus all in between.\n"
    "\n"
    "By default, a map node will create a convex hull around its child "
    "nodes. However, this behaviour can be modified. The following keys are "
    "recognized:\n"
    "\n"
    "* `to-ground`: If `true`, child nodes will be extended vertically down "
    "to the ground plane, as with a `full` wall.\n"
    "* `chunk-size`: Any integer greater than 1. If this is set, child nodes "
    "will not share a single convex hull. Instead, there will be a "
    "sequence of smaller hulls, each encompassing this many items.\n"
    "* `highlight`: If `true`, render the node in OpenSCAD’s "
    "highlighting style. This is convenient while you work.\n"
    "* `hull-around`: The list of child nodes. Required.\n"
    "\n"
    "In the following example, `A` and `B` are aliases that would be defined "
    "elsewhere. The example is interpreted to mean that a plate should be "
    "created stretching from the south-by-southeast corner of `A` to the "
    "north-by-northeast corner of `B`. Due to `chunk-size` 2, that first "
    "plate will be joined, not hulled, with a second plate from `B` back to a "
    "different corner of `A`, with a longer stretch of wall "
    "segments down the corner of `A`.\n"
    "\n"
    "```case:\n"
    "  tweaks:\n"
    "    - chunk-size: 2\n"
    "      hull-around:\n"
    "      - [A, SSE, 0]\n"
    "      - [B, NNE, 0]\n"
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
     :parse-fn key-based-polygons
     :validate [::foot-plate-polygons]}
    "A list describing the horizontal shape, size and "
    "position of each mounting plate as a polygon."]
   [:section [:mcu]
    "This is short for ”micro-controller unit”. Each half has one."]
   [:parameter [:mcu :preview]
    {:default false :parse-fn boolean}
    "If `true`, render a visualization of the MCU for use in development."]
   [:parameter [:mcu :type]
    {:default :promicro :parse-fn keyword :validate [::supported-mcu-type]}
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
    "the rear housing. Otherwise, place the MCU in relation to a key mount "
    "identified by `key-alias`."]
   [:parameter [:mcu :position :key-alias]
    {:default :origin :parse-fn keyword}
    "The name of a key at which to place the MCU if `prefer-rear-housing` "
    "is `false` or rear housing is not included."]
   [:parameter [:mcu :position :corner]
    {:default "ENE" :parse-fn string-corner :validate [::corner]}
    "A code for a corner of the rear housing or of `key-alias`. "
    "This determines both the location and facing of the MCU."]
   [:parameter [:mcu :position :offset]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}
    "A 3D offset in mm, measuring from the `corner`."]
   [:parameter [:mcu :position :rotation]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}
    "A vector of 3 angles in radians. This parameter governs the rotation of "
    "the MCU around its anchor point in the front. You would not normally "
    "need this for the MCU."]
   [:section [:mcu :support]
    "The support structure that holds the MCU PCBA in place."]
   [:parameter [:mcu :support :style]
    {:default :lock :parse-fn keyword :validate [::supported-mcu-support-style]}
    "The style of the support. Available styles are:\n\n"
    "* `lock`: A separate physical object that is bolted in place over the "
    "MCU. This style is appropriate only with a rear housing, and then only "
    "when the PCB aligns with a long wall of that housing. It has the "
    "advantage that it can hug the connector on the PCB tightly, thus "
    "preventing a fragile surface-mounted connector from breaking off.\n"
    "* `stop`: A gripper that holds the MCU in place at its rear end. "
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
    "Threaded fasteners—a nut and a bolt—connect the lock to the case."]
   [:parameter [:mcu :support :lock :fastener :style]
    {:default :button :parse-fn keyword :validate [::supported-cap-style]}
    "A supported bolt cap style."]
   [:parameter [:mcu :support :lock :fastener :diameter]
    {:default 1 :parse-fn num} "The ISO metric diameter of the fastener."]
   [:section [:mcu :support :lock :socket]
    "A housing around the USB connector on the MCU."]
   [:parameter [:mcu :support :lock :socket :thickness]
    {:default 1 :parse-fn num}
    "The wall thickness of the socket."]
   [:section [:mcu :support :lock :bolt]
    "The part of a `lock`-style support that does not print as part of the "
    "keyboard case. This bolt, named by analogy with a lock, is not to be "
    "confused with the threaded fasteners holding it in place."]
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
    "The length of the base that contains the threaded fasteners used to "
    "secure the bolt over the MCU. This is in addition to `overshoot` and "
    "goes in the opposite direction, away from the PCB."]
   [:parameter [:mcu :support :lock :bolt :mount-thickness]
    {:default 1 :parse-fn num}
    "The thickness of the mount. You will need a threaded fastener slightly "
    "longer than this."]
   [:section [:mcu :support :stop]
    "Parameters relevant only with a `stop`-style support."]
   [:parameter [:mcu :support :stop :key-alias]
    {:default :origin :parse-fn keyword}
    "The name of a key where a stop will start to attach itself."]
   [:parameter [:mcu :support :stop :direction]
    {:default :south :parse-fn keyword :validate [::direction]}
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
    {:default [1 1 1] :parse-fn vec :validate [::3d-point]}
    "The size of a hole in the case, for the female to fit into."]
   [:section [:connection :position]
    "Where to place the socket. Equivalent to `connection` → `mcu`."]
   [:parameter [:connection :position :prefer-rear-housing]
    {:default true :parse-fn boolean}]
   [:parameter [:connection :position :key-alias]
    {:default :origin :parse-fn keyword}]
   [:parameter [:connection :position :corner]
    {:default "ENE" :parse-fn string-corner :validate [::corner]}]
   [:parameter [:connection :position :raise]
    {:default false :parse-fn boolean}
    "If `true`, and the socket is being placed in relation to the rear "
    "housing, put it directly under the ceiling, instead of directly over "
    "the floor."]
   [:parameter [:connection :position :offset]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}]
   [:parameter [:connection :position :rotation]
    {:default [0 0 0] :parse-fn vec :validate [::3d-point]}]
   [:section [:wrist-rest]
    "An optional extension to support the user’s wrist."]
   [:parameter [:wrist-rest :include]
    {:default false :parse-fn boolean}
    "If `true`, include a wrist rest with the keyboard."]
   [:parameter [:wrist-rest :style]
    {:default :threaded
     :parse-fn keyword
     :validate [::supported-wrist-rest-style]}
    "The style of the wrist rest. Available styles are:\n\n"
    "* `threaded`: threaded fasteners connect the case and wrist rest. "
    "This works with a great variety of keyboard shapes and will allow "
    "adjusting the position of the wrist rest for different hands.\n"
    "* `solid`: a printed plastic bridge along the ground as part of the "
    "model. This has more limitations, both in manufacture and in use. "
    "It includes a hook on the near outward side of the case, which will only "
    "be useful if the case wall at that point is short and finger column 2 "
    "is positioned and walled in such a way that the solid bridge can be "
    "wedged between the hook and the column."]
   [:parameter [:wrist-rest :preview]
    {:default false :parse-fn boolean}
    "Preview mode. If `true`, this puts a model of the wrist rest in the same "
    "OpenSCAD file as the case. That model is simplified, intended for gauging "
    "distance, not for printing."]
   [:section [:wrist-rest :position]
    "The wrist rest is positioned in relation to a key mount."]
   [:parameter [:wrist-rest :position :key-alias]
    {:default :origin :parse-fn keyword}
    "A named key where the wrist rest will attach. "
    "The vertical component of its position will be ignored."]
   [:parameter [:wrist-rest :position :offset]
    {:default [0 0] :parse-fn vec}
    "An offset in mm from the selected key to one corner of the base of the "
    "wrist rest. Specifically, it is the corner close to the keyboard case, "
    "on the right-hand side of the right-hand half."]
   [:section [:wrist-rest :shape]
    "The wrist rest needs to fit the user’s hand."]
   [:parameter [:wrist-rest :shape :plinth-base-size]
    {:default [1 1 1] :parse-fn vec :validate [::3d-point]}
    "The size of the plinth up to but not including the narrowing upper lip "
    "and rubber parts."]
   [:parameter [:wrist-rest :shape :chamfer]
    {:default 1 :parse-fn num}
    "A distance in mm. The plinth is shrunk and then regrown by this much to "
    "chamfer its corners."]
   [:parameter [:wrist-rest :shape :lip-height]
    {:default 1 :parse-fn num}
    "The height of a narrowing, printed lip between the base of the plinth "
    "and the rubber part."]
   [:section [:wrist-rest :shape :pad]
    "The top of the wrist rest should be printed or cast in a soft material, "
    "such as silicone rubber."]
   [:parameter [:wrist-rest :shape :pad :surface-heightmap]
    {:default "resources/heightmap/default.dat"}
    "A filepath. The path, and file, will be interpreted by OpenScad, using "
    "its [`surface()` function](https://en.wikibooks.org/wiki/"
    "OpenSCAD_User_Manual/Other_Language_Features#Surface).\n\n"
    "The file should contain a heightmap to describe the surface of the "
    "rubber pad."]
   [:section [:wrist-rest :shape :pad :height]
    "The piece of rubber extends a certain distance up into the air and down "
    "into the plinth. All measurements in mm."]
   [:parameter [:wrist-rest :shape :pad :height :surface-range]
    {:default 1 :parse-fn num}
    "The vertical range of the heightmap. Whatever values are in "
    "the heightmap will be normalized to this scale."]
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
   [:section [:wrist-rest :fasteners]
    "This is only relevant with the `threaded` style of wrist rest."]
   [:parameter [:wrist-rest :fasteners :amount]
    {:default 1 :parse-fn int}
    "The number of fasteners connecting each case to its wrist rest."]
   [:parameter [:wrist-rest :fasteners :diameter]
    {:default 1 :parse-fn int} "The ISO metric diameter of each fastener."]
   [:parameter [:wrist-rest :fasteners :length]
    {:default 1 :parse-fn int} "The length in mm of each fastener."]
   [:section [:wrist-rest :fasteners :height]
    "The vertical level of the fasteners."]
   [:parameter [:wrist-rest :fasteners :height :first]
    {:default 0 :parse-fn int}
    "The distance in mm from the bottom of the first fastener "
    "down to the ground level of the model."]
   [:parameter [:wrist-rest :fasteners :height :increment]
    {:default 0 :parse-fn num}
    "The vertical distance in mm from the center of each fastener to the "
    "center of the next."]
   [:section [:wrist-rest :fasteners :mounts]
    "The mounts, or anchor points, for each fastener on each side."]
   [:parameter [:wrist-rest :fasteners :mounts :width]
    {:default 1 :parse-fn num}
    "The width in mm of the face or front bezel on each "
    "connecting block that will anchor a fastener."]
   [:section [:wrist-rest :fasteners :mounts :case-side]
    "The side of the keyboard case."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :key-alias]
    {:default :origin :parse-fn keyword}
    "A named key. A mount point on the case side will be placed near this key."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :offset]
    {:default [0 0] :parse-fn vec}
    "An two-dimensional vector offset in mm from the key to the mount."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :depth]
    {:default 1 :parse-fn num}
    "The thickness of the mount in mm along the axis of the fastener(s)."]
   [:section [:wrist-rest :fasteners :mounts :case-side :nuts]
    "Extra features for threaded nuts on the case side."]
   [:section [:wrist-rest :fasteners :mounts :case-side :nuts :bosses]
    "Nut bosses on the rear (interior) of the mount. You may want this if the "
    "distance between case and plinth is big enough for a nut. If that "
    "distance is too small, bosses can be counterproductive."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :nuts :bosses :include]
    {:default false :parse-fn boolean}
    "If `true`, include bosses."]
   [:section [:wrist-rest :fasteners :mounts :plinth-side]
    "The side of the wrist rest."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :offset]
    {:default [0 0] :parse-fn vec}
    "The offset in mm from the corner of the plinth to the fastener mount "
    "point attached to the plinth."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :depth]
    {:default 1 :parse-fn num}
    "The thickness of the mount in mm along the axis of the fastener(s). "
    "This is typically larger than the case-side depth to allow adjustment."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :pocket-scale]
    {:default 1 :parse-fn num}
    "A scale coefficient for the nut model used to carve out the pocket. "
    "This defaults to 1. You would need to set it higher if your printing "
    "material, in combination with your nozzle diameter, shrinks or expands "
    "in such a way that the deep pocket would otherwise be too tight."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :pocket-height]
    {:default 0 :parse-fn num}
    "The height of the nut pocket inside the mounting plate, in mm.\n\n"
    "With a large positive value, this will provide a chute for the nut(s) "
    "to go in from the top of the plinth, which allows you to hide the hole "
    "beneath the pad. With a large negative value, the pocket will "
    "instead open from the bottom, which is convenient if `depth` is small. "
    "With a small value or the default value of zero, it will be necessary to "
    "pause printing in order to insert the nut(s); this last option is "
    "therefore recommended for advanced users only."]
   [:section [:wrist-rest :solid-bridge]
    "This is only relevant with the `solid` style of wrist rest."]
   [:parameter [:wrist-rest :solid-bridge :width]
    {:default 1 :parse-fn num}
    "The width in mm of the land bridge between the case and the plinth.\n"
    "\n"
    "On the right-hand side of the keyboard, the bridge starts from the wrist "
    "rest `key-alias` and extends this many mm to the left.\n"
    "\n"
    "The value of this parameter, and the shape of the keyboard case, should "
    "be arranged in a such a way that the land bridge is wedged in place by a "
    "vertical wall on that left side."]
   [:parameter [:wrist-rest :solid-bridge :height]
    {:default 1 :parse-fn num}
    "The height in mm of the land bridge between the case and the plinth."]
   [:section [:mask]
    "A box limits the entire shape, cutting off any projecting byproducts of "
    "the algorithms. By resizing and moving this box, you can select a "
    "subsection for printing. You might want this while you are printing "
    "prototypes for a new style of switch, MCU support etc."]
   [:parameter [:mask :size]
    {:default [1000 1000 1000] :parse-fn vec :validate [::3d-point]}
    "The size of the mask in mm. By default, `[1000, 1000, 1000]`."]
   [:parameter [:mask :center]
    {:default [0 0 500] :parse-fn vec :validate [::3d-point]}
    "The position of the center point of the mask. By default, `[0, 0, 500]`, "
    "which is supposed to mask out everything below ground level."]])

(defn- print-markdown-fragment
  "Print a description of a node in the settings structure using Markdown."
  [node level]
  (let [heading-style
          (fn [text]
            (if (< level 7)  ; Markdown only supports up to h6 tags.
              (str (string/join "" (repeat level "#")) " " text)
              (str "###### " text " at level " level)))]
    (doseq [key (remove #(= :metadata %) (keys node))]
      (println)
      (if (spec/valid? ::parameter-spec (key node))
        (do (println
              (heading-style
                (format (get-in node [key :heading-template] "Parameter `%s`")
                        (name key))))
            (println)
            (println (get-in node [key :help] "Undocumented.")))
        (do (println (heading-style (format "Section `%s`" (name key))))
            (println)
            (println (get-in node [key :metadata :help] "Undocumented."))
            (print-markdown-fragment (key node) (inc level)))))))

(defn print-markdown-section
  "Print documentation for a section based on its flat specifications.
  Use the first entry as an introduction."
  [flat]
  (println (apply str (first flat)))
  (print-markdown-fragment (inflate flat) 2))

(defn validate-configuration
  "Attempt to describe any errors in the user configuration."
  [candidate]
  (try
    (validate-branch (inflate main-raws) candidate)
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
       (println "Validation error:" (.getMessage e))
       (println "    At key(s):" (string/join " >> " (:keys data)))
       (if (:accepted-keys data)
         (println "    Accepted key(s) there:" (:accepted-keys data)))
       (if (:raw-value data)
         (println "    Value before parsing:" (:raw-value data)))
       (if (:parsed-value data)
         (println "    Value after parsing:" (:parsed-value data)))
       (if (:spec-explanation data)
         (println "    Validator output:" (:spec-explanation data)))
       (if (:original-exception data)
         (do (println "    Caused by:")
             (print "      ")
             (println
               (string/join "\n      "
                 (string/split-lines (pr-str (:original-exception data)))))))
       (System/exit 1)))))
