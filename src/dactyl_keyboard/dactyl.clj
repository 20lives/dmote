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

;;;;;;;;;;;;;;;;;;;;;;
;; Shape Parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

;; The shape of the finger key cluster is defined by the number of rows above
;; and below the home row in each column.
(def rows-above-home {0 1, 1 2, 2 2, 3 2, 4 1, 5 1})
(def rows-below-home {0 1, 1 2, 2 3, 3 2, 4 2, 5 2})
(def rows-default 1)  ; Default number of rows for columns omitted above.

;; The tenting angle controls overall left-to-right tilt.
(def tenting-angle (/ π 20/3))

;; keyboard-z-offset controls the overall height of the finger cluster and with
;; it, the entire keyboard.
(def keyboard-z-offset 20)

;; Finger switch mounts may need more or less spacing depending on the size
;; of your keycaps.
(def finger-mount-separation-x 0.8)
(def finger-mount-separation-y -0.5)

;; Finger key placement parameters:
(def column-style :standard)  ; :standard, :orthographic, or :fixed.
;; α is the default progressive Tait-Bryan pitch of each finger row.
;; α therefore controls the front-to-back curvature of the keyboard.
(def α (/ π 9.2))
;; β is the default progressive Tait-Bryan roll of each finger column.
;; β therefore controls the side-to-side curvature of the keyboard.
(def β (/ π 50))

;; Individual columns may have a non-standard curvature.
(def finger-column-tweak-α {2 (/ π 8.2)})
(def pitch-centerrow (/ π 12))
(def curvature-centercol 3)   ; Column where the effect of β will be zero.
(def curvature-centerrow 0)   ; Row where the effect of α will be zero.

;; Individual columns may be translated (offset).
(defn finger-column-translation [column]
  (cond
    (= column 2) [0 4 -4.5]
    (>= column 4) [0 3 5]
    :else [0 0 0]))

;; Individual switches may be finely adjusted, including intrinsic rotation.
;; These are maps of column-row pairs to operator values.
(def finger-intrinsic-pitch {[2 -3] (/ π -8)
                             [5 1] (/ π -2.5)})
(def finger-tweak-translation {[2 -3] [1 0 2]
                               [5 1] [0 0 -10]})

;; Thumb key placement is similar to finger key placement:
(def thumb-cluster-offset-from-fingers [2 -4 -11])
(def thumb-cluster-column-offset [0 0 2])
(def thumb-cluster-rotation [(/ π 3) 0 (/ π -3)])
(def intrinsic-thumb-key-rotation
   {[0 0] [0 (/ π 15) 0]
    [0 -1] [0 (/ π -15) 0]
    [0 -2] [0 (/ π -15) 0]
    [-1 0] [0 (/ π -15) 0]
    [-1 -1] [0 (/ π 15) 0]
    [-1 -2] [0 (/ π 15) 0]})
(def intrinsic-thumb-key-translation
   {[-1 0] [0 0 3]
    [-1 -1] [-3 0 0]
    [-1 -2] [-3 0 0]
    [0 0] [0 0 3]
    [0 -1] [3 0 0]
    [0 -2] [3 0 0]})
(def thumb-mount-separation 1)

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
;; Both wall-z-offset and wall-xy-offset are in the mount’s frame of reference,
;; not in the absolute coordinate system.
(def wall-z-offset -10)
(def wall-xy-offset 0)
;; Ultimately, from a set of posts placed by the offsets and the wall-thickness
;; parameter, the wall drops down to the floor. The actual thickness of the
;; wall at that point is a function of post size and the angle of the nearest
;; switch mount, as well as the thickness parameter itself.
(def wall-thickness 1)

;; Settings for column-style :fixed.
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column offsets above.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; Relative to middle finger.
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;; Wrist rest shape:
(def wrist-rest-σ 2.5)       ; Softness of curvature.
(def wrist-rest-θ 12)        ; Surface angle coefficient.
(def wrist-z-coefficient 6)  ; Relationship of wrist-rest-θ to height.
(def wrist-connector-height 13)

;; Minor features:
(def mcu-finger-column 2)
(def rj9-translation [-3 -8 0])

