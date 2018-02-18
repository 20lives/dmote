;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-Manuform Keyboard — Opposable Thumb Edition – CAD Script ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
;;; your MCU firmware (TMK/QMK etc.) matrix.

(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  "Convert a number of degrees to radians."
  (* (/ degrees 180) π))

;; Each switch mount has four corners with offsets in two directions.
;; Capitals in symbol names are reserved for these shorthand definitions
;; of the four corners. In each case, the cardinal direction naming the side
;; of the key comes first. The second item names one end of that side.
(def NNE [:north :east])
(def ENE [:east :north])
(def SSE [:south :east])
(def ESE [:east :south])
(def SSW [:south :west])
(def WSW [:west :south])
(def NNW [:north :west])
(def WNW [:west :north])

;;;;;;;;;;;;;;;;;;;;;;
;; Shape Parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

;; The shape of the finger key cluster is defined by the number of rows above
;; and below the home row in each column.
(def rows-above-home {0 1, 1 2, 2 2, 3 2, 4 1, 5 0})
(def rows-below-home {0 1, 1 2, 2 3, 3 2, 4 2, 5 2})
(def rows-default 1)  ; Default number of rows for columns omitted above.

;; The tenting angle controls overall left-to-right tilt.
(def tenting-angle (/ π 8.5))

;; keyboard-z-offset controls the overall height of the keyboard.
(def keyboard-z-offset 13)

;; Finger key placement parameters:
(def keycap-style :dsa)       ; :sa or :dsa.
(def column-style :standard)  ; :standard, :orthographic, or :fixed.
;; α is the default progressive Tait-Bryan pitch of each finger row.
;; α therefore controls the front-to-back curvature of the keyboard.
(def α (/ π 9.2))
;; β is the default progressive Tait-Bryan roll of each finger column.
;; β therefore controls the side-to-side curvature of the keyboard.
(def β (/ π 50))

;; Individual columns may have a non-standard curvature.
(def finger-column-tweak-α
  {2 (/ π 8.2)
   4 (/ π 6.6)
   5 (/ π 6.6)})
(def pitch-centerrow (/ π 12))
(def curvature-centercol 3)   ; Column where the effect of β will be zero.

(defn finger-column-curvature-centerrow [column]
  "Identify the row where the effect of α will be zero."
  (cond
    (>= column 4) -1
    :else 0))

;; Individual columns may be translated (offset).
(defn finger-column-translation [column]
  (cond
    (= column 2) [0 4 -4.5]
    (>= column 4) [0 4 5]
    :else [0 0 0]))

;; Individual switches may be finely adjusted, including intrinsic rotation.
;; These are maps of column-row pairs to operator values.
(def finger-tweak-early-translation
  {[1 -2] [0 -5 2]
   [2 -3] [0 -9 1]
   [3 -2] [0 -5 2]})
(def finger-intrinsic-pitch
  {[1 -2] (/ π -10)
   [2 -3] (/ π -6)
   [3 -2] (/ π -10)
   [4 1] (/ π -2.5)})
(def finger-tweak-late-translation
  {[4 1] [0 10 -5]})

;; Finger switch mounts may need more or less spacing depending on the size
;; of your keycaps, curvature etc.
(def finger-mount-separation-x 0.3)
(def finger-mount-separation-y -0.4)

;; Thumb key placement is similar to finger key placement:
(def thumb-cluster-offset-from-fingers [6 3 0])
(def thumb-cluster-column-offset [0 0 2])
(def thumb-cluster-rotation [(/ π 3) 0 (/ π -6)])
(def intrinsic-thumb-key-rotation
   {[-1 0] [0 (/ π -5) 0]
    [-1 -1] [0 (/ π -5) 0]
    [-1 -2] [0 (/ π -5) 0]
    [0 0] [0 (/ π -3) 0]
    [0 -1] [0 (/ π -3) 0]
    [0 -2] [0 (/ π -3) 0]})
(def intrinsic-thumb-key-translation
   {[-1 0] [0 0 0]
    [-1 -1] [0 0 0]
    [-1 -2] [0 0 0]
    [0 0] [0 0 17]
    [0 -1] [0 0 17]
    [0 -2] [0 0 17]})
(def thumb-mount-separation 0)

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
       [1 -2] [2 8]
       [2 -3] [0 -13]  ; Extra space for ease of soldering.
       [0 -10]))))
(defn thumb-key-wall-offsets [coordinates corner]
  (let [[column row] coordinates]
   (case column
     -1 [0 -5]
     [0 -10])))

;; Ultimately, from a set of posts placed by the offsets and the wall-thickness
;; parameter, the wall drops down to the floor. The actual thickness of the
;; wall at that point is a function of post size and the angle of the nearest
;; switch mount, as well as the thickness parameter itself.
(def wall-thickness 1)

;; It may be desirable to add rubber feet, cork etc. to the bottom of the
;; keyboard, to increase friction and/or improve feel and sound.
;; Plates can be added through ‘foot-plate-posts’: A vector of vectors,
;; defining floor-level plates in relation to finger keys.
;; A future version may support threaded holes through these feet for mounting
;; printed parts on solid plates.
(def include-feet true)
(def foot-height 4)
(def foot-plate-posts
  [;; Close to user, fairly central, in two parts:
   [[[2 -3] NNW] [[2 -3] NNE] [[3 -2] SSW] [[2 -2] SSE]]
   [[[3 -2] SSW] [[3 -2] SSE] [[4 -2] WSW] [[2 -2] SSE]]
   ;; On the left, using optional offsets:
   [[[0 -1] WSW [0 0]] [[0 -1] WSW [4 -18 0]] [[0 -1] WSW [11 -16]] [[0 -1] WSW [12 -3]]]
   ;; On the right:
   [[[5 0] WNW] [[5 0] NNE] [[5 -1] ENE]]])

;; Settings for column-style :fixed.
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column offsets above.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; Relative to middle finger.
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;; Wrist rest shape:
(def wrist-plinth-width 35)
(def wrist-plinth-length 62)
(def wrist-plinth-height 50)
(def wrist-connection-column 4)
(def wrist-connection-offset [5 -30])
(def wrist-rest-σ 2.5)       ; Softness of curvature.
(def wrist-rest-θ 12)        ; Surface angle coefficient.
(def wrist-z-coefficient 3)  ; Relationship of wrist-rest-θ to height.
(def wrist-connector-height 15)

;; Given that independent movement of each half of the keyboard is not useful,
;; each half can include a mounting plate for a ‘beam’ (a straight piece of
;; wood, aluminium, rigid plastic etc.) to connect the two halves mechanically.
(def include-backplate true)
;; The backplate will center along a finger column.
(def backplate-column 2)
(def backplate-offset [0 0 -18])
;; The backplate will have two holes for threaded fasteners.
(def backplate-fastener-distance 30)
(def backplate-fastener-diameter 5)
(def backplate-beam-height 20)
;; The ‘installation-angle’ is the angle of each half of the keyboard relative
;; to the lateral beam.
(def installation-angle (deg2rad -6))

