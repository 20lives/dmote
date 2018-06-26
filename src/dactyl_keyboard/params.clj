;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameters                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This file describes the interpretation of configuration files for the
;;; application.

(ns dactyl-keyboard.params
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.generics :as generics]))

;;;;;;;;;;;;;;;;;;;;;
;; Case Dimensions ;;
;;;;;;;;;;;;;;;;;;;;;

;; Switch mount plates and the webbing between them have configurable thickness.
(def plate-thickness 3)
(def web-thickness plate-thickness)
(def corner-post-width 1.3)


;;;;;;;;;;;;;;;;
;; Back Plate ;;
;;;;;;;;;;;;;;;;

;; Given that independent movement of each half of the keyboard is not useful,
;; each half can include a mounting plate for a ‘beam’ (a straight piece of
;; wood, aluminium, rigid plastic etc.) to connect the two halves mechanically.
(def include-backplate-block true)

;; The plate will center along a finger column.
(def backplate-column 2)
(def backplate-offset [2 0.5 -11])

(def backplate-beam-height
  "The nominal height (vertical extent) of the plate itself.
  Because the plate is bottom-hulled to the floor and its vertical position
  is determined by the backplate-column and backplate-offset settings, this
  setting’s only real effect is on the area of the plate above its holes."
  20)

;; The backplate will have two holes for threaded fasteners.
(def backplate-fastener-distance 30)  ; Distance between fastener centers.
(def backplate-fastener-diameter 6)

;; The back plate block can optionally contain nut bosses for the fasteners.
(def include-backplate-boss true)


;;;;;;;;;;;;;;;;;;;;
;; Minor Features ;;
;;;;;;;;;;;;;;;;;;;;

;; Placement of the microcontroller unit.
(def mcu-finger-column 4)
(def mcu-offset [-0.5 3.5 0.5])
(def mcu-connector-direction :east)

;; Placement of the RJ9 port for interfacing the two halves.
(def rj9-translation [-1.7 -7.5 0])

;; LED holes along the inner wall. Defaults are for WS2818 at 17 mm intervals.
(def include-led-housings true)
(def led-housing-size 5.5)  ; Exaggerated for printing inaccuracy; really 5 mm.
(def led-emitter-diameter 4)
(def led-pitch 16.8)  ; Allowance for slight wall curvature.
(def led-amount 3)


;;;;;;;;;;;;;;;;;;;;;
;; Serialized Data ;;
;;;;;;;;;;;;;;;;;;;;;

;; This section loads, parses and validates a user configuration from YAML.

(defn- coalesce [coll [type path & metadata]]
  "Recursively assemble a tree structure from flat specifications."
  (case type
    :nest (assoc-in coll path
            (reduce
              coalesce
              (ordered-map :metadata {:help (apply str (rest metadata))})
              (first metadata)))
    :section (assoc-in coll path
               (ordered-map :metadata {:help (apply str metadata)}))
    :parameter (assoc-in coll path (first metadata))
    (throw (Exception. "Bad type in configuration master."))))

;; Parsers:

(defn string-corner [string]
  "For use with YAML, where string values are not automatically converted."
  ((keyword string) generics/keyword-to-directions))

(defn tuple-of [item-parser]
  "A maker of parsers for vectors."
  (fn [candidate] (into [] (map item-parser candidate))))

