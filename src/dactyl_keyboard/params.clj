;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameters                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This file is dedicated to variables you can use to personalize the models.
;;; You can’t control everything from here, but you can change a number of
;;; things without having to adjust more complex code.

(ns dactyl-keyboard.params
  (:require [unicode-math.core :refer :all]
            [dactyl-keyboard.generics :refer :all]))

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
(def thumb-cluster-offset-from-fingers [9 0 0])
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
   {[-1 0] [0 0 0]
    [-1 -1] [0 0 0]
    [-1 -2] [0 0 0]
    [0 0] [0 0 15]
    [0 -1] [0 0 15]
    [0 -2] [0 0 15]})
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
       [1 -2] [2 4]
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
;; defining floor-level plates in relation to finger keys, optionally with
;; two-dimensional offsets.
;; A future version may support threaded holes through these feet for mounting
;; printed parts on solid plates.
(def include-feet true)
(def foot-height 4)
(def foot-plate-posts
  [;; Close to user, fairly central, in two parts:
   [[[2 -3] NNW] [[2 -3] NNE] [[3 -2] SSW] [[2 -2] SSE]]
   [[[3 -2] SSW] [[3 -2] SSE] [[4 -2] WSW] [[2 -2] SSE]]
   ;; On the right:
   [[[5 0] WNW] [[5 0] NNE] [[5 -1] ENE]]])

;; Settings for column-style :fixed.
;; The defaults roughly match Maltron settings:
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
(def wrist-connection-offset [0 -40])
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
(def backplate-offset [0 0 -15])
;; The backplate will have two holes for threaded fasteners.
(def backplate-fastener-distance 30)
(def backplate-fastener-diameter 5)
(def backplate-beam-height 20)
;; The ‘installation-angle’ is the angle of each half of the keyboard relative
;; to the lateral beam.
(def installation-angle (deg2rad -6))

;; Minor features:
(def mcu-finger-column 4)
(def mcu-offset [0 4 0])
(def mcu-connector-direction :east)
(def rj9-translation [-3 -8 0])

;; LED holes along the inner wall. Defaults are for WS2818 at 17 mm intervals.
(def include-led-housings true)
(def led-housing-size 5.3)  ; Exaggerated; really 5 mm.
(def led-emitter-diameter 4)
(def led-pitch 16.8)  ; Allowance for slight wall curvature.
(def led-amount 3)