;; LED holes along the inner wall. Defaults are for WS2818 at 17 mm intervals.
(def led-housing-size 5)
(def led-emitter-diameter 4)
(def led-pitch 17)
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

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

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
  implement that form in version 0.4.0 of the module.

  """
  (fn [obj]
    (->> obj
      (rotate x [1 0 0])
      (rotate y [0 1 0])
      (rotate z [0 0 1]))))

(defn swing-callables [translator radius rotator obj]
  "Rotate passed object with passed radius, instead of around its own axes.

  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.

  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that.

  "
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

;; ALPS-style switches:
(def alps-width 15.5)
(def alps-notch-height 1)
(def alps-depth 13.4)
(def alps-height-below-notch 4.5)

;; Hardcode ALPS as our switch type.
(def keyswitch-depth alps-depth)
(def keyswitch-width alps-width)
(def keyswitch-cutout-height alps-height-below-notch)

;; Mount plates are a bit wider than typical keycaps.
(def mount-width 18.4)
(def mount-height 18.4)
(def key-width-1u 18.25)

;;;;;;;;;;;;;;;;;;;;;;
;; Matrix Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn coordinate-pairs
  ([columns rows] (for [column columns row rows] [column row]))
  ([columns rows selector] (filter selector (coordinate-pairs columns rows))))

(def compass
  "Translation particles for each cardinal direction."
  (array-map
   :north {:dx 0,  :dy 1},
   :east  {:dx 1,  :dy 0},
   :south {:dx 0,  :dy -1},
   :west  {:dx -1, :dy 0}))

(defn compass-delta [axis & directions]
  "Find a coordinate axis delta for movement in any of the stated directions."
  (let [value (get-in compass [(first directions) axis])]
    (if (or (not (zero? value)) (= (count directions) 1))
      value
      (apply compass-delta axis (rest directions)))))

(defn compass-dx [& directions] (apply compass-delta :dx directions))
(defn compass-dy [& directions] (apply compass-delta :dy directions))

(defn turning-left [direction]
  "Retrieve a direction keyword for turning left from ‘direction’."
  (ffirst (filter #(= direction (second %))
                  (partition 2 1 '(:north) (keys compass)))))

(defn turning-right [direction]
  (second (first (filter #(= direction (first %))
                         (partition 2 1 '(:north) (keys compass))))))

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

(def negative-cap
  "The shape of a channel for a keycap to move in.

  These are useful when keys are placed in such a way that the webbing between
  neighbouring mounts, or nearby walls, might otherwise obstruct movement.

  "
  (let [base (+ (max keyswitch-width keyswitch-depth) 2)
        end (* 1.2 key-width-1u)]
  (color [0 0 1 1]
    (translate [0 0 plate-thickness]
      (union
        (extrude-linear
          {:height 1 :center false :scale (/ key-width-1u base)}
          (square base base))
        (translate [0 0 1]
          (extrude-linear
            {:height 5 :center false :scale 1.2}
            (square key-width-1u key-width-1u)))
        (translate [0 0 6]
          (extrude-linear
            {:height 20 :center false :scale 1}
            (square end end))))))))

;; SA is a keycap form factor.
(def sa-profile-key-height 12.7)
(def sa-double-length 37.5)
(def sa-cap
   {1 (let [bl2 (/ key-width-1u 2)
            m (/ 17 2)
            key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                               (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                               (translate [0 0 0.05]))
                          (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                               (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                               (translate [0 0 6]))
                          (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                               (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                               (translate [0 0 12])))]
           (->> key-cap
            (translate [0 0 (+ 5 plate-thickness)])
            (color [220/255 163/255 163/255 1])))
    2 (let [bl2 (/ sa-double-length 2)
            bw2 (/ 18.25 2)
            key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                               (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                               (translate [0 0 0.05]))
                          (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                               (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                               (translate [0 0 12])))]
        (->> key-cap
             (translate [0 0 (+ 5 plate-thickness)])
             (color [127/255 159/255 127/255 1])))
    1.5 (let [bl2 (/ 18.25 2)
              bw2 (/ 28 2)
              key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                 (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                 (translate [0 0 0.05]))
                            (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                 (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                 (translate [0 0 12])))]
          (->> key-cap
               (translate [0 0 (+ 5 plate-thickness)])
               (color [240/255 223/255 175/255 1])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — General ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def single-plate
  (translate [0 0 (/ plate-thickness 2)]
    (cube mount-width mount-height plate-thickness)))

(def single-switch-cutout
  "Negative space for the insertion of a key.

  A cube centered on a switch plate, with some overshoot for clean previews.

  "
  (translate [0 0 (/ plate-thickness 2)]
    (cube keyswitch-width
          keyswitch-depth
          (- (* 2 keyswitch-cutout-height) plate-thickness))))

(defn mount-corner-offset [directions]
  "Produce a translator for getting to one corner of a switch mount."
  (general-corner
    mount-width mount-height web-thickness plate-thickness directions))

(defn mount-corner-post [directions]
  "A post shape that comes offset for one corner of a key mount."
  (translate (mount-corner-offset directions) web-post))

;; Convenient special cases of mount-corner-offset for hardcoded tweaks.
(def mount-north-east (mount-corner-offset [:north :east]))
(def mount-north-west (mount-corner-offset [:north :west]))
(def mount-south-west (mount-corner-offset [:south :west]))
(def mount-south-east (mount-corner-offset [:south :east]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cap-top-height (+ plate-thickness sa-profile-key-height))

(defn effective-α [column row] (get finger-column-tweak-α column α))

(defn row-radius [column row]
  (+ (/ (/ (+ mount-height finger-mount-separation-y) 2)
           (Math/sin (/ (effective-α column row) 2)))
     cap-top-height))

(defn column-radius [column]
  (+ (/ (/ (+ mount-width finger-mount-separation-x) 2)
           (Math/sin (/ β 2)))
     cap-top-height))

(defn column-x-delta [column]
  (+ -1 (- (* (column-radius column) (Math/sin β)))))

(defn finger-placement [translate-fn rotate-x-fn rotate-y-fn [column row] shape]
  "Place and tilt passed ‘shape’ as if it were a key.

  Excuse the nested macros. Basically, the order of operations is:

  1. Intrinsic key-specific rotation.
  2. Global style choice.
  3. Global operations not subordinate to style.
  4. Individual translation.

  "
  (let [column-curvature-offset (- curvature-centercol column)
        roll-angle (* β column-curvature-offset)
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
         (rotate-x-fn (get finger-intrinsic-pitch [column row] 0))
         applicator
         (rotate-x-fn pitch-centerrow)
         (rotate-y-fn tenting-angle)
         (translate-fn [0 0 keyboard-z-offset])
         (translate-fn (get finger-tweak-translation [column row] [0 0 0])))))

(defn finger-key-position [coordinates position]
  "Produce coordinates for passed matrix position with offset 'position'."
  (finger-placement (partial map +) rotate-around-x rotate-around-y coordinates position))

(defn finger-key-place [coordinates shape]
  "Put passed shape in specified matrix position.

  This resembles (translate (finger-key-position column row [0 0 0]) shape)), but
  performs the full transformation on the shape, not just the translation.

  "
  (finger-placement
    translate (fn [angle obj] (rotate angle [1 0 0] obj)) (fn [angle obj] (rotate angle [0 1 0] obj)) coordinates shape))

(def finger-plates
  (apply union (map #(finger-key-place % single-plate) finger-key-coordinates)))

(def finger-cutouts
  (apply union (map #(finger-key-place % single-switch-cutout) finger-key-coordinates)))

(def finger-key-channels
  (apply union (map #(finger-key-place % negative-cap) finger-key-coordinates)))

(def finger-keycaps
  (apply union (map #(finger-key-place % (sa-cap 1)) finger-key-coordinates)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def thumb-origin
  (map + (finger-key-position
           [thumb-connection-column (first (finger-row-indices thumb-connection-column))]
           [(/ mount-width -2) (/ mount-height -2) 0])
         thumb-cluster-offset-from-fingers))

(defn thumb-key-place [[column row] shape]
  (let [offset (if (= -1 column) thumb-cluster-column-offset [0 0 0])]
    (->> shape
         ((rotator-vector (intrinsic-thumb-key-rotation [column row] [0 0 0])))
         (translate [(* column (+ mount-width thumb-mount-separation))
                     (* row (+ mount-height thumb-mount-separation))
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

(def thumb-plates (for-thumbs single-plate))

(def thumb-cutouts (for-thumbs single-switch-cutout))

(def thumb-key-channels (for-thumbs negative-cap))

(def thumb-keycaps (for-thumbs (sa-cap 1)))

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
             (placer coord-here (corner-finder [:north :east]))
             (placer coord-here (corner-finder [:south :east]))
             (placer coord-east (corner-finder [:north :west]))
             (placer coord-east (corner-finder [:south :west]))))
          ;; Connecting rows.
          (if (and fill-here fill-north)
            (hull
             (placer coord-here (corner-finder [:north :west]))
             (placer coord-here (corner-finder [:north :east]))
             (placer coord-north (corner-finder [:south :west]))
             (placer coord-north (corner-finder [:south :east]))))
          ;; Selectively filling the area between all four possible mounts.
          (hull
            (if fill-here (placer coord-here (corner-finder [:north :east])))
            (if fill-north (placer coord-north (corner-finder [:south :east])))
            (if fill-east (placer coord-east (corner-finder [:north :west])))
            (if fill-northeast (placer coord-northeast (corner-finder [:south :west]))))))))))

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

(defn wall-segment-offset [segment direction]
  (let [{dx :dx dy :dy} (direction compass)]
   (case segment
     1 [(* dx wall-thickness)
        (* dy wall-thickness)
        -1]
     2 [(* dx wall-xy-offset)
        (* dy wall-xy-offset)
        wall-z-offset]
     3 [(* dx (+ wall-xy-offset wall-thickness))
        (* dy (+ wall-xy-offset wall-thickness))
        wall-z-offset])))

(defn wall-brace [[placer0 direction0 post0] [placer1 direction1 post1]]
  (union
    (hull
      (placer0 post0)
      (placer0 (translate (wall-segment-offset 1 direction0) post0))
      (placer0 (translate (wall-segment-offset 2 direction0) post0))
      (placer0 (translate (wall-segment-offset 3 direction0) post0))
      (placer1 post1)
      (placer1 (translate (wall-segment-offset 1 direction1) post1))
      (placer1 (translate (wall-segment-offset 2 direction1) post1))
      (placer1 (translate (wall-segment-offset 3 direction1) post1)))
    (bottom-hull
      (placer0 (translate (wall-segment-offset 2 direction0) post0))
      (placer0 (translate (wall-segment-offset 3 direction0) post0))
      (placer1 (translate (wall-segment-offset 2 direction1) post1))
      (placer1 (translate (wall-segment-offset 3 direction1) post1)))
      ))

(defn wall-corner-offset [direction corner]
  "Combined [x y z] offset from the center of a switch mount to one corner of the hem of its skirt."
  (vec (map + (wall-segment-offset 3 direction) corner)))

(defn key-wall-deref [placer post-finder [coordinates direction turn]]
  [(partial placer coordinates)
   direction
   (post-finder [direction (turn direction)])])

(defn key-wall-brace [placer post-finder anchors]
  (apply wall-brace (map (partial key-wall-deref placer post-finder) anchors)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Case Walls — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def case-walls-for-the-fingers
  (apply union
     (walk-and-wall
       [[0 (first (finger-row-indices 0))] :north]
       [[2 (first (finger-row-indices 2))] :north]
       finger?
       (partial key-wall-brace finger-key-place mount-corner-post))))

(def case-wrist-hook
  (let [column last-finger-column
        [x2 y1] (take 2
                 (finger-key-position
                   [column (first (finger-row-indices column))]
                   (wall-corner-offset :south mount-south-east)))
        x1 (- x2 10)
        x0 (- x1 5)
        y0 (- y1 8)]
    (extrude-linear
      {:height wrist-connector-height}
      (polygon [[x0 y0] [x1 y1] [x2 y1] [x2 y0]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Case Walls — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def case-walls-for-the-thumbs
  (apply union
     (walk-and-wall
       [[0 -1] :south]
       [[-1 -1] :north]
       thumb?
       (partial key-wall-brace thumb-key-place mount-corner-post))))

(def key-cluster-bridge
  "Walling and webbing between the thumb cluster and the finger cluster.

  This makes strict assumptions about the selected keyboard layout and is
  difficult to parameterize.

  "
  (let [post mount-corner-post
        wall (fn [segment directions]
               (translate
                 (wall-segment-offset segment (second directions))
                 (mount-corner-post directions)))
        f0 (partial finger-key-place [0 (first (finger-row-indices 0))])
        f1 (partial finger-key-place [1 (first (finger-row-indices 1))])
        f2 (partial finger-key-place [2 (first (finger-row-indices 2))])
        t0 (partial thumb-key-place [-1 -2])
        t1 (partial thumb-key-place [-1 -1])
        t2 (partial thumb-key-place [-1 0])
        t3 (partial thumb-key-place [0 0])
        t4 (partial thumb-key-place [0 -1])]
   (union
    ;; A triangular upper wall for t1.
    (hull
     (t1 (post [:south :west]))
     (t1 (post [:north :west]))
     (t1 (wall 1 [:south :west]))
     (t1 (wall 1 [:north :west]))
     (t1 (wall 2 [:south :west]))
     (t1 (wall 3 [:south :west])))
    ;; A very small piece connecting the partial walls of t1 and t2.
    (hull
     (t1 (post [:north :west]))
     (t1 (wall 1 [:north :west]))
     (t2 (wall 1 [:south :west]))
     (t2 (post [:south :west])))
    ;; A short partial wall for t2.
    (hull
     (t2 (post [:south :west]))
     (t2 (post [:north :west]))
     (t2 (wall 1 [:south :west]))
     (t2 (wall 1 [:north :west])))
    ;; A short partial wall for f2.
    (hull
     (f2 (post [:north :west]))
     (f2 (post [:south :west]))
     (f2 (wall 1 [:north :west]))
     (f2 (wall 1 [:south :west])))
    ;; A big piece connecting the finger wall on the left to t1.
    (hull
     (bottom-hull (f0 (wall 3 [:south :west])))
     (t1 (wall 1 [:north :west])))
    ;; A piece connecting the upper wall of f0 to both t1 and t2.
    (hull
     (f0 (post [:south :west]))
     (f0 (wall 1 [:south :west]))
     (f0 (wall 3 [:south :west]))
     (t1 (wall 1 [:north :west]))
     (t2 (wall 1 [:south :west])))
    ;; A deliberately somewhat oversized link between f0 and t2.
    (hull
     (f0 (post [:south :west]))
     (f0 (post [:south :east]))
     (t2 (wall 1 [:north :west]))
     (t2 (wall 1 [:south :west])))
    ;; A bunch of interlinking plates at the top level.
    (triangle-hulls
     (f0 (post [:south :west]))
     (t2 (wall 1 [:north :west]))
     (f0 (post [:south :east]))
     (t2 (post [:north :west]))
     (f1 (post [:north :west]))
     (t2 (post [:north :east]))
     (f1 (post [:south :west]))
     (t3 (post [:north :west]))
     (f1 (post [:south :west]))
     (t3 (post [:north :east]))  ; Top right corner of thumb cluster.
     (f1 (post [:south :east]))
     (t3 (post [:north :east]))  ; Reprise.
     (f2 (wall 1 [:north :west]))
     (t3 (post [:north :east]))  ; Reprise.
     (f2 (wall 1 [:south :west]))
     (t3 (post [:north :east]))  ; Reprise.
     (f2 (wall 3 [:south :west]))
    ;; A shim transitioning from a corner of t3 into a partial wall of t3/t4.
    (hull
     (t3 (wall 1 [:north :east]))
     (t4 (wall 2 [:north :east]))
     (t4 (wall 1 [:north :east]))
     (t3 (post [:south :east]))
     (t4 (wall 2 [:north :east]))
     (t4 (post [:north :east])))
    ;; A medium piece connecting the walls of the two clusters on the right.
    (hull
     (f2 (wall 3 [:south :west]))
     (t3 (wall 1 [:north :east]))
     (t4 (wall 1 [:north :east]))
     (t4 (wall 2 [:north :east]))
     (t4 (wall 3 [:north :east])))
    ;; A large piece also connecting the walls of the two clusters.
    (bottom-hull
     (f2 (wall 2 [:south :west]))
     (f2 (wall 3 [:south :west]))
     (t4 (wall 2 [:north :east]))
     (t4 (wall 3 [:north :east])))))))

;;;;;;;;;;;;;;;;
;; Wrist Rest ;;
;;;;;;;;;;;;;;;;

(defn wrist-to-case [[column corner]]
  "An [x y] coordinate pair at the south wall of the case."
  (take 2
    (finger-key-position
      [column (first (finger-row-indices column))]
      (wall-corner-offset :south corner))))

(def wrist-connection-column 2)
(def wrist-connector-xy-west (wrist-to-case [wrist-connection-column mount-south-east]))
(def wrist-connector-xy-east (wrist-to-case [last-finger-column mount-south-east]))
(def wrist-plinth-xy-west (vec (map + wrist-connector-xy-west [0 -20])))
(def wrist-plinth-xy-east [(first wrist-connector-xy-east) (second wrist-plinth-xy-west)])
(def wrist-plinth-width (- (first wrist-plinth-xy-east)
                           (first wrist-plinth-xy-west)))
(def wrist-plinth-length 70)
(def wrist-plinth-height 50)
(def wrist-grid-unit-x 6)
(def wrist-grid-unit-y wrist-plinth-length)
(def wrist-node-size 2)

(def last-wrist-column (int (/ wrist-plinth-width wrist-grid-unit-x)))
(def last-wrist-row (int (/ wrist-plinth-length wrist-grid-unit-y)))
(def all-wrist-columns (range 0 (+ last-wrist-column 1)))
(def all-wrist-rows (range 0 (+ last-wrist-row 1)))
(def node-coordinates (coordinate-pairs all-wrist-columns all-wrist-rows))

(defn wrist? [[column row]]
  "True if specified node in wrist rest surface has been requested."
  (and (<= 0 column last-wrist-column) (<= 0 row last-wrist-row)))

(def wrist-node
  (let [h (+ (abs wall-z-offset) plate-thickness)
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
  (extrude-linear
    {:height wrist-connector-height}
    (polygon
      (concat
        (rest
          (map wrist-to-case
            (for [column (filter (partial <= wrist-connection-column) all-finger-columns)
                  corner [mount-south-west mount-south-east]]
              [column corner])))
        [wrist-plinth-xy-east wrist-plinth-xy-west]))))

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
    (map #(bottom-hull (wrist-node-place % wrist-node)) node-coordinates)))

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
        (partial key-wall-brace wrist-node-place node-corner-post)))))

(def wrist-rest
  (union
    wrist-connector
    #_wrist-nodes  ; Visualization.
    (color [1 1 1 1] wrist-surface)
    wrist-plinth))

;;;;;;;;;;;;;;;;;;;;
;; Minor Features ;;
;;;;;;;;;;;;;;;;;;;;

;; 4P4C connector holder:
(def rj9-origin
  (let [c0 [0 (last (finger-row-indices 0))]
        c1 [1 (last (finger-row-indices 1))]
        corner (fn [c] (finger-key-position c (wall-corner-offset :north mount-north-west)))
        [x0 y0] (take 2 (map + (corner c0) (corner c1)))]
   (map + [0 0 0] [(/ x0 2) (/ y0 2) 0])))
(defn rj9-position [shape]
  (->> shape
       (translate rj9-translation)
       (rotate (/ π 6) [0 0 1])
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
  channel for the 4 wires entering the case.

  "
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
(def usb-holder-position (finger-key-position [1 0] (map + (wall-segment-offset 2 :north) [0 (/ mount-height 2) 0])))
(def usb-holder-size [6.5 10.0 13.6])
(def usb-holder-thickness 4)
(def usb-holder
    (->> (cube (+ (first usb-holder-size) usb-holder-thickness) (second usb-holder-size) (+ (last usb-holder-size) usb-holder-thickness))
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))
(def usb-holder-hole
    (->> (apply cube usb-holder-size)
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))

;; LED strip:
(def led-height (+ (/ led-housing-size 2) 2))
(def west-wall-west-points
  (for [row (finger-row-indices 0)
        corner [mount-south-west mount-north-west]]
   (let [[x y] (take 2 (finger-key-position
                        [0 row]
                        (wall-corner-offset :west corner)))]
    [(+ x wall-thickness) y])))
(def west-wall-east-points
  (map (fn [[x y]] [(+ x 10) y]) west-wall-west-points))
(def west-wall-led-channel
  (extrude-linear {:height 50}
    (polygon (concat west-wall-west-points (reverse west-wall-east-points)))))
(defn led-hole-position [ordinal]
  (let [[x0 y0] (take 2 (finger-key-position
                         [0 (first (finger-row-indices 0))]
                         (wall-corner-offset :west mount-north-west)))]
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
  (color [0.5 0.5 0.5 1]
    (cube micro-usb-width micro-usb-length micro-usb-height)))
(def micro-usb-channel (cube 7.8 10 2.8))

;; Teensy MCUs:
;; Not fully supported at the moment since Pro Micro is hardcoded below.
(def teensy-width 20)
(def teensy-height 12)
(def teensy-length 33)
(def teensy2-length 53)

;; Arduino Pro Micro MCU:
(def promicro-width 18)
(def promicro-length 33)
(def promicro-pcb-thickness 1.3)
(def promicro-usb-offset
   [0
    (+ (/ promicro-length 2) 1 (/ micro-usb-length -2))
    (+ (/ promicro-pcb-thickness 2) (/ micro-usb-height 2))])
(def promicro-usb (cube promicro-width promicro-length promicro-pcb-thickness))

(def promicro-model
  (union
    (translate promicro-usb-offset micro-usb-receptacle)
    (color [26/255, 90/255, 160/255 1] promicro-usb)))

(def promicro-space-model
  (union
    (translate promicro-usb-offset micro-usb-channel)
    (hull
      (translate (vec (map + promicro-usb-offset [0 4 0])) (cube 15 1 10))
      (translate (vec (map + promicro-usb-offset [0 9 0])) (cube 30 1 25)))
    promicro-usb))

(defn mcu-position [shape]
  "Transform passed shape into the reference frame for an MCU holder."
  (let [[x y] (take 2
                (finger-key-position
                  [mcu-finger-column
                   (last (finger-row-indices mcu-finger-column))]
                  (wall-corner-offset :north mount-north-west)))]
   (->> shape
        (rotate (/ π -2) [0 1 0])
        (translate [x
                    (- y (/ promicro-length 2))
                    (+ (/ promicro-width 2) 4)]))))

(def promicro-visualization (mcu-position promicro-model))
(def promicro-negative (mcu-position promicro-space-model))

;; Holder for Pro Micro:
(def promicro-support
  (let [plinth-width 4
        plinth-height 4
        cervix-column mcu-finger-column
        cervix-coordinates [cervix-column
                            (first (take-last 3 (finger-row-indices cervix-column)))]]
    (union
      (mcu-position
        (union
          ;; A support beneath the end of the PCB.
          (translate
            [(- (/ promicro-width -2) (/ plinth-height 2)) (/ promicro-length -2) 0]
            (cube plinth-height 3 plinth-width))
          ;; A little gripper to hold onto the PCB and stabilize it horizontally.
          ;; This is intended to be just shallow enough that the spine holding it
          ;; will bend back far enough for the installation, and is placed to
          ;; avoid covering any of the through-holes.
          (translate
            [0 (/ promicro-length -2) 0]
            (cube (/ promicro-width 2) 2 plinth-width))))
        ;; The spine connects a sacrum, which is the main body of the plinth
        ;; at ground level, with a cervix that helps support the finger web.
        (triangle-hulls
          (mcu-position
            (translate [(+ (/ promicro-width -3) 1) (+ (/ promicro-width -2) -12) 0]
              (cube 16 9 plinth-width)))
          (finger-key-place cervix-coordinates (mount-corner-post [:north :east]))
          (finger-key-place cervix-coordinates (mount-corner-post [:south :east]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Final Composition and Output ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn print-finger-matrix []
  "Print a picture of the finger layout. No thumb keys."
  (for [row (reverse all-finger-rows)
        column all-finger-columns]
    (do (if (finger? [column row]) (print "□") (print "·"))
        (if (= column last-finger-column) (println)))))

(def model-right
  (union
    (difference
      (union
        (difference
          (union case-walls-for-the-fingers
                 case-wrist-hook
                 case-walls-for-the-thumbs
                 key-cluster-bridge
                 promicro-support)
          promicro-negative
          led-holes
          rj9-space)
        finger-plates
        finger-connectors
        thumb-plates
        thumb-connectors
        rj9-holder)
      finger-cutouts
      finger-key-channels
      thumb-cutouts
      thumb-key-channels
      (translate [0 0 -20] (cube 500 500 40)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    #_promicro-visualization
    #_finger-keycaps
    #_thumb-keycaps))

(def wrist-rest-right
  (intersection
    (difference
      wrist-rest
      (union
        case-walls-for-the-fingers
        case-wrist-hook))
     (translate [0 0 50] (cube 500 500 100))))

(spit "things/right-hand.scad"
      (write-scad model-right))

(spit "things/right-wrist.scad"
      (write-scad wrist-rest-right))

(spit "things/left-hand.scad"
      (write-scad (mirror [-1 0 0] model-right)))

(spit "things/left-wrist.scad"
      (write-scad (mirror [-1 0 0] wrist-rest-right)))

(defn -main [dum] 1)  ; Dummy to make it easier to batch.
