;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameters                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This file is dedicated to variables you can use to personalize the models.
;;; You can’t control everything from here, but you can change a number of
;;; things without having to adjust more complex code.

(ns dactyl-keyboard.params
  (:require [clojure.string :refer [join]]
            [yaml.core :as yaml]
            [scad-clj.model :exclude [use import] :refer :all]
            [flatland.ordered.map :refer [ordered-map]]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.generics :as generics]))

;;; If you get creative and the bridge between the thumb and finger clusters
;;; is broken beyond what you can fix from here, you may want to look at
;;; tweaks.clj before touching more central code in the ‘cad’ folder.

;;; Throughout this program, the word ‘finger’ is used in its secondary sense
;;; to exclude the thumb.

;;; The fingers have their keys in a roughly rectangular matrix.
;;; The matrix follows the geometric coordinate system on the right-hand side
;;; of the keyboard.

;;; The key in the far left column, middle row (Caps Lock in ISO QWERTY)
;;; has the matrix coordinates (0, 0). Above it (e.g. Tab) is (0, 1), below it
;;; (e.g. left Shift) is (0, -1) and so on.

;;; The thumbs have their own 2 × 3 matrix where the top right is (0, 0),
;;; likewise aligned with the right-hand-side coordinate system.

;;; In touch typing terms, the majority of keys on finger row 0 will be on the
;;; home row, but the matrix in this program has no necessary relationship with
;;; the matrix in your MCU firmware (TMK/QMK etc.).

;;; All measurements of distance are in millimetres.
;;; This includes the size of threaded fasteners, which should be ISO metric.

;;; All angles must be specified in radians, and that is the default unit in
;;; scad-clj. The ‘deg->rad’ function can be called to convert from degrees.


;;;;;;;;;;;;;;;;
;; Key Layout ;;
;;;;;;;;;;;;;;;;

;; The shape of the finger key cluster is defined by the number of rows above
;; and below the home row in each column.
(def rows-above-home {0 1, 1 1, 2 1, 3 1, 4 0, 5 0})
(def rows-below-home {0 1, 1 2, 2 3, 3 2, 4 2, 5 2})
(def rows-default 1)  ; Default number of rows for columns omitted above.

;; The tenting angle controls overall left-to-right tilt.
(def tenting-angle (/ π 11.5))

;; The offset controls the overall height of the keyboard.
(def keyboard-z-offset 7)

;; Finger key placement parameters:
(def keycap-style :dsa)       ; :sa or :dsa.
(def column-style :standard)  ; :standard, :orthographic, or :fixed.

;; Cutouts for switches optionally include a trench beneath the switch, which
;; is useful when other choices here produce obstacles to soldering.
(def keyswitch-trench-depth 0)

(defn finger-column-curvature-centerrow [column]
  "Identify the row where Tait-Bryan pitch will have no progressive element."
  ;; This is a function (‘defn’) acting on a column (‘[column]’) of keys.
  (cond  ; The result here is conditional.
    (>= column 4) -1
    :else 0))

(def pitch-centerrow
  "The pitch of the center row controls the general front-to-back incline."
  (/ π 12))

(defn progressive-pitch [[column row]]
  "Define the progressive Tait-Bryan pitch of each finger key, acting in
  addition to the pitch of the center row.
  This controls the front-to-back curvature of the keyboard."
  (cond
    (= column 2) (if (pos? row) (deg->rad 22) (deg->rad 25))
    (and (= column 3) (pos? row)) (deg->rad 20)
    :else (deg->rad 26)))

;; β is the default progressive Tait-Bryan roll of each finger column.
;; β therefore controls the side-to-side curvature of the keyboard.
(def β (/ π 50))
(def curvature-centercol 3)   ; Column where the effect of β will be zero.

;; Individual columns may be translated (offset).
(defn finger-column-translation [column]
  (cond
    (= column 2) [0 4 -4.5]
    (>= column 4) [0 4 5]
    :else [0 0 0]))

;; Individual switches may be finely adjusted, including intrinsic rotation.
;; These are maps of column-row pairs to operator values.
(def finger-tweak-early-translation
  {[2 -3] [0 -7 2]})