;; Minor features:
(def mcu-finger-column 4)
(def mcu-connector-direction :east)
(def rj9-translation [-3 -8 0])

;; LED holes along the inner wall. Defaults are for WS2818 at 17 mm intervals.
(def include-led-housings true)
(def led-housing-size 5.3)  ; Exaggerated; really 5 mm.
(def led-emitter-diameter 4)
(def led-pitch 16.8)  ; Allowance for slight wall curvature.
(def led-amount 3)

;;;;;;;;;;;;;;;;;;;;;;;
;; General Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn abs [n]
  "The absolute of n."
  (max n (- n)))

(defn 𝒩 [x ￼￼μ σ]
  "The normal distribution’s probability density function with unicode-math."
  (let [v (ⁿ σ 2)]
    (* (/ 1 (√ (* 2 π v)))
       (ⁿ e (- (/ (ⁿ (- x ￼￼μ) 2) (* 2 v)))))))

(defn 𝒩′ [x ￼￼μ σ]
  "The first derivative of 𝒩."
  (* (/ (- x) (ⁿ σ 2)) (𝒩 x ￼￼μ σ)))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(defn bottom-extrusion [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom-extrusion 0.001 p)))

(defn pair-bottom-hulls [& shapes]
  (apply union
         (map (partial apply bottom-hull)
              (partition 2 1 shapes))))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn rotator-vector [[x y z]]
  "Create an anonymous rotation function for passed vector of three angles.
  This emulates OpenSCAD’s rotate(a=[...]). scad-clj’s ‘rotatev’ was unable to
  implement that form in version 0.4.0 of the module."""
  (fn [obj]
    (->> obj
      (rotate x [1 0 0])
      (rotate y [0 1 0])
      (rotate z [0 0 1]))))

(defn swing-callables [translator radius rotator obj]
  "Rotate passed object with passed radius, not around its own axes.

  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.

  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that."
  (if (vector? rotator)
    (if (= (count rotator) 3)
      (swing-callables translator radius (rotator-vector rotator) obj)
      (swing-callables translator radius (partial rotate (first rotator) (second rotator)) obj))
    ;; Else assume the rotator is usable as a function and apply it.
    (->> obj
      (translator [0 0 (- radius)])
      rotator
      (translator [0 0 radius]))))

(defn swing-radius [radius rotator obj]
  "A simplification that uses ‘translate’."
  (swing-callables translate radius rotator obj))

(def web-post
  "A shape for attaching things to a corner, e.g. a corner of a switch mount."
   (cube corner-post-width corner-post-width web-thickness))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Core Definitions — All Switches ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Mounts for neighbouring 1U keys are about 0.75” apart.
(def mount-1u 19.05)

;; Typical 1U keycap width and depth, approximate.
(def key-width-1u 18.25)
(def key-margin (/ (- mount-1u key-width-1u) 2))

;; Mount plates are a bit wider than typical keycaps.
(def mount-width 18.4)
(def mount-depth 18.4)

;; ALPS-style switches:
(def alps-width 15.5)
(def alps-depth 13.4)
(def alps-notch-height 1)
(def alps-height-below-notch 4.5)

;; Hardcode ALPS as our switch type.
(def keyswitch-depth alps-depth)
(def keyswitch-width alps-width)
(def keyswitch-cutout-height alps-height-below-notch)

;;;;;;;;;;;;;;;;;;;;;;
;; Matrix Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn coordinate-pairs
  ([columns rows] (for [column columns row rows] [column row]))
  ([columns rows selector] (filter selector (coordinate-pairs columns rows))))

(def compass-to-grid
  "Translation particles for each cardinal direction."
  (array-map
   :north {:dx 0,  :dy 1},
   :east  {:dx 1,  :dy 0},
   :south {:dx 0,  :dy -1},
   :west  {:dx -1, :dy 0}))

(def compass-radians
  {:north 0,
   :east  (/ π 2),
   :south π,
   :west  (/ π -2)})

(defn compass-delta [axis & directions]
  "Find a coordinate axis delta for movement in any of the stated directions."
  (let [value (get-in compass-to-grid [(first directions) axis])]
    (if (or (not (zero? value)) (= (count directions) 1))
      value
      (apply compass-delta axis (rest directions)))))

(defn compass-dx [& directions] (apply compass-delta :dx directions))
(defn compass-dy [& directions] (apply compass-delta :dy directions))

(defn turning-left [direction]
  "Retrieve a direction keyword for turning left from ‘direction’."
  (ffirst (filter #(= direction (second %))
                  (partition 2 1 '(:north) (keys compass-to-grid)))))

(defn turning-right [direction]
  (second (first (filter #(= direction (first %))
                         (partition 2 1 '(:north) (keys compass-to-grid))))))

(defn next-column [column direction]
  "Each column runs along the y axis; changing columns changes x."
  (+ column (compass-dx direction)))

(defn next-row [row direction]
  "Each row runs along the x axis; changing rows changes y."
  (+ row (compass-dy direction)))

(defn walk-matrix [[column row] & directions]
  "A tuple describing the key position an arbitrary orthogonal walk would lead to."
  (if (empty? directions)
    [column row]
    (let [direction (first directions)]
      (apply (partial walk-matrix [(next-column column direction) (next-row row direction)])
        (rest directions)))))

(defn general-corner [area-x area-y area-z neighbour-z directions]
  "Produce a translator for getting to one corner of a switch mount."
  [(* (apply compass-dx directions) (- (/ area-x 2) (/ corner-post-width 2)))
   (* (apply compass-dy directions) (- (/ area-y 2) (/ corner-post-width 2)))
   (+ (/ area-z -2) neighbour-z)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definitions — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def last-finger-column (apply max (keys rows-above-home)))
(def all-finger-columns (range 0 (+ last-finger-column 1)))
(def max-rows-above-home (apply max (vals rows-above-home)))  ; This doubles as the top row index.
(def max-rows-below-home (apply max (vals rows-below-home)))
(def all-finger-rows (range (- max-rows-below-home) (+ max-rows-above-home 1)))

(defn finger? [[column row]]
  "True if specified finger key has been requested."
  (cond
    (< column 0) false  ; Off grid.
    (> column last-finger-column) false  ; Off grid; lookups would cause null pointer.
    (= row 0) true  ;  Home row.
    (> row 0) (>= (get rows-above-home column row) row)
    (< row 0) (>= (get rows-below-home column row) (abs row))))

(defn finger-row-indices [column]
  "Return the range of row indices valid for passed column index."
  (range (- (get rows-below-home column rows-default))
         (+ (get rows-above-home column rows-default) 1)))
                                 ; range is exclusive ^

(defn first-in-column [column]
  [column (first (finger-row-indices column))])

(defn last-in-column [column]
  [column (last (finger-row-indices column))])

(defn finger-column-indices [row]
  "Return the range of column indices valid for passed row index."
  (filter #(finger? [% row]) all-finger-columns))

(def finger-key-coordinates
  (coordinate-pairs all-finger-columns all-finger-rows finger?))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definitions — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-thumb-columns [-1 0])
(def all-thumb-rows [-2 -1 0])

(def thumb-cluster-midpoint
  [(/ (+ (* 2 mount-width) thumb-mount-separation) 2)
   (/ (+ (* 3 mount-width) (* 2 thumb-mount-separation)) 2)
   0])

(defn thumb? [[column row]]
  "True if specified thumb key has been requested."
  (and (<= -1 column 0) (<= -2 row 0)))

;; Where to connect to finger cluster.
(def thumb-connection-column 1)

;;;;;;;;;;;;;;;;;;;
;; Keycap Models ;;
;;;;;;;;;;;;;;;;;;;

(defn negative-cap-shape [scale-top]
  "The shape of a channel for a keycap to move in.
  These are useful when keys are placed in such a way that the webbing between
  neighbouring mounts, or nearby walls, might otherwise obstruct movement."
  (let [base (+ (max keyswitch-width keyswitch-depth) 2)
        factor 1.05
        end (* factor key-width-1u)]
   (color [0.75 0.75 1 1]
    (translate [0 0 plate-thickness]
      (union
        ;; A base to accommodate the edges of a switch overhanging the hole:
        (extrude-linear
          {:height 1 :center false :scale (/ key-width-1u base)}
          (square base base))
        ;; Space for the keycap’s edges in travel:
        (translate [0 0 1]
          (extrude-linear
            {:height 5 :center false :scale factor}
            (square key-width-1u key-width-1u)))
        ;; Space for the upper body of a keycap at rest:
        (translate [0 0 6]
          (extrude-linear
            {:height 20 :center false :scale scale-top}
            (square end end))))))))

(def negative-cap-maximal (negative-cap-shape 2))
(def negative-cap-linear (negative-cap-shape 1))
(def negative-cap-minimal (negative-cap-shape 0.2))

(defn key-length [units] (- (* units mount-1u) (* 2 key-margin)))

;; Keycap form factor differences.
(def sa-profile-key-height 12.7)  ; Signature Plastics says 11.6 (home row).
(def dsa-profile-key-height 8.1)  ; Signature Plastics says 7.3.
(def dsa-profile-key-to-mount 5.8)

;; Implementation of keycap form choice.
(def keycap-height
  (case keycap-style
    :sa sa-profile-key-height
    :dsa dsa-profile-key-height))
(def keycap-to-mount dsa-profile-key-to-mount)
(def cap-bottom-height (+ plate-thickness keycap-to-mount))
(def cap-top-height (+ cap-bottom-height keycap-height))

(defn keycap [units]
  "The shape of one keycap, rectangular base, ’units’ in width, at rest."
  (let [base-width (key-length units)
        base-depth (key-length 1)
        vertical-scale 0.73  ; Approximately correct for DSA.
        z-offset (+ cap-bottom-height (/ keycap-height 2))]
   (->>
     (square base-width base-depth)
     (extrude-linear {:height keycap-height :scale vertical-scale})
     (translate [0 0 z-offset])
     (color [220/255 163/255 163/255 1]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — General ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def single-switch-plate
  (translate [0 0 (/ plate-thickness 2)]
    (cube mount-width mount-depth plate-thickness)))

(def single-switch-cutout
  "Negative space for the insertion of a key switch.
  A cube centered on a switch plate, with some overshoot for clean previews,
  and a further, more narrow dip for the legs of the switch."
  (let [h (- (* 2 keyswitch-cutout-height) plate-thickness)
        dip-factor 2.5]
   (translate [0 0 (/ plate-thickness 2)]
     (union
       (cube keyswitch-width keyswitch-depth h)
       (translate [0 0 (- h)]
         (extrude-linear
           {:height keyswitch-cutout-height :center false :scale dip-factor}
           (square (/ keyswitch-width dip-factor) (/ keyswitch-depth dip-factor))))))))

(defn mount-corner-offset [directions]
  "Produce a translator for getting to one corner of a switch mount."
  (general-corner
    mount-width mount-depth web-thickness plate-thickness directions))

(defn mount-corner-post [directions]
  "A post shape that comes offset for one corner of a key mount."
  (translate (mount-corner-offset directions) web-post))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn effective-α [column row] (get finger-column-tweak-α column α))

(defn row-radius [column row]
  (+ (/ (/ (+ mount-1u finger-mount-separation-y) 2)
           (Math/sin (/ (effective-α column row) 2)))
     cap-bottom-height))

(defn column-radius [column]
  (+ (/ (/ (+ mount-1u finger-mount-separation-x) 2)
           (Math/sin (/ β 2)))
     cap-bottom-height))

(defn column-x-delta [column]
  (+ -1 (- (* (column-radius column) (Math/sin β)))))

(defn finger-placement [translate-fn rotate-x-fn rotate-y-fn [column row] shape]
  "Place and tilt passed ‘shape’ as if it were a key."
  (let [column-curvature-offset (- curvature-centercol column)
        roll-angle (* β column-curvature-offset)
        curvature-centerrow (finger-column-curvature-centerrow column)
        pitch-angle (* (effective-α column row) (- row curvature-centerrow))
        pitch-radius (row-radius column row)
        column-z-delta (* (column-radius column) (- 1 (Math/cos roll-angle)))
        apply-default-style (fn [obj]
          (->> obj
            (swing-callables translate-fn pitch-radius (partial rotate-x-fn pitch-angle))
            (swing-callables translate-fn (column-radius column) (partial rotate-y-fn roll-angle))
            (translate-fn (finger-column-translation column))))
        apply-orthographic-style (fn [obj]
          (->> obj
            (swing-callables translate-fn pitch-radius (partial rotate-x-fn pitch-angle))
            (rotate-y-fn  roll-angle)
            (translate-fn [(- (* column-curvature-offset (column-x-delta column))) 0 column-z-delta])
            (translate-fn (finger-column-translation column))))
        apply-fixed-style (fn [obj]
          (->> obj
            (rotate-y-fn  (nth fixed-angles column))
            (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
            (swing-callables translate-fn (+ pitch-radius (nth fixed-z column)) (partial rotate-x-fn pitch-angle))
            (rotate-y-fn  fixed-tenting)
            (translate-fn [0 (second (finger-column-translation column)) 0])))
        applicator (case column-style
         :orthographic apply-orthographic-style
         :fixed        apply-fixed-style
         :standard     apply-default-style)
       ]
    (->> shape
         (translate-fn (get finger-tweak-early-translation [column row] [0 0 0]))
         (rotate-x-fn (get finger-intrinsic-pitch [column row] 0))
         applicator
         (rotate-x-fn pitch-centerrow)
         (rotate-y-fn tenting-angle)
         (translate-fn [0 (* mount-1u curvature-centerrow) keyboard-z-offset])
         (translate-fn (get finger-tweak-late-translation [column row] [0 0 0])))))

(defn finger-key-position [coordinates position]
  "Produce coordinates for passed matrix position with offset 'position'."
  (finger-placement (partial map +) rotate-around-x rotate-around-y coordinates position))

(defn finger-key-place [coordinates shape]
  "Put passed shape in specified matrix position.
  This resembles (translate (finger-key-position column row [0 0 0]) shape)), but
  performs the full transformation on the shape, not just the translation."
  (finger-placement
    translate (fn [angle obj] (rotate angle [1 0 0] obj)) (fn [angle obj] (rotate angle [0 1 0] obj)) coordinates shape))

(def finger-plates
  (apply union (map #(finger-key-place % single-switch-plate) finger-key-coordinates)))

(def finger-cutouts
  (apply union (map #(finger-key-place % single-switch-cutout) finger-key-coordinates)))

(def finger-key-channels
  (apply union (map #(finger-key-place % negative-cap-minimal) finger-key-coordinates)))

(def finger-keycaps
  (apply union (map #(finger-key-place % (keycap 1)) finger-key-coordinates)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def thumb-origin
  (map + (finger-key-position
           (first-in-column thumb-connection-column)
           [(/ mount-width -2) (/ mount-depth -2) 0])
         thumb-cluster-offset-from-fingers))

(defn thumb-key-place [[column row] shape]
  (let [offset (if (= -1 column) thumb-cluster-column-offset [0 0 0])]
    (->> shape
         ((rotator-vector (intrinsic-thumb-key-rotation [column row] [0 0 0])))
         (translate [(* column (+ mount-1u thumb-mount-separation))
                     (* row (+ mount-1u thumb-mount-separation))
                     0])
         (translate offset)
         (translate (get intrinsic-thumb-key-translation [column row] [0 0 0]))
         ((rotator-vector thumb-cluster-rotation))
         (translate thumb-origin)
         )))

(defn for-thumbs [shape]
  (apply union (for [column all-thumb-columns
                     row all-thumb-rows]
                 (thumb-key-place [column row] shape))))

(def thumb-plates (for-thumbs single-switch-plate))

(def thumb-cutouts (for-thumbs single-switch-cutout))

(def thumb-key-channels (for-thumbs negative-cap-minimal))

(def thumb-keycaps (for-thumbs (keycap 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Mount Connectors ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn web-shapes [coordinate-sequence spotter placer corner-finder]
  "A vector of shapes covering the interstices between points in a matrix."
  (loop [remaining-coordinates coordinate-sequence
         shapes []]
    (if (empty? remaining-coordinates)
      shapes
      (let [coord-here (first remaining-coordinates)
            coord-north (walk-matrix coord-here :north)
            coord-east (walk-matrix coord-here :east)
            coord-northeast (walk-matrix coord-here :north :east)
            fill-here (spotter coord-here)
            fill-north (spotter coord-north)
            fill-east (spotter coord-east)
            fill-northeast (spotter coord-northeast)]
      (recur
        (rest remaining-coordinates)
        (conj
          shapes
          ;; Connecting columns.
          (if (and fill-here fill-east)
            (hull
             (placer coord-here (corner-finder ENE))
             (placer coord-here (corner-finder ESE))
             (placer coord-east (corner-finder WNW))
             (placer coord-east (corner-finder WSW))))
          ;; Connecting rows.
          (if (and fill-here fill-north)
            (hull
             (placer coord-here (corner-finder WNW))
             (placer coord-here (corner-finder ENE))
             (placer coord-north (corner-finder WSW))
             (placer coord-north (corner-finder ESE))))
          ;; Selectively filling the area between all four possible mounts.
          (hull
            (if fill-here (placer coord-here (corner-finder ENE)))
            (if fill-north (placer coord-north (corner-finder ESE)))
            (if fill-east (placer coord-east (corner-finder WNW)))
            (if fill-northeast (placer coord-northeast (corner-finder WSW))))))))))

(defn walk-and-web [columns rows spotter placer corner-finder]
  (remove nil?
    (web-shapes (coordinate-pairs columns rows) spotter placer corner-finder)))

(def finger-connectors
  (apply union
    (walk-and-web all-finger-columns all-finger-rows finger? finger-key-place mount-corner-post)))

(def thumb-connectors
  (apply union
    (walk-and-web all-thumb-columns all-thumb-rows thumb? thumb-key-place mount-corner-post)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Case Walls — General ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wall-segment-offset [segment cardinal-direction [xy-offset z-offset]]
  (let [{dx :dx dy :dy} (cardinal-direction compass-to-grid)]
   (case segment
     0 [0 0 0]
     1 [(* dx wall-thickness)
        (* dy wall-thickness)
        (/ z-offset (abs z-offset))]
     2 [(* dx xy-offset)
        (* dy xy-offset)
        z-offset]
     3 [(* dx (+ xy-offset wall-thickness))
        (* dy (+ xy-offset wall-thickness))
        z-offset]
     4 [(* dx (+ xy-offset))
        (* dy (+ xy-offset))
        (+ z-offset (/ z-offset (abs z-offset)))]
        )))

(defn wall-element [segment [placer direction post offsets]]
  (placer (translate (wall-segment-offset segment direction offsets) post)))

(defn wall-skirt [point0 point1]
  "The portion of a wall that follows what it’s built around."
  (hull
    (wall-element 0 point0)
    (wall-element 1 point0)
    (wall-element 2 point0)
    (wall-element 3 point0)
    (wall-element 0 point1)
    (wall-element 1 point1)
    (wall-element 2 point1)
    (wall-element 3 point1)))

(defn wall-hem [point0 point1]
  "The vertical portion of a wall."
  (bottom-hull
    (wall-element 2 point0)
    (wall-element 3 point0)
    (wall-element 2 point1)
    (wall-element 3 point1)))

(defn wall-to-ground [point0 point1]
  (union
    (wall-skirt point0 point1)
    (wall-hem point0 point1)))

(defn finger-wall-corner-offset [coordinates directions]
  "Combined [x y z] offset from the center of a switch mount.
  This goes to one corner of the hem of the mount’s skirt of walling
  and is used mainly for finding the base of walls."
  (vec
    (map +
      (wall-segment-offset
        3 (first directions) (finger-key-wall-offsets coordinates directions))
      (mount-corner-offset directions))))

(defn finger-wall-corner-position [coordinates directions]
  "Absolute position of the lower wall around a finger key."
  (finger-key-position coordinates
    (finger-wall-corner-offset coordinates directions)))

(defn finger-wall-offset [coordinates direction]
  "Combined [x y z] offset to the center of a wall.
  Computed as the arithmetic average of its two corners."
  (letfn [(c [turn]
            (finger-wall-corner-offset coordinates [direction (turn direction)]))]
    (vec (map / (vec (map + (c turning-left) (c turning-right))) [2 2 2]))))

(defn key-wall-deref [placer offsetter post-finder [coordinates direction turn]]
  (let [corner [direction (turn direction)]]
   [(partial placer coordinates)
     direction
     (post-finder corner)
     (offsetter coordinates corner)]))

(defn key-wall-skirt-only [placer offsetter post-finder anchors]
  (apply wall-skirt (map (partial key-wall-deref placer offsetter post-finder) anchors)))

(defn key-wall-to-ground [placer offsetter post-finder anchors]
  (apply wall-to-ground (map (partial key-wall-deref placer offsetter post-finder) anchors)))

;; Functions for specifying parts of a perimeter wall. These all take the
;; edge-walking algorithm’s position and direction upon seeing the need for
;; each part.

(defn wall-straight-body [[coordinates direction]]
  "The part of a case wall that runs along the side of a key mount on and edge of the board."
  (let [facing (turning-left direction)]
    [[coordinates facing turning-right] [coordinates facing turning-left]]))

(defn wall-straight-join [[coordinates direction]]
  "The part of a case wall that runs between two key mounts in a straight line."
  (let [next-coord (walk-matrix coordinates direction)
        facing (turning-left direction)]
    [[coordinates facing turning-right] [next-coord facing turning-left]]))

(defn wall-outer-corner [[coordinates direction]]
  "The part of a case wall that smooths out an outer, sharp corner."
  (let [original-facing (turning-left direction)]
    [[coordinates original-facing turning-right] [coordinates direction turning-left]]))

(defn wall-inner-corner [[coordinates direction]]
  "The part of a case wall that covers any gap in an inner corner."
  (let [opposite (walk-matrix coordinates (turning-left direction) direction)
        reverse (turning-left (turning-left direction))]
    [[coordinates direction turning-left] [opposite reverse turning-left]]))

;; Edge walking.

(defn walk-and-wall [start stop occlusion-fn bracer]
  "Walk the edge of the populated key matrix clockwise. Wall it in.

  Return a vector of shapes.

  Stop when the passed terminus function meets the current position.
  Assume the matrix doesn’t have any holes in it.

  "
  (loop [place-and-direction start
         shapes []]
    (let [[coordinates direction] place-and-direction
          left (walk-matrix coordinates (turning-left direction))
          ahead (walk-matrix coordinates direction)
          ahead-left (walk-matrix coordinates direction (turning-left direction))
          landscape (vec (map occlusion-fn [left ahead-left ahead]))
          situation (case landscape
            [false false false] :outer-corner
            [false false true] :straight
            [false true true] :inner-corner
            (throw (Exception. (str "Unforeseen landscape at " place-and-direction ": " landscape))))]
      (if (and (= place-and-direction stop) (not (empty? shapes)))
        shapes
        (recur
          (case situation
            :outer-corner  ; Turn right in place.
              [coordinates (turning-right direction)]
            :straight
              [ahead direction]
            :inner-corner  ; Jump diagonally ahead-left while also turning left.
              [ahead-left (turning-left direction)])
          (conj
            shapes
            (bracer (wall-straight-body place-and-direction))
            (case situation
              :outer-corner
                (bracer (wall-outer-corner place-and-direction))
              :straight
                (bracer (wall-straight-join place-and-direction))
              :inner-corner
                (bracer (wall-inner-corner place-and-direction)))))))))

;; Some additional surfaces for rubber feet:

(def foot-plates
  "Model plates from polygons in ‘foot-plate-posts’.
  Each vector specific a point in a polygon must have finger key coordinates
  and a mount corner identified by a direction tuple. These cam be followed by
  a two-dimensional offset for tweaking."
  (letfn [(xy
            ([coordinates directions]
              (xy coordinates directions [0 0 0]))
            ([coordinates directions offset]
              (vec (map +
                (take 2 (finger-wall-corner-position coordinates directions))
                offset))))
          (plate [positions]
            (extrude-linear {:height foot-height :center false}
              (polygon (map (fn [spec] (apply xy spec)) positions))))]
   (apply union (map plate foot-plate-posts))))

;; Tweaks:

(def finger-case-tweaks
  "A collection of ugly workarounds for aesthetics."
  (letfn [(post [coordinates corner segment]
            (finger-key-place coordinates
              (translate
                (wall-segment-offset segment
                  (first corner) (finger-key-wall-offsets coordinates corner))
                (mount-corner-post corner))))
          (top [coordinates corner]
            (hull (post coordinates corner 0) (post coordinates corner 1)))
          (bottom [coordinates corner]
            (hull (post coordinates corner 2) (post coordinates corner 3)))]
   (union
     ;; The corners of finger key [4, 1] look strange because of the
     ;; irregular angle and placement of the key.
     (hull
       (top [4 1] ESE)
       (top [4 1] SSE)
       (bottom [4 1] ESE)
       (bottom [4 1] SSE))
     (bottom-hull
       (bottom [4 1] ESE)
       (bottom [4 1] SSE))
     (hull
       (top [4 1] SSE)
       (top [4 1] SSW)
       (top [4 1] WSW)
       (top [4 1] WNW)
       (top [4 1] NNW))
     ;; A tidy connection to the neighbouring key.
     (hull
       (top [4 1] NNW)
       (top [4 1] WNW)
       (top [3 2] NNE)
       (top [3 2] ENE)
       (bottom [3 2] ENE)
       (top [3 2] ESE)
       (bottom [3 2] ESE)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Case Walls — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def case-walls-for-the-fingers
  (apply union
    (walk-and-wall
      [(first-in-column 0) :north]
      [(first-in-column 2) :south]
      finger?
      (partial key-wall-to-ground
        finger-key-place finger-key-wall-offsets mount-corner-post))
    (walk-and-wall
      [(first-in-column 2) :south]
      [(first-in-column 2) :north]
      finger?
      (partial key-wall-skirt-only
        finger-key-place finger-key-wall-offsets mount-corner-post))
    (walk-and-wall
      [(first-in-column 1) :west]
      [(first-in-column 1) :north]
      finger?
      (partial key-wall-skirt-only
        finger-key-place finger-key-wall-offsets mount-corner-post))))

(def case-wrist-hook
  (let [[column row] (first-in-column last-finger-column)
        [x4 y2 _] (finger-key-position [column row] (mount-corner-offset ESE))
        x3 (- x4 2)
        x2 (- x3 6)
        x1 (- x2 2)
        x0 (- x1 0.6)
        y1 (- y2 6)
        y0 (- y1 1)]
    (extrude-linear
      {:height wrist-connector-height}
      ;; Draw the outline of the hook moving counterclockwise.
      (polygon [[x0 y1]  ; Left part of the point.
                [x1 y0]  ; Right part of the point.
                [x3 y0]  ; Rightmost contact with the connector.
                [x4 y2]  ; Rightmost contact with the case.
                [x2 y2]] ; Leftmost contact with the case.
                ))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Case Walls — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def case-walls-for-the-thumbs
  (apply union
    (walk-and-wall
      [[-1 0] :north]
      [[-1 -2] :north]
      thumb?
      (partial key-wall-skirt-only
        thumb-key-place thumb-key-wall-offsets mount-corner-post))))

(def key-cluster-bridge
  "Walling and webbing between the thumb cluster and the finger cluster.
  This makes strict assumptions about the selected keyboard layout and is
  difficult to parameterize."
  (letfn [(post [key directions segment]
            (let [[placer offsetter coordinates] key
                  offsets (offsetter coordinates directions)]
             (placer coordinates
               (translate
                 (wall-segment-offset segment (first directions) offsets)
                 (mount-corner-post directions)))))]
   (let [f0 [finger-key-place finger-key-wall-offsets (first-in-column 0)]
         f1 [finger-key-place finger-key-wall-offsets (first-in-column 1)]
         f2 [finger-key-place finger-key-wall-offsets (first-in-column 2)]
         t0 [thumb-key-place thumb-key-wall-offsets [-1 -2]]
         t1 [thumb-key-place thumb-key-wall-offsets [-1 -1]]
         t2 [thumb-key-place thumb-key-wall-offsets [-1 0]]
         t3 [thumb-key-place thumb-key-wall-offsets [0 0]]
         t4 [thumb-key-place thumb-key-wall-offsets [0 -1]]
         t5 [thumb-key-place thumb-key-wall-offsets [0 -2]]]
    (union
      (hull
        (post t0 WSW 3)
        (post t0 WSW 1)
        (post t0 WSW 0)
        (post t0 WNW 0))
      (triangle-hulls
        (post t0 WSW 3)
        (post f0 WSW 3)
        (post t0 WNW 0)
        (post f0 WSW 3)
        (post t1 WNW 0)
        (post f0 WSW 1)
        (post t2 WSW 0)
        (post f0 WSW 0))
      ;; A big chunk where t2 looms over f0:
      (hull
        (post t2 WSW 0)
        (post t2 WSW 2)
        (post t2 WSW 3)
        (post t2 WNW 0)
        (post t2 WNW 1)
        (post t2 WNW 2)
        (post t2 NNW 3)
        (post f0 WSW 0)
        (post f0 ESE 0))
      ;; Completion of the skirt around f1:
      (hull
        (post f1 WNW 0)
        (post f1 WSW 0)
        (post f1 WSW 1)
        (post f1 WSW 2)
        (post f1 WSW 3))
      ;; Filling the gap between the f1 skirt and the upper edge thumb skirt:
      (triangle-hulls
        (post f0 ESE 0)
        (post t2 NNW 3)
        (post f1 WNW 0)
        (post t2 NNE 3)
        (post f1 WSW 3)
        (post t3 NNW 3)
        (post f1 SSW 2)
        (post f1 SSW 3))
      ;; Filling the gap between the raised f1 skirt and f2 mount:
      (hull
        (post f1 SSE 0)
        (post f1 SSE 1)
        (post f1 SSE 2)
        (post f1 SSE 3)
        (post f2 WNW 0)
        (post f2 WSW 0)
        (post f2 WSW 1))
      ;; A tiny plate between the clusters:
      (hull
        (post f1 WSW 3)
        (post t2 NNE 3)
        (post t3 NNW 3))
      ;; A big top plate reaching down to the vertical part of the wall:
      (triangle-hulls
        (post f1 WSW 2)
        (post f1 ESE 2)
        (post t3 NNW 3)
        (hull
          (post f2 WSW 0)
          (post f2 WSW 1)
          (post f2 SSW 1))
        (post t3 NNE 3)
        (post f2 SSW 3)
        (hull
          (post t3 NNE 3)
          (post f2 WSW 4)
          (post t3 ENE 3)
          (post f2 SSW 4))
        (post t3 ENE 3)
        (post f2 NNW 4)
        (hull
          (post t5 NNE 2)
          (post t5 NNE 3)
          (post t5 ENE 2)
          (post t5 ENE 3)))
      ;; The back plate of the outermost corner of t5:
      (hull
        (post t5 ENE 3)
        (post t5 ENE 4)
        (post t5 ESE 3)
        (post t5 ESE 4)
        (post t5 SSE 3)
        (post t5 SSE 4)
        (post t5 SSW 3)
        (post t5 SSW 4))
      ;; The back plate of f2:
      (apply hull
        (for [segment [3 4]
              north-south [:north :south]
              east-west [:east :west]]
          (union
            (post f2 [north-south east-west] segment)
            (post f2 [east-west north-south] segment))))
      ;; Lower wall:
      (pair-bottom-hulls
        (hull
          (post f2 ENE 2)
          (post f2 ENE 3)
          (post f2 ENE 4))
        (hull
          (post f2 NNW 4))
        (hull
          (post t5 NNE 2)
          (post t5 NNE 3)
          (post t5 ENE 2)
          (post t5 ENE 3))
        (hull
          (post t5 WSW 2)
          (post t5 WSW 3))
        (hull
          (post t0 ESE 2)
          (post t0 ESE 3))
        (hull
          (post t0 WSW 2)
          (post t0 WSW 3))
        (hull
          (post f0 WSW 2)
          (post f0 WSW 3)))))))

(def key-cluster-bridge-cutouts
  (union
    (finger-key-place (first-in-column 1) negative-cap-maximal)
    (finger-key-place (first-in-column 2) negative-cap-maximal)))

;;;;;;;;;;;;;;;;
;; Wrist Rest ;;
;;;;;;;;;;;;;;;;

(defn case-south-wall-xy [[column corner]]
  "An [x y] coordinate pair at the south wall of the keyboard case."
  (take 2 (finger-wall-corner-position (first-in-column column) corner)))

(def wrist-connector-xy-west (case-south-wall-xy [wrist-connection-column SSE]))
(def wrist-connector-xy-east (case-south-wall-xy [last-finger-column SSE]))
(def wrist-plinth-xy-west (vec (map + wrist-connector-xy-west wrist-connection-offset)))
(def wrist-plinth-xy-east [(+ (first wrist-plinth-xy-west) wrist-plinth-width)
                           (second wrist-plinth-xy-west)])
(def wrist-grid-unit-x 4)
(def wrist-grid-unit-y wrist-plinth-length)
(def wrist-node-size 2)
(def wrist-wall-z-offset -1)
(defn wrist-wall-offsetter [coordinates corner] [0 wrist-wall-z-offset])

(def last-wrist-column (int (/ wrist-plinth-width wrist-grid-unit-x)))
(def last-wrist-row (int (/ wrist-plinth-length wrist-grid-unit-y)))
(def all-wrist-columns (range 0 (+ last-wrist-column 1)))
(def all-wrist-rows (range 0 (+ last-wrist-row 1)))

(defn wrist? [[column row]]
  "True if specified node in wrist rest surface has been requested."
  (and (<= 0 column last-wrist-column) (<= 0 row last-wrist-row)))

(def wrist-node
  (let [h (+ (abs wrist-wall-z-offset) plate-thickness)
        dz (- (- (/ h 2) plate-thickness))]
    (translate [0 0 dz] (cube wrist-node-size wrist-node-size h))))

(defn node-corner-offset [directions]
  "Produce a translator for getting to one corner of a wrist rest node."
  (general-corner
    wrist-node-size wrist-node-size web-thickness plate-thickness directions))

(defn node-corner-post [directions]
  "A post shape that comes offset for one corner of a wrist rest node."
  (translate (node-corner-offset directions) web-post))

(def wrist-connector
  (let [bevel 10
        p0 (case-south-wall-xy [(- wrist-connection-column 1) SSE])]
   (extrude-linear
     {:height wrist-connector-height}
     (polygon
       (concat
         [p0]
         (map case-south-wall-xy
           (for [column (filter (partial <= wrist-connection-column) all-finger-columns)
                 corner [SSW SSE]]
             [column corner]))
         [[(first wrist-connector-xy-east) (second wrist-plinth-xy-west)]
          wrist-plinth-xy-west
          [(first wrist-plinth-xy-west) (- (second p0) bevel)]
          [(- (first wrist-plinth-xy-west) bevel) (second p0)]]
    )))))

(defn wrist-node-place [[column row] shape]
  (let [μ 0
        M (- column (* 2 (/ last-wrist-column 3)))  ; Placement of curvature.
        σ wrist-rest-σ
        θ wrist-rest-θ
        z (* wrist-z-coefficient θ)
        ]
  (->> shape
       ((rotator-vector [0 (* θ (𝒩′ M μ σ)) 0]))
       (translate [0 0 (- (* z (𝒩 M μ σ)))])
       (translate [(* column wrist-grid-unit-x) (* row wrist-grid-unit-y) 0])
       (translate [(first wrist-plinth-xy-west)
                   (- (second wrist-plinth-xy-west) wrist-plinth-length)
                   wrist-plinth-height])
       )))

(def wrist-nodes
  (apply union
    (map #(bottom-hull (wrist-node-place % wrist-node))
      (coordinate-pairs all-wrist-columns all-wrist-rows))))

(def wrist-surface
  (map bottom-hull
    (walk-and-web all-wrist-columns all-wrist-rows wrist? wrist-node-place node-corner-post)))

(def wrist-plinth
  (apply union
    (map bottom-hull
      (walk-and-wall
        [[0 0] :north]
        [[0 0] :north]
        (fn [[column row]] (and (<= 0 column last-wrist-column) (<= 0 row last-wrist-row)))
        (partial key-wall-to-ground wrist-node-place wrist-wall-offsetter node-corner-post)))))

(def wrist-rest-model
  (union
    wrist-connector
    #_wrist-nodes  ; Visualization.
    (color [1 1 1 1] wrist-surface)
    wrist-plinth))

;;;;;;;;;;;;;;;;;;;;
;; Minor Features ;;
;;;;;;;;;;;;;;;;;;;;

;; Plate for a connecting beam, rod etc.:
(defn backplate-place [shape]
  (let [coordinates (last-in-column backplate-column)
        position (finger-key-position coordinates (finger-wall-offset coordinates :north))]
   (->>
     shape
     (rotate installation-angle [0 0 1])
     (translate position)
     (translate [0 0 (/ backplate-beam-height -2)])
     (translate backplate-offset))))

(def backplate-shape
  "A mounting plate for a connecting beam."
  (let [height backplate-beam-height
        width (+ backplate-fastener-distance height)
        depth 4
        interior-protrusion 8
        exterior-bevel 1
        interior-bevel 7]
   (hull
     (translate [0 (- interior-protrusion) 0]
       (cube (- width interior-bevel) depth (- height interior-bevel)))
     (cube width depth height)
     (translate [0 exterior-bevel 0]
       (cube (dec width) depth (dec height))))))

(def backplate-fastener-holes
  "Two holes for screws through the back plate."
  (letfn [(hole [x-offset]
            (->>
              (cylinder (/ backplate-fastener-diameter 2) 25)
              (rotate (/ π 2) [1 0 0])
              (translate [x-offset 0 0])
              backplate-place))]
   (union
     (hole (/ backplate-fastener-distance 2))
     (hole (/ backplate-fastener-distance -2)))))

(def backplate-block
  (bottom-hull (backplate-place backplate-shape)))

;; 4P4C connector holder:
(def rj9-origin
  (let [c0 [0 (last (finger-row-indices 0))]
        c1 [1 (last (finger-row-indices 1))]
        corner (fn [c] (finger-wall-corner-position c NNW))
        [x0 y0] (take 2 (map + (corner c0) (corner c1)))]
   (map + [0 0 0] [(/ x0 2) (/ y0 2) 0])))
(defn rj9-position [shape]
  (->> shape
       (translate rj9-translation)
       (rotate (deg2rad 36) [0 0 1])
       (translate [(first rj9-origin) (second rj9-origin) 10.5])))
(def rj9-metasocket
  (hull
    (translate [0 1 18] (cube 6 4 1))
    (cube 13.8 12 21)))
(def rj9-socket-tshort (union (translate [0 2 0] (cube 10.78  9 18.38))
                              (translate [0 0 5] (cube 10.78 13  5))))
(def rj9-socket-616e
  "The shape of a 4P4C female connector for use as a negative.
  An actual 616E socket is not symmetric along the x axis. This model of it,
  being intended for mirroring, is deliberately imprecise. It includes a
  channel for the 4 wires entering the case and excludes the vertical bar."
  (translate [0 1 0]
    (union
     (cube 10 11 17.7)
     (translate [0 0 -5] (cube 8 20 7.7)))))
(def rj9-space  (rj9-position rj9-metasocket))
(def rj9-holder (rj9-position (difference rj9-metasocket
                                          rj9-socket-616e)))

;; USB female holder:
;; This is not needed if the MCU has an integrated USB connector and that
;; connector is directly exposed through the case.
(def usb-holder-position
  (let [coordinates [0 0]]
   (finger-key-position coordinates
     (map +
       (wall-segment-offset 2 :north (finger-key-wall-offsets coordinates WNW))
       [0 (/ mount-depth 2) 0]))))
(def usb-holder-size [6.5 10.0 13.6])
(def usb-holder-thickness 4)
(def usb-holder
    (->> (cube (+ (first usb-holder-size) usb-holder-thickness) (second usb-holder-size) (+ (last usb-holder-size) usb-holder-thickness))
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))
(def usb-holder-hole
    (->> (apply cube usb-holder-size)
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))

;; LED strip:
(def led-height (+ (/ led-housing-size 2) 5))
(def west-wall-west-points
  (for [row (finger-row-indices 0)
        corner [WSW WNW]]
   (let [[x y _] (finger-wall-corner-position [0 row] corner)]
    [(+ x wall-thickness) y])))
(def west-wall-east-points
  (map (fn [[x y]] [(+ x 10) y]) west-wall-west-points))
(def west-wall-led-channel
  (extrude-linear {:height 50}
    (polygon (concat west-wall-west-points (reverse west-wall-east-points)))))
(defn led-hole-position [ordinal]
  (let [row (first (finger-row-indices 0))
        [x0 y0 _] (finger-wall-corner-position [0 row] WNW)]
   [x0 (+ y0 (* led-pitch ordinal)) led-height]))
(defn led-emitter-channel [ordinal]
  (->> (cylinder (/ led-emitter-diameter 2) 50)
       (rotatev (/ π 2) [0 1 0])
       (translate (led-hole-position ordinal))))
(defn led-housing-channel [ordinal]
  (->> (cube 50 led-housing-size led-housing-size)
       (translate (led-hole-position ordinal))))
(def led-holes
  (let [holes (range led-amount)]
   (union
     (intersection
       west-wall-led-channel
       (apply union (map led-housing-channel holes)))
     (apply union (map led-emitter-channel holes)))))

;;;;;;;;;;;;;;;;;;;;;
;; Microcontroller ;;
;;;;;;;;;;;;;;;;;;;;;

;; MicroUSB female:
(def micro-usb-width 7.5)
(def micro-usb-length 5.3)
(def micro-usb-height 2.8)

(def micro-usb-receptacle
  "A USB female."
  (color [0.5 0.5 0.5 1]
    (cube micro-usb-width micro-usb-length micro-usb-height)))
(def micro-usb-channel (cube 7.8 10 2.8))

;; Teensy MCU: Not fully supported at the moment. Pro Micro is hardcoded below.
(def teensy-width 20)
(def teensy-height 12)
(def teensy-length 33)
(def teensy2-length 53)

;; Arduino Pro Micro MCU:
(def promicro-width 18)
(def promicro-length 33)
(def promicro-thickness 1.5)  ; Slightly exaggerated.

(def mcu-microusb-offset
  "A millimetre offset between an MCU PCB and a micro-USB female."
  [0
   (+ (/ promicro-length 2) 1 (/ micro-usb-length -2))
   (+ (/ promicro-thickness 2) (/ micro-usb-height 2))])

(def promicro-pcb (cube promicro-width promicro-length promicro-thickness))
(def mcu-height-above-ground 2)
(def mcu-model
  (union
    (translate mcu-microusb-offset micro-usb-receptacle)
    (color [26/255, 90/255, 160/255 1] promicro-pcb)))

(def mcu-space-requirements
  "Negative space for an MCU in use, including USB connectors."
  (let [alcove 10]
    (union
      (translate mcu-microusb-offset
        (union
          ;; Female USB connector:
          micro-usb-channel
          ;; Male USB connector:
          (hull
            (translate [0 4 0] (cube 15 1 10))
            (translate [0 9 0] (cube 20 1 15)))))
      ;; An alcove in the inner wall, because a blind notch is hard to clean:
      (translate [0 (/ (- promicro-length alcove) 2) 0]
        (cube (+ promicro-width 5) alcove (+ promicro-thickness (* 2 micro-usb-height))))
      ;; The negative of the PCB, just to put a notch in the spine:
      promicro-pcb)))

(def mcu-finger-coordinates (last-in-column mcu-finger-column))
(defn mcu-position [shape]
  "Transform passed shape into the reference frame for an MCU holder."
  (let [[x y] (take 2
                (finger-key-position
                  mcu-finger-coordinates
                  (finger-wall-offset mcu-finger-coordinates mcu-connector-direction)))]
   (->>
     shape
     ;; Put the USB end of the PCB at [0, 0].
     (translate [0 (/ promicro-length -2) 0])
     ;; Flip it to stand on the long edge for soldering access.
     (rotate (/ π -2) [0 1 0])
     ;; Lift it to ground level.
     (translate [0 0 (/ promicro-width 2)])
     ;; Lift it a little further, to clear a support structure.
     (translate [0 0 mcu-height-above-ground])
     ;; Turn it around the z axis to point USB in the ordered direction.
     (rotate (- (compass-radians mcu-connector-direction)) [0 0 1])
     ;; Move it to the ordered case wall.
     (translate [x y 0]))))

(def mcu-visualization (mcu-position mcu-model))
(def mcu-negative (mcu-position mcu-space-requirements))

;; Holder for MCU:
(def mcu-support
  (let [plinth-width 4
        plinth-height mcu-height-above-ground
        rev-dir (turning-left (turning-left mcu-connector-direction))
        cervix-coordinates (walk-matrix mcu-finger-coordinates rev-dir rev-dir)]
    (union
      (mcu-position
        (union
          ;; A support beneath the end of the PCB.
          (translate
            [(- (/ promicro-width -2) (/ plinth-height 2)) (/ promicro-length -2) 0]
            (cube plinth-height 3 plinth-width))
          ;; A little gripper stabilize the PCB horizontally.
          ;; This is intended to be just shallow enough that the outer wall
          ;; will bend back far enough for the installation and is placed to
          ;; avoid covering any of the through-holes.
          (translate
            [0 (/ promicro-length -2) 0]
            (cube (/ promicro-width 2) 2 plinth-width))))
      ;; The spine connects a sacrum, which is the main body of the plinth
      ;; at ground level, with a cervix that helps support the finger web.
      (hull
        (mcu-position
          (translate [(+ (/ promicro-width -3) 3) (- (/ promicro-width -2) 12) 0]
            (cube 16 9 plinth-width)))
        (finger-key-place cervix-coordinates
          (mount-corner-post [mcu-connector-direction (turning-left rev-dir)]))
        (finger-key-place cervix-coordinates
          (mount-corner-post [mcu-connector-direction (turning-right rev-dir)]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Final Composition and Output ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn print-finger-matrix []
  "Print a picture of the finger layout. No thumb keys."
  (for [row (reverse all-finger-rows)
        column all-finger-columns]
    (do (if (finger? [column row]) (print "□") (print "·"))
        (if (= column last-finger-column) (println)))))

(def wrist-rest-right
  (intersection
    (difference
      wrist-rest-model
      (union
        case-walls-for-the-fingers
        case-wrist-hook))
     (translate [0 0 50] (cube 500 500 100))))

(def keyboard-right
  (union
    (difference
      (union
        (difference case-walls-for-the-fingers rj9-space)
        case-wrist-hook
        case-walls-for-the-thumbs
        key-cluster-bridge
        finger-case-tweaks
        mcu-support
        finger-plates
        finger-connectors
        thumb-plates
        thumb-connectors
        rj9-holder
        (if include-feet foot-plates)
        (if include-backplate backplate-block))
      key-cluster-bridge-cutouts
      mcu-negative
      finger-cutouts
      finger-key-channels
      thumb-cutouts
      thumb-key-channels
      (if include-led-housings led-holes)
      (if include-backplate backplate-fastener-holes)
      (translate [0 0 -20] (cube 500 500 40)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    #_wrist-rest-right
    #_mcu-visualization
    #_finger-keycaps
    #_thumb-keycaps))

(spit "things/right-hand.scad"
      (write-scad keyboard-right))

(spit "things/right-wrist.scad"
      (write-scad wrist-rest-right))

(spit "things/left-hand.scad"
      (write-scad (mirror [-1 0 0] keyboard-right)))

(spit "things/left-wrist.scad"
      (write-scad (mirror [-1 0 0] wrist-rest-right)))

(defn -main [dum] 1)  ; Dummy to make it easier to batch.