(defn map-like [key-value-parsers]
  "Return a parser of a map where the exact keys are known."
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
(spec/def ::supported-key-cluster #{:finger :thumb})
(spec/def ::supported-cluster-style #{:standard :orthographic})
(spec/def ::supported-wrist-rest-style #{:threaded :solid})
(spec/def ::flexcoord (spec/or :absolute int? :extreme #{:first :last}))
(spec/def ::2d-flexcoord (spec/coll-of ::flexcoord :count 2))
(spec/def ::key-coordinates ::2d-flexcoord)  ; Exposed for unit testing.
(spec/def ::3d-point (spec/coll-of number? :count 3))
(spec/def ::corner (set (vals generics/keyword-to-directions)))
(spec/def ::wall-segment (spec/int-in 0 5))
(spec/def ::wall-extent (spec/or :partial ::wall-segment :full #{:full}))
(spec/def ::tweak-plate-leaf
  (spec/or :short (spec/tuple keyword? ::corner ::wall-segment)
           :long (spec/tuple keyword? ::corner ::wall-segment ::wall-segment)))
(spec/def ::foot-plate-polygons (spec/coll-of ::foot-plate))

;; Composition of parsing and validation:

;; Leaf metadata imitates clojure.tools.cli with extras.
(spec/def ::parameter-descriptor
  #{:heading-template :help :default :parse-fn :validate})
(spec/def ::parameter-spec (spec/map-of ::parameter-descriptor some?))

(defn parse-leaf [nominal candidate]
  (let [raw (or candidate (:default nominal))
        parse-fn (get nominal :parse-fn identity)]
   (try
     (parse-fn raw)
     (catch Exception e
       (throw (ex-info "Could not cast value to correct data type"
                        {:type :parsing-error
                         :raw-value raw
                         :original-exception e}))))))

(declare validate-leaf validate-branch)

(defn validate-node [nominal candidate key]
  "Validate a fragment of a configuration received through the UI."
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

(defn validate-branch [nominal candidate]
  "Validate a section of a configuration received through the UI."
  (reduce (partial validate-node nominal)
          candidate
          (remove #(= :metadata %)
            (distinct (apply concat (map keys [nominal candidate]))))))

(defn validate-leaf [nominal candidate]
  "Validate a specific parameter received through the UI."
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

(def nested-raws
  "A flat version of a special part of a user configuration."
  [[:section [:parameters]
    "This section, and everything in it, can be repeated at several levels: "
    "Here at the global level, for each key cluster, for each column, and "
    "at the row level. See below. Only the most specific option available "
    "for each key will be applied to that key."]
   [:section [:parameters :layout]
    "How to place keys."]
   [:section [:parameters :layout :matrix]
    "Roughly how keys are spaced out to form a matrix."]
   [:section [:parameters :layout :matrix :neutral]
    "The neutral point in a column or row is where any progressive curvature "
    "both starts and has no effect."]
   [:parameter [:parameters :layout :matrix :neutral :column]
    {:help (str "An integer column ID.")
     :default 0
     :parse-fn int}]
   [:parameter [:parameters :layout :matrix :neutral :row]
    {:help (str "An integer row ID.")
     :default 0
     :parse-fn int}]
   [:section [:parameters :layout :matrix :separation]
    "Tweaks to control the systematic separation of keys. The parameters in "
    "this section will be multiplied by the difference between each affected "
    "key’s coordinates and the neutral column and row."]
   [:parameter [:parameters :layout :matrix :separation :column]
    {:help (str "A distance in mm.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :layout :matrix :separation :row]
    {:help (str "A distance in mm.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :layout :pitch]
    "Tait-Bryan pitch, meaning the rotation of keys around the x axis."]
   [:parameter [:parameters :layout :pitch :base]
    {:help (str "An angle in radians. Set at a high level, this controls the "
                "general front-to-back incline of a key cluster.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :layout :pitch :intrinsic]
    {:help (str "An angle in radians. Intrinsic pitching occurs early in key "
                "placement. It is typically intended to produce a tactile "
                "break between two rows of keys, as in the typewriter-like "
                "terracing common on flat keyboards with OEM-profile or "
                "similarly angled caps.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :layout :pitch :progressive]
    {:help (str "An angle in radians. This progressive pitch factor bends "
                "columns lengthwise. If set to zero, columns are flat.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :layout :roll]
    "Tait-Bryan roll, meaning the rotation of keys around the y axis."]
   [:parameter [:parameters :layout :roll :base]
    {:help (str "An angle in radians. This is the “tenting” angle. Applied to "
                "the finger cluster, it controls the overall left-to-right "
                "tilt of each half of the keyboard.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :layout :roll :progressive]
    {:help (str "An angle in radians. This progressive roll factor bends rows "
                "lengthwise, which also gives the columns a lateral curvature.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :layout :yaw]
    "Tait-Bryan yaw, meaning the rotation of keys around the z axis."]
   [:parameter [:parameters :layout :yaw :base]
    {:help (str "An angle in radians.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :layout :translation]
    "Translation in the geometric sense, displacing keys in relation to each "
    "other. Depending on when this translation takes places, it may have a "
    "a cascading effect on other aspects of key placement. All measurements "
    "are in mm."]
   [:parameter [:parameters :layout :translation :early]
    {:help (str "A 3-dimensional vector. ”Early” translation happens before "
                "other operations in key placement and therefore has the "
                "biggest knock-on effects.")
     :default [0 0 0]
     :parse-fn vec
     :validate [::3d-point]}]
   [:parameter [:parameters :layout :translation :mid]
    {:help (str "A 3-dimensional vector. This happens after columns are styled "
                "but before base pitch and roll. As such it is a good place to "
                "adjust whole columns for relative finger length.")
     :default [0 0 0]
     :parse-fn vec
     :validate [::3d-point]}]
   [:parameter [:parameters :layout :translation :late]
    {:help (str "A 3-dimensional vector. “Late” translation is the last step "
                "in key placement and therefore interacts very little with "
                "other steps. As a result, the z-coordinate, which is the last "
                "number in this vector, serves as a general vertical offset "
                "of the finger key cluster from the ground plane. If set at a "
                "high level, this controls the overall height of the keyboard, "
                "including the height of the case walls.")
     :default [0 0 0]
     :parse-fn vec
     :validate [::3d-point]}]
   [:section [:parameters :channel]
    "Above each switch mount, there is a channel of negative space for the "
    "user’s finger and the keycap to move inside. This is only useful in those "
    "cases where nearby walls or webbing between mounts on the keyboard would "
    "otherwise obstruct movement."]
   [:parameter [:parameters :channel :height]
    {:help (str "The height in mm of the negative space, starting from the "
                "bottom edge of each keycap in its pressed (active) state.")
     :default 1
     :parse-fn num}]
   [:parameter [:parameters :channel :top-width]
    {:help (str "The width in mm of the negative space at its top. Its width "
                "at the bottom is defined by the keycap.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :channel :margin]
    {:help (str "The width in mm of extra negative space around the edges of "
                "a keycap, on all sides.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :wall]
    "The walls of the keyboard case support the key mounts and protect the "
    "electronics. They are generated by an algorithm that walks around each "
    "key cluster.\n"
    "\n"
    "This section determines the shape of the case wall, specifically "
    "the skirt around each key mount along the edges of the board. These skirts "
    "are made up of geometric hulls wrapping sets of corner posts.\n"
    "\n"
    "There is one corner post at each actual corner of every key mount. "
    "More posts are displaced from it, going down the sides. Their placement "
    "is affected by the way the key mounts are rotated etc."]
   [:parameter [:parameters :wall :thickness]
    {:help (str "A distance in mm.\n"
                "\n"
                "This is actually the distance between some pairs of corner "
                "posts, in the key mount’s frame of reference. It is therefore "
                "inaccurate as a measure of wall thickness on the x-y plane.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :wall :bevel]
    {:help (str "A distance in mm.\n"
                "\n"
                "This is applied at the very top of a wall, making up the "
                "difference between wall segments 0 and 1. It is applied again "
                "at the bottom, making up the difference between segments "
                "3 and 4.")
     :default 0
     :parse-fn num}]
   [:section [:parameters :wall :north]
    "Throughout the program, “north” refers to the side of a key "
    "facing directly away from the user, barring yaw.\n"
    "\n"
    "This section describes the shape of the wall on the north "
    "side of the keyboard. There are identical sections for the "
    "other cardinal directions."]
   [:parameter [:parameters :wall :north :extent]
    {:help (str "Two types of values are permitted here:\n\n"
                "* The keyword `full`. This means a complete "
                "wall extending from the key mount all the way down to the "
                "ground via segments numbered 0 through 4 and a vertical drop "
                "thereafter.\n"
                "* An integer corresponding to the last wall segment to be "
                "included. A zero means there will be no wall. No matter the "
                "number, there will be no vertical drop to the floor.")
     :default :full
     :parse-fn keyword-or-integer
     :validate [::wall-extent]}]
   [:parameter [:parameters :wall :north :parallel]
    {:help (str "A distance in mm. The later wall segments extend this far "
                "away from the corners of their key mount, on its plane.")
     :default 0
     :parse-fn num}]
   [:parameter [:parameters :wall :north :perpendicular]
    {:help (str "A distance in mm. The later wall segments extend this far "
                "away from the corners of their key mount, away from its plane.")
     :default 0
     :parse-fn num}]
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
    {:default 0 :parse-fn num}]])


;; Ultimately, from a set of posts placed by the offsets and the wall-thickness
;; parameter, the wall drops down to the floor. The actual thickness of the
;; wall at that point is a function of post size and the angle of the nearest
;; switch mount, as well as the thickness parameter itself.

(def nested-cooked (reduce coalesce (ordered-map) nested-raws))

(def parse-overrides
  "A function to parse input for the entire [:by-key :clusters] section."
  (let [iteration identity] ;#(validate-node nested-cooked % :parameters)
    (map-of
      (partial spec/conform ::supported-key-cluster)
      (map-like
        {:parameters iteration
         :columns
          (map-of
            keyword-or-integer
            (map-like
              {:parameters iteration
               :rows
                 (map-of
                   keyword-or-integer
                   (map-like {:parameters iteration}))}))}))))

;; A predicate made from nested-cooked is applied in validation of nested appearances.
(spec/def ::parameters #(some? (validate-branch (:parameters nested-cooked) %)))
(spec/def ::individual-row (spec/keys :opt-un [::parameters]))
(spec/def ::rows (spec/map-of ::flexcoord ::individual-row))
(spec/def ::individual-column (spec/keys :opt-un [::rows ::parameters]))
(spec/def ::columns (spec/map-of ::flexcoord ::individual-column))
(spec/def ::individual-cluster (spec/keys :opt-un [::columns ::parameters]))
(spec/def ::overrides (spec/map-of ::supported-key-cluster ::individual-cluster))

(def configuration-raws
  "A flat version of the specification for a user configuration."
  [[:section [:keycaps]
    "Keycaps are the plastic covers placed over the switches. Their shape will "
    "help determine the spacing between key mounts if the keyboard is curved. "
    "Negative space is also reserved for the caps."]
   [:parameter [:keycaps :preview]
    {:help (str "If `true`, include models of the keycaps. This is intended "
                "for illustration in development. The models are not good "
                "enough for printing.")
     :default false
     :parse-fn boolean}]
   [:parameter [:keycaps :body-height]
    {:help (str "The height in mm of each keycap, measured from top to bottom "
                "of the entire cap by itself.\n\n"
                "An SA cap would be about 11.6 mm, DSA 7.3 mm.")
     :default 1
     :parse-fn num}]
   [:parameter [:keycaps :resting-clearance]
    {:help (str "The height in mm of the air gap between keycap and switch "
                "mount, in a resting state.")
     :default 1
     :parse-fn num}]
   [:section [:switches]
    "Electrical switches close a circuit when pressed. They cannot be "
    "printed. This section specifies how much space they need to be mounted."]
   [:parameter [:switches :travel]
    {:help (str "The distance in mm that a keycap can travel vertically when "
                "mounted on a switch.")
     :default 1
     :parse-fn num}]
   [:section [:key-clusters]
     "This section describes where to put keys on the keyboard."]
   [:section [:key-clusters :finger]
    "The main cluster of keys, for “fingers” in a sense excluding the thumb."
    "Everything else is placed in relation to the finger cluster."]
   [:parameter [:key-clusters :finger :style]
    {:help (str "Cluster layout style. One of:\n"
                "\n"
                "* `standard`: Both columns and rows have the same type of "
                "curvature applied in a logically consistent manner."
                "* `orthographic`: Rows are curved somewhat differently. "
                "This creates more space between columns and may prevent "
                "key mounts from fusing together if you have a broad matrix.")
     :default :standard
     :parse-fn keyword
     :validate [::supported-cluster-style]}]
   [:parameter [:key-clusters :finger :matrix-columns]
    {:help (str "A list of key columns. Columns are aligned with the user’s "
                "fingers. Each column will be known by its index in this "
                "list, starting at zero for the first item. Each item may "
                "contain:\n"
                "\n"
                "* `rows-above-home`: An integer specifying the amount of keys "
                "on the far side of the home row in the column. If "
                "this parameter is omitted, the effective value will be zero.\n"
                "* `rows-below-home`: An integer specifying the amount of keys "
                "on the near side of the home row in the column. If this "
                "parameter is omitted, the effective value will be zero.\n"
                "\n"
                "For example, on a normal QWERTY keyboard, H is on the home "
                "row for purposes of touch typing, and you would probably want "
                "to use it as such here too, even though the matrix in this "
                "program has no necessary relationship with touch typing, "
                "nor with the matrix in your MCU firmware (TMK/QMK etc.). "
                "Your H key will then get the coordinates "
                "[0, 0] as the home-row key in the far left column on the "
                "right-hand side of the keyboard.\n"
                "\n"
                "In that first column, to continue the QWERTY pattern, you "
                "will want `rows-above-home` set to 1, to make a Y key, or 2 "
                "to make a 6 key, or 3 to make a function key above the 6. "
                "Your Y key will have the coordinates [0, 1]. Your 6 key will "
                "have the coordinates [0, 2], etc.\n"
                "\n"
                "Still in that first column, to finish the QWERTY pattern, "
                "you will want `rows-below-home` set to 2, where the two "
                "keys below H are N (coordinates [0, -1]) and Space "
                "(coordinates [0, -2]).\n"
                "\n"
                "The next item in the list will be column 1, with J as [1, 0] "
                "and so on. On the left-hand side of a DMOTE, everything is "
                "mirrored so that [0, 0] will be G instead of H, [1, 0] will "
                "be F instead of J, and so on.")
     :default [{}]
     :parse-fn vec}]
   [:parameter [:key-clusters :finger :aliases]
    {:help (str "A map of short names to specific keys by coordinate pair. "
                "Such aliases are for use elsewhere in the configuration.")
     :default {:origin [0 0]}
     :parse-fn (map-of keyword (tuple-of keyword-or-integer))
     :validate [(spec/map-of keyword? ::2d-flexcoord)]}]
   [:section [:key-clusters :thumb]
    "A cluster of keys just for the thumb."]
   [:section [:key-clusters :thumb :position]
    "The thumb cluster is positioned in relation to the finger cluster."]
   [:parameter [:key-clusters :thumb :position :key-alias]
    {:help (str "A finger key as named under `aliases` above.")
     :default :origin
     :parse-fn keyword}]
   [:parameter [:key-clusters :thumb :position :offset]
    {:help (str "A 3-dimensional offset in mm from the indicated key.")
     :default [0 0 0]
     :parse-fn (tuple-of num)
     :validate [::3d-point]}]
   [:parameter [:key-clusters :thumb :style]
    {:help (str "As for the finger cluster.")
     :default :standard
     :parse-fn keyword
     :validate [::supported-cluster-style]}]
   [:parameter [:key-clusters :thumb :matrix-columns]
    {:help (str "As for the finger cluster.")
     :default [{}]
     :parse-fn vec}]
   [:parameter [:key-clusters :thumb :aliases]
    {:help (str "As for the finger cluster. Note, however, that aliases must "
                "be unique even between clusters.")
     :default {}
     :parse-fn (map-of keyword (tuple-of keyword-or-integer))
     :validate [(spec/map-of keyword? ::2d-flexcoord)]}]
   [:nest [:by-key] nested-raws
    "This section is special. It’s nested for all levels of specificity."]
   [:parameter [:by-key :clusters]
    {:heading-template "Section `%s` ← overrides go in here"
     :help (str "This is an anchor point for overrides of the `parameters` "
                "section described above. Overrides start at the key cluster "
                "level. This section therefore permits keys that identify "
                "specific key clusters.\n"
                "\n"
                "For each such key, two subsections are permitted: A new, more "
                "specific `parameters` section and a `columns` "
                "section. Columns are indexed by their ordinal integers or "
                "the words “first” or “last”, which take priority.\n"
                "\n"
                "A column can have its own `parameters` and "
                "its own `rows`, which are indexed in relation to the home "
                "row or again with “first” or “last”. Finally, each row can "
                "have its own `parameters`, which are specific to the "
                "full combination of cluster, column and row.\n"
                "\n"
                "WARNING: Due to a peculiarity of the YAML parser, take care "
                "to quote your numeric column and row indices as strings.\n"
                "\n"
                "In the following example, the parameter `P`, which is not "
                "really supported, will have the value “true” for all keys "
                "except the one closest to the user (“first” row) in the "
                "second column from the left on the right-hand side of the "
                "keyboard (column 1; this is the second from the right on the "
                "left-hand side of the keyboard).\n"
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
                "                P: false```")
     :default {}
     :parse-fn parse-overrides
     :validate [::overrides]}]
   [:section [:case]
    "The most important part of the keyboard case is generated from the "
    "`wall` parameters above. This section deals with lesser features of the "
    "case."]
   [:section [:case :rear-housing]
    "The furthest row of the key cluster can be extended into a rear housing "
    "for the MCU and various other features."]
   [:parameter [:case :rear-housing :include]
    {:help (str "If `true`, add a rear housing. Please arrange case "
                "walls so as not to interfere, by removing them along the "
                "far side of the last row.")
     :default false
     :parse-fn boolean}]
   [:parameter [:case :rear-housing :distance]
    {:help (str "The horizontal distance in mm between the furtest key in "
                "the row and the roof of the rear housing.")
     :default 0
     :parse-fn num}]
   [:parameter [:case :rear-housing :height]
    {:help (str "The height in mm of the roof, over the floor.")
     :default 0
     :parse-fn num}]
   [:section [:case :rear-housing :offsets]
    "Modifiers for the size of the roof. All are in mm."]
   [:parameter [:case :rear-housing :offsets :north]
    {:help (str "The total extent on the y axis.")
     :default 0
     :parse-fn num}]
   [:parameter [:case :rear-housing :offsets :west]
    {:help (str "The extent on the x axis past the first key in the row.")
     :default 0
     :parse-fn num}]
   [:parameter [:case :rear-housing :offsets :east]
    {:help (str "The extent on the x axis past the last key in the row.")
     :default 0
     :parse-fn num}]
   [:parameter [:case :tweaks]
    {:help (str "Additional shapes. This is usually needed to bridge gaps "
                "between the walls of the finger and key clusters.\n"
                "\n"
                "The expected value here is an arbitrarily nested structure, "
                "starting with a list. Each item in the list can follow one of "
                "the following patterns:\n"
                "\n"
                "* A leaf node. This is a 3- or 4-tuple list with contents "
                "specified below.\n"
                "* A map, representing an instruction to combine nested "
                "items in a specific way.\n"
                "* A list of any combination of the other two types. This type "
                "exists at the top level, and as the immediate child of each "
                "map node.\n"
                "\n"
                "Each leaf node identifies particular set of key mount corner "
                "posts. These are identical to the posts used to build the "
                "walls (see above), but this section gives you greater freedom "
                "in how to combine them. A leaf node must contain:\n"
                "\n"
                "* A key alias defined under `key-clusters`.\n"
                "* A key corner ID, such as `NNE` for north by north-east.\n"
                "* A wall segment ID (0 to 4).\n"
                "\n"
                "Together, these identify a starting segment. Optionally, a "
                "leaf node may contain a second segment ID trailing the first. "
                "In that case, the leaf will represent the geometric hull of "
                "the first and second indicated segments, plus all in "
                "between.\n"
                "\n"
                "By default, a map node will create a geometric hull around "
                "its child nodes. However, this behaviour can be modified. The "
                "following keys are recognized:\n"
                "\n"
                "* `to-ground`: If `true`, child nodes will be extended "
                "vertically down to the ground plane, as with a `full` wall.\n"
                "* `chunk-size`: Any integer greater than 1. If this is set, "
                "child nodes will not share a single geometric hull. Instead, "
                "there will be a sequence of smaller hulls, each encompassing "
                "this many items.\n"
                "* `highlight`: If `true`, render the node in OpenSCAD’s "
                "highlighting style. This is convenient while you work.\n"
                "* `hull-around`: The list of child nodes. Required.\n"
                "\n"
                "In the following example, `A` and `B` are aliases that would "
                "be defined elsewhere. The example is interpreted to mean that "
                "a plate should be created stretching from the "
                "south-by-southeast corner of `A` to the north-by-northeast "
                "corner of `B`. Due to `chunk-size` 2, that first plate will "
                "be joined, not hulled, with a second plate from `B` back to a "
                "different corner of `A`, with a longer stretch of wall "
                "segments down the corner of `A`.\n"
                "\n"
                "```case:\n"
                "  tweaks:\n"
                "    - chunk-size: 2\n"
                "      hull-around:\n"
                "      - [A, SSE, 0]\n"
                "      - [B, NNE, 0]\n"
                "      - [A, SSW, 0, 4]```\n")
     :default []
     :parse-fn case-tweaks
     :validate [::hull-around]}]
   [:section [:case :foot-plates]
    "Optional flat surfaces at ground level for adding silicone rubber feet "
    "or cork strips etc. to the bottom of the keyboard to increase friction "
    "and/or improve feel, sound and ground clearance."]
   [:parameter [:case :foot-plates :include]
    {:help (str "If `true`, include foot plates.")
     :default false
     :parse-fn boolean}]
   [:parameter [:case :foot-plates :height]
    {:help (str "The height in mm of each mounting plate.")
     :default 4
     :parse-fn num}]
   [:parameter [:case :foot-plates :polygons]
    {:help (str "A list describing the horizontal shape, size and "
                "position of each mounting plate as a polygon.")
     :default []
     :parse-fn key-based-polygons
     :validate [::foot-plate-polygons]}]
   [:section [:wrist-rest]
    "An optional extension to support the user’s wrist."]
   [:parameter [:wrist-rest :include]
    {:help (str "If `true`, include a wrist rest with the keyboard.")
     :default false
     :parse-fn boolean}]
   [:parameter [:wrist-rest :style]
    {:help (str "The style of the wrist rest. Available styles are:\n\n"
                "* `threaded`: threaded fastener(s) connect the case "
                "and wrist rest.\n"
                "* `solid`: a printed plastic bridge along the ground "
                "as part of the model.")
     :default :threaded
     :parse-fn keyword
     :validate [::supported-wrist-rest-style]}]
   [:parameter [:wrist-rest :preview]
    {:help (str "Preview mode. If `true`, this puts a model of the "
                "wrist rest in the same OpenSCAD file as the case. "
                "That model is simplified, intended for gauging "
                "distance, not for printing.")
     :default false
     :parse-fn boolean}]
   [:section [:wrist-rest :position]
    "The wrist rest is positioned in relation to a key mount."]
   [:parameter [:wrist-rest :position :key-alias]
    {:help (str "A named key where the wrist rest will attach. "
                "The vertical component of its position is ignored.")
     :default :origin
     :parse-fn keyword}]
   [:parameter [:wrist-rest :position :offset]
    {:help (str "An offset in mm from the selected key to one corner of the "
                "base of the wrist rest. Specifically, it is the corner close "
                "to the keyboard case, on the right-hand side of the "
                "right-hand half.")
     :default [0 0]
     :parse-fn vec}]
   [:section [:wrist-rest :shape]
    "The wrist rest needs to fit the user’s hand."]
   [:parameter [:wrist-rest :shape :plinth-base-size]
    {:help (str "The size of the plinth up to but not including the "
                "narrowing upper lip and rubber parts.")
     :default [1 1 1]
     :parse-fn vec
     :validate [::3d-point]}]
   [:parameter [:wrist-rest :shape :chamfer]
    {:help (str "A distance in mm. The plinth is shrunk and then regrown by "
                "this much to chamfer its corners.")
     :default 1
     :parse-fn num}]
   [:parameter [:wrist-rest :shape :lip-height]
    {:help (str "The height of a narrowing, printed lip between "
                "the base of the plinth and the rubber part.")
     :default 1
     :parse-fn num}]
   [:section [:wrist-rest :shape :pad]
    "The top of the wrist rest should be printed or cast in a soft material, "
    "such as silicone rubber."]
   [:parameter [:wrist-rest :shape :pad :surface-heightmap]
    {:help (str "A filepath. The path, and file, will be interpreted by "
                "OpenScad, using its [`surface()` function("
                "https://en.wikibooks.org/wiki/OpenSCAD_User_Manual/"
                "Other_Language_Features#Surface).\n"
                "\n"
                "The file should contain a heightmap to describe the surface "
                "of the rubber pad.")
     :default "resources/heightmap/default.dat"}]
   [:section [:wrist-rest :shape :pad :height]
    "The piece of rubber extends a certain distance up into the air and down "
    "into the plinth. All measurements in mm."]
   [:parameter [:wrist-rest :shape :pad :height :surface-range]
    {:help (str "The vertical range of the heightmap. Whatever values are in "
                "the heightmap will be normalized to this scale.")
     :default 1
     :parse-fn num}]
   [:parameter [:wrist-rest :shape :pad :height :lip-to-surface]
    {:help (str "The part of the rubber pad between the top of the lip and "
                "the point where the heightmap comes into effect. This is "
                "useful if your heightmap itself has very low values at the "
                "edges, such that moulding and casting it without a base "
                "would be difficult.")
     :default 1
     :parse-fn num}]
   [:parameter [:wrist-rest :shape :pad :height :below-lip]
    {:help (str "The depth of the rubber wrist support, measured from the top "
                "of the lip, going down into the plinth. This part of the pad "
                "just keeps it in place.")
     :default 1
     :parse-fn num}]
   [:section [:wrist-rest :fasteners]
    "This is only relevant with the `threaded` style of wrist rest."]
   [:parameter [:wrist-rest :fasteners :amount]
    {:help (str "The number of fasteners connecting each case to "
                "its wrist rest.")
     :default 1
     :parse-fn int}]
   [:parameter [:wrist-rest :fasteners :diameter]
    {:help (str "The ISO metric diameter of each fastener.")
     :default 1
     :parse-fn int}]
   [:parameter [:wrist-rest :fasteners :length]
    {:help (str "The length in mm of each fastener.")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :fasteners :height]
    "The vertical level of the fasteners."]
   [:parameter [:wrist-rest :fasteners :height :first]
    {:help (str "The distance in mm from the bottom of the first fastener "
                "down to the ground level of the model.")
     :default 0
     :parse-fn int}]
   [:parameter [:wrist-rest :fasteners :height :increment]
    {:help (str "The vertical distance in mm from the center of each fastener "
                "to the center of the next.")
     :default 0
     :parse-fn num}]
   [:section [:wrist-rest :fasteners :mounts]
    "The mounts, or anchor points, for each fastener on each side."]
   [:parameter [:wrist-rest :fasteners :mounts :width]
    {:help (str "The width in mm of the face or front bezel on each "
                "connecting block that will anchor a fastener.")
     :default 1
     :parse-fn num}]
   [:section [:wrist-rest :fasteners :mounts :case-side]
    "The side of the keyboard case."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :key-alias]
    {:help (str "A named key. A mount point on the case side "
                "will be placed near this key.")
     :default :origin
     :parse-fn keyword}]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :offset]
    {:help (str "An offset in mm from the key to the mount.")
     :default [0 0]
     :parse-fn vec}]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :depth]
    {:help (str "The thickness of the mount in mm "
                "along the axis of the fastener(s).")
     :default 1
     :parse-fn num}]
   [:section [:wrist-rest :fasteners :mounts :plinth-side]
    "The side of the wrist rest."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :offset]
    {:help (str "The offset in mm from the corner of the plinth to "
                "the fastener mount point attached to the plinth.")
     :default [0 0]
     :parse-fn vec}]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :depth]
    {:help (str "The thickness of the mount in mm "
                "along the axis of the fastener(s). "
                "This is typically larger than the "
                "case-side depth to allow adjustment.")
     :default 1
     :parse-fn num}]
   [:section [:wrist-rest :solid-bridge]
    "This is only relevant with the `solid` style of wrist rest."]
   [:parameter [:wrist-rest :solid-bridge :width]
    {:help (str "The width in mm of the land bridge between the case and the "
                "plinth. On the right-hand side of the keyboard, the bridge "
                "starts from the wrist rest `key-alias` and extends this many "
                "mm to the left.\n"
                "\n"
                "The value of this parameter, and the shape of the keyboard "
                "case, should be arranged in a such a way that the land bridge "
                "is wedged in place by a vertical wall on that left side.")
     :default 1
     :parse-fn num}]
   [:parameter [:wrist-rest :solid-bridge :height]
    {:help (str "The height in mm of the land bridge between the "
                "case and the plinth.")
     :default 1
     :parse-fn num}]])

(def master
  "Collected structural metadata for a user configuration."
  (reduce coalesce (ordered-map) configuration-raws))

(defn- print-markdown-fragment [node level]
  (let [h (string/join "" (repeat level "#"))]
    (doseq [key (remove #(= :metadata %) (keys node))]
      (println)
      (if (spec/valid? ::parameter-spec (key node))
        (do (println h (format (get-in node [key :heading-template] "Parameter `%s`") (name key)))
            (println)
            (println (get-in node [key :help] "Undocumented.")))
        (do (println h (format "Section `%s`" (name key)))
            (println)
            (println (get-in node [key :metadata :help] "Undocumented."))
            (print-markdown-fragment (key node) (inc level)))))))

(defn print-markdown-documentation []
  (println "# Configuration options")
  (println)
  (println (str "Each heading in this document represents a recognized "
                "configuration key in YAML files for a DMOTE variant."))
  (println)
  (println (str "This documentation was generated from the application CLI."))
  (print-markdown-fragment master 2))

(defn validate-configuration [candidate]
  "Attempt to describe any errors in the user configuration."
  (try
     (validate-branch master candidate)
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