(def finger-intrinsic-pitch
  {[2 -3] (/ π -8)})
(def finger-tweak-late-translation
  {})

;; Finger switch mounts may need more or less spacing depending on the size
;; of your keycaps, curvature etc.
(def finger-mount-separation-x 0.3)
(def finger-mount-separation-y -0.4)

;; Thumb key placement is similar to finger key placement:
(def thumb-cluster-offset-from-fingers [10 1 6])
(def thumb-cluster-column-offset [0 0 2])
(def thumb-cluster-rotation [(/ π 3) 0 (/ π -12)])
(def intrinsic-thumb-key-rotation
   {[-1 0] [0 (/ π -5) 0]
    [-1 -1] [0 (/ π -5) 0]
    [-1 -2] [0 (/ π -5) 0]
    [0 0] [0 (/ π -3) 0]
    [0 -1] [0 (/ π -3) 0]
    [0 -2] [0 (/ π -3) 0]})
(def intrinsic-thumb-key-translation
   {[0 0] [0 0 15]
    [0 -1] [0 0 15]
    [0 -2] [0 0 15]})
(def thumb-mount-separation 0)

;; Settings for column-style :fixed.
;; The defaults roughly match Maltron settings:
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column offsets above.
(def fixed-angles [(deg->rad 10) (deg->rad 10) 0 0 0 (deg->rad -15) (deg->rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; Relative to middle finger.
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg->rad 0))


;;;;;;;;;;;;;;;;;;;;;
;; Case Dimensions ;;
;;;;;;;;;;;;;;;;;;;;;

;; The size of the keyboard case is determined primarily by the key layout,
;; but there are other parameters for the thickness of the shell etc.

;; Switch mount plates and the webbing between them have configurable thickness.
(def plate-thickness 3)
(def web-thickness plate-thickness)

;; Wall shape and size:
;; These settings control the skirt of walling beneath each key mount on the
;; edges of the board. These walls are made up of hulls wrapping sets of
;; corner posts.
(def corner-post-width 1.3)
;; There is one corner post at each actual corner of every switch mount, and
;; more posts displaced from it, going down the sides. These anchor the
;; different parts of a wall relative to the switch mount. Their placement
;; is affected by the way the mount is rotated for the curvature of the board.
;; Offset are therefore in the mount’s frame of reference, not in the absolute
;; coordinate system.
(defn finger-key-wall-offsets [coordinates directions]
  "Return horizontal and vertical offsets from a finger key mount.
  These are needed for building a wall around the specific key mount."
  (let [[column row] coordinates]
   (if (>= row 2)
     [0 -13]  ; Extra space for ease of soldering at the high far end.
     (case coordinates
       [1 -2] [2 4]
       [2 -3] [0 (if (some #{:south} directions) -8 -16)]
       [4 1] [0 -1]
       [0 -10]))))
(defn finger-key-web [coordinates]
  "A predicate function for whether or not to web in a coordinate pair."
  (not (= coordinates [4 1])))
(defn thumb-key-wall-offsets [coordinates corner]
  (let [[column row] coordinates]
   (case column
     -1 [0 -8]
     [0 -10])))

;; Ultimately, from a set of posts placed by the offsets and the wall-thickness
;; parameter, the wall drops down to the floor. The actual thickness of the
;; wall at that point is a function of post size and the angle of the nearest
;; switch mount, as well as the thickness parameter itself.
(def wall-thickness 1)


;;;;;;;;;;;;;;;;
;; Wrist Rest ;;
;;;;;;;;;;;;;;;;

;; Shape of the top.
(def wrist-rest-σ 2.5)       ; Softness of curvature.
(def wrist-rest-θ 12)        ; Surface angle coefficient.
(def wrist-z-coefficient 3)  ; Relationship of wrist-rest-θ to height.


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

;; The ‘installation-angle’ is the angle of each half of the keyboard relative
;; to the lateral beam.
(def installation-angle (deg->rad -6))


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

(def configuration-raws
  "A flat version of the specification for a user configuration."
  [[:section [:wrist-rest]
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
     :validate [(partial contains? #{:threaded :solid})]}]
   [:parameter [:wrist-rest :preview]
    {:help (str "Preview mode. If `true`, this puts a model of the "
                "wrist rest in the same OpenSCAD file as the case. "
                "That model is simplified, intended for gauging "
                "distance, not for printing.")
     :default false
     :parse-fn boolean}]
   [:section [:wrist-rest :position]
    "The wrist rest is positioned in relation to a specific key."]
   [:parameter [:wrist-rest :position :finger-key-column]
    {:help (str "A finger key column ID. The wrist rest will be "
                "attached to the first key in that column.")
     :default 0
     :parse-fn int}]
   [:parameter [:wrist-rest :position :key-corner]
    {:help (str "A corner for the first key in the column.")
     :default "SSE"
     :parse-fn generics/string-corner
     :validate [generics/corner?]}]
   [:parameter [:wrist-rest :position :offset]
    {:help "An offset in mm from the selected key."
     :default [0 0]
     :parse-fn vec}]
   [:parameter [:wrist-rest :plinth-base-size]
    {:help (str "The size of the plinth up to but not including the "
                "narrowing upper lip and rubber parts.")
     :default [1 1 1]
     :parse-fn vec}]
   [:parameter [:wrist-rest :lip-height]
    {:help (str "The height of a narrowing, printed lip between "
                "the base of the plinth and the rubber part.")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :rubber]
    "The top of the wrist rest should be printed or cast in a soft material, "
    "such as silicone rubber."]
   [:section [:wrist-rest :rubber :height]
    "The piece of rubber extends a certain distance up into the air and down "
    "into the plinth."]
   [:parameter [:wrist-rest :rubber :height :above-lip]
    {:help (str "The height of the rubber wrist support, measured from the "
                "top of the lip.")
     :default 1
     :parse-fn int}]
   [:parameter [:wrist-rest :rubber :height :below-lip]
    {:help (str "The depth of the rubber wrist support, "
                "measured from the top of the lip.")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :rubber :shape]
    "The piece of rubber should fit the user’s hand."]
   [:parameter [:wrist-rest :rubber :shape :grid-size]
    {:default [1 1]}]
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
     :parse-fn int}]
   [:section [:wrist-rest :fasteners :mounts]
    "The mounts, or anchor points, for each fastener on each side."]
   [:parameter [:wrist-rest :fasteners :mounts :width]
    {:help (str "The width in mm of the face or front bezel on each "
                "connecting block that will anchor a fastener.")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :fasteners :mounts :case-side]
    "The side of the keyboard case."]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :finger-key-column]
    {:help (str "A finger key column ID. On the case side, fastener mounts "
                "will be attached at ground level near the first key in that "
                "column.")
     :default 0
     :parse-fn int}]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :key-corner]
    {:help "A corner of the key identified by `finger-key-column`."
     :default "SSE"
     :parse-fn generics/string-corner
     :validate [generics/corner?]}]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :offset]
    {:help (str "An offset in mm from the corner of "
                "the finger key to the mount.")
     :default [0 0]
     :parse-fn vec}]
   [:parameter [:wrist-rest :fasteners :mounts :case-side :depth]
    {:help (str "The thickness of the mount in mm "
                "along the axis of the fastener(s).")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :fasteners :mounts :plinth-side]
    "The side of the wrist rest."]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :offset]
    {:help (str "The offset in mm from the nearest "
                "corner of the plinth to the "
                "fastener mount attached to the "
                "plinth.")
     :default [1 1]
     :parse-fn vec}]
   [:parameter [:wrist-rest :fasteners :mounts :plinth-side :depth]
    {:help (str "The thickness of the mount in mm "
                "along the axis of the fastener(s). "
                "This is typically larger than the "
                "case-side depth to allow adjustment.")
     :default 1
     :parse-fn int}]
   [:section [:wrist-rest :solid-bridge]
    "This is only relevant with the `solid` style of wrist rest."]
   [:parameter [:wrist-rest :solid-bridge :height]
    {:help (str "The height in mm of the land bridge between the "
                "case and the plinth.")
     :default 14
     :parse-fn int}]
   [:section [:foot-plates]
    "Optional flat surfaces at ground level for adding silicone rubber feet "
    "or cork strips etc. to the "
    "bottom of the keyboard to increase friction and/or "
    "improve feel, sound and ground clearance."]
   [:parameter [:foot-plates :include]
    {:help (str "If `true`, include foot plates.")
     :default false
     :parse-fn boolean}]
   [:parameter [:foot-plates :height]
    {:help (str "The height in mm of each mounting plate.")
     :default 4}]
   [:parameter [:foot-plates :polygons]
    {:help (str "A list describing the horizontal shape, size and "
                "position of each mounting plate as a polygon.")
     :default []
     :parse-fn vec}]])

(def master
  "Collect structural metadata for a user configuration.
  This happens in two stages: Sections (branches), then individual parameters
  (leaves), as a simple means of ensuring that the tree is composed of
  ordered maps rather than ordinary maps."
  (reduce
    (fn [coll [type path & metadata]]
      (case type
        :section (assoc-in coll path
                   (ordered-map :metadata {:help (apply str metadata)}))
        :parameter (assoc-in coll path (first metadata))
        (throw (Exception. "Bad type in configuration master."))))
    (ordered-map)
    configuration-raws))

(def reserved-key?
  "Endpoint metadata imitates clojure.tools.cli but adds :help."
  (partial contains? #{:help :default :parse-fn :validate}))

(defn endpoint? [node]
  (boolean
    (and (map? node)
         (or (empty? node)
             (some reserved-key? (keys node))))))

(defn- print-markdown-fragment [node level]
  (let [h (join "" (repeat level "#"))]
    (doseq [key (remove #(= :metadata %) (keys node))]
      (println)
      (if (endpoint? (key node))
        (do (println h (format "Parameter `%s`" (name key)))
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

(declare validate-leaf validate-branch)

(defn validate-node [nominal candidate key]
  (assert (not (reserved-key? key)))
  (if (contains? nominal key)
    (if (endpoint? (key nominal))
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
                    {:type :superfluous-key :keys (list key)}))))

(defn validate-branch [nominal candidate]
  "Validate a fragment of a configuration received through the UI."
  (reduce (partial validate-node nominal)
          candidate
          (remove #(= :metadata %)
            (distinct (apply concat (map keys [nominal candidate]))))))

(defn parse-leaf [nominal candidate]
  (let [raw (or candidate (:default nominal))
        parse-fn (get nominal :parse-fn identity)]
   (try
     (parse-fn raw)
     (catch Exception e
       (throw (ex-info "Could not cast value to correct data type"
                        {:type :parse-error :value raw :original-exception e}))))))

(defn validate-leaf [nominal candidate]
  (assert (endpoint? nominal))
  (try
    (reduce
      (fn [unvalidated validator]
        (if (validator unvalidated)
           unvalidated
           (throw (ex-info "Value out of range"
                           {:type :validation-error
                            :value unvalidated
                            :raw-value candidate
                            :validator validator}))))
      (parse-leaf nominal candidate)
      (get nominal :validate [some?]))))

(defn validate-configuration [candidate]
  "Attempt to describe any errors in the user configuration."
  (try
     (validate-branch master candidate)
     (catch clojure.lang.ExceptionInfo e
       (let [data (ex-data e)]
        (println "Validation error:" (.getMessage e))
        (println "  At key(s):" (join " >> " (:keys data)))
        (if (:value data) (println "  Value:" (:value data)))
        (if (:raw-value data) (println "  Value before parsing:" (:raw-value data)))
        (if (:validator data) (println "  Validator:" (:validator data)))
        (System/exit 1)))))

(defn load-configuration [filepaths]
  "Read and combine YAML from files, in the order given."
  (let [load (fn [path]
               (if-let [data (yaml/from-file path)]
                 data
                 (do (println (format "Failed to load file “%s”." path))
                     (System/exit 1))))]
   (validate-configuration (apply generics/soft-merge (map load filepaths)))))
