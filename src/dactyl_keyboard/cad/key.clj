;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Key Utilities — Switches and Keycaps                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.key
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.params :refer :all]
            [dactyl-keyboard.cad.misc :refer :all]
            [dactyl-keyboard.cad.matrix :refer :all]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Core Definitions — All Switches ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def web-post
  "A shape for attaching things to a corner of a switch mount."
   (cube corner-post-width corner-post-width web-thickness))

;; Mounts for neighbouring 1U keys are about 0.75” apart.
(def mount-1u 19.05)

;; Typical 1U keycap width and depth, approximate.
(def key-width-1u 18.25)
(def key-margin (/ (- mount-1u key-width-1u) 2))

;; Mount plates are a bit wider than typical keycaps.
(def mount-width (+ key-width-1u 0.15))
(def mount-depth mount-width)

;; ALPS-style switches:
(def alps-hole-x 15.5)
(def alps-hole-y 12.6)
(def alps-overhang-x 17.25)  ; Width of notches.
(def alps-overhang-y 14.25)
(def alps-overhang-z 1)  ; Height of notch above hole/plate.
(def alps-underhang-z 4.5)  ; Height of body up to plate top.

;; Hardcode ALPS as our switch type.
(def keyswitch-hole-y alps-hole-y)
(def keyswitch-hole-x alps-hole-x)
(def keyswitch-overhang-x alps-overhang-x)
(def keyswitch-overhang-y alps-overhang-y)
(def keyswitch-cutout-height alps-underhang-z)


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Definitions — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster-properties [cluster getopt]
  "Derive some properties about a specific key cluster from raw configuration info."
  (let [matrix (getopt :key-clusters cluster :matrix-columns)
        gather (fn [key default] (map #(get % key default) matrix))
        column-range (range 0 (count matrix))
        last-column (last column-range)
        max-rows-above-home (apply max (gather :rows-above-home 0))
        max-rows-below-home (apply max (gather :rows-below-home 0))
        row-range (range (- max-rows-below-home) (+ max-rows-above-home 1))
        key-requested?
          (fn [[column row]]
            "True if specified key is requested."
            (if-let [data (nth matrix column nil)]
              (cond
                (< row 0) (>= (get data :rows-below-home 0) (abs row))
                (> row 0) (>= (get data :rows-above-home 0) row)
                :else true)  ; Home row.
              false))  ; Column not in matrix.
        key-coordinates (coordinate-pairs column-range row-range key-requested?)
        M (fn [f coll] (into {} (map f coll)))
        row-indices-by-column
          (M (fn [c] [c (filter #(key-requested? [c %]) row-range)])
             column-range)
        coordinates-by-column
          (M (fn [[c rows]] [c (for [r rows] [c r])]) row-indices-by-column)
        column-indices-by-row
          (M
            (fn [r] [r (filter #(key-requested? [% r]) column-range)])
            row-range)
        coordinates-by-row
          (M (fn [[r cols]] [r (for [c cols] [c r])]) column-indices-by-row)]
   {:last-column last-column
    :column-range column-range
    :row-range row-range
    :key-requested? key-requested?
    :key-coordinates key-coordinates
    :row-indices-by-column row-indices-by-column
    :coordinates-by-column coordinates-by-column
    :column-indices-by-row column-indices-by-row
    :coordinates-by-row coordinates-by-row}))

(defn print-matrix [cluster getopt]
  "Print a schematic picture of a key cluster. For your REPL."
  (let [prop (partial getopt :key-clusters cluster :derived)]
    (doseq [row (reverse (prop :row-range)) column (prop :column-range)]
      (if ((prop :key-requested?) [column row]) (print "□") (print "·"))
      (if (= column (prop :last-column)) (println)))))


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
  (let [base (+ (max keyswitch-hole-x keyswitch-hole-y) 2)
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
  "Negative space for the insertion of a key switch and the movement of a cap.
  A cube centered on a switch plate, with some overshoot for clean previews,
  and a further, more narrow dip for the legs of the switch,
  and narrowing space above the plate for a keycap."
  (let [h (- (* 2 keyswitch-cutout-height) plate-thickness)
        trench-scale 2.5]
   (translate [0 0 (/ plate-thickness 2)]
     (union
       negative-cap-minimal
       ;; Space for the above-hole part of a switch.
       (translate [0 0 plate-thickness]
         (cube keyswitch-overhang-x keyswitch-overhang-y plate-thickness))
       ;; The hole through the plate.
       (cube keyswitch-hole-x keyswitch-hole-y h)
       ;; ALPS-specific space for wings to flare out.
       (translate [0 0 -1.5]
         (cube (+ keyswitch-hole-x 1) keyswitch-hole-y plate-thickness))
       (if (not (zero? keyswitch-trench-depth))
         (translate [0 0 (- h)]
           (extrude-linear
             {:height keyswitch-trench-depth :center false :scale trench-scale}
             (square (/ keyswitch-hole-x trench-scale) (/ keyswitch-hole-y trench-scale)))))))))

(defn mount-corner-offset [directions]
  "Produce a translator for getting to one corner of a switch mount."
  (general-corner
    mount-width mount-depth web-thickness plate-thickness directions))

(defn mount-corner-post [directions]
  "A post shape that comes offset for one corner of a key mount."
  (translate (mount-corner-offset directions) web-post))

(defn column-radius [column]
  (+ (/ (/ (+ mount-1u finger-mount-separation-x) 2)
        (Math/sin (/ β 2)))
     cap-bottom-height))

(defn column-x-delta [column]
  (+ -1 (- (* (column-radius column) (Math/sin β)))))

(defn stylist [style translate-fn pitcher pitch-radius rotate-y-fn [column row] obj]
  "Produce a closure that will apply a specific key cluster style."
  (let [column-curvature-offset (- curvature-centercol column)
        roll-angle (* β column-curvature-offset)]
   (case style
     :standard
       (->> obj
         (swing-callables translate-fn pitch-radius pitcher)
         (swing-callables translate-fn (column-radius column) (partial rotate-y-fn roll-angle))
         (translate-fn (finger-column-translation column)))
     :orthographic
       (->> obj
         (swing-callables translate-fn pitch-radius pitcher)
         (rotate-y-fn roll-angle)
         (translate-fn [(- (* column-curvature-offset (column-x-delta column)))
                        0
                        (* (column-radius column)
                           (- 1 (Math/cos (* β column-curvature-offset))))])
         (translate-fn (finger-column-translation column)))
     :fixed
       (->> obj
         (rotate-y-fn (nth fixed-angles column))
         (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
         (swing-callables translate-fn (+ pitch-radius (nth fixed-z column)) pitcher)
         (rotate-y-fn fixed-tenting)
         (translate-fn [0 (second (finger-column-translation column)) 0])))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Fingers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn finger-placement [translate-fn rotate-x-fn rotate-y-fn getopt [column row] subject]
  "Place and tilt passed ‘subject’ as if it were a key or coordinate vector."
  (let [curvature-centerrow (finger-column-curvature-centerrow column)
        pitch-angle (* (progressive-pitch [column row]) (- row curvature-centerrow))
        pitch-radius (+ (/ (/ (+ mount-1u finger-mount-separation-y) 2)
                           (Math/sin (/ (progressive-pitch [column row]) 2)))
                        cap-bottom-height)]
    (->> subject
         (translate-fn (get finger-tweak-early-translation [column row] [0 0 0]))
         (rotate-x-fn (get finger-intrinsic-pitch [column row] 0))
         (stylist column-style translate-fn (partial rotate-x-fn pitch-angle) pitch-radius rotate-y-fn [column row])
         (rotate-x-fn pitch-centerrow)
         (rotate-y-fn (getopt :key-clusters :finger :tenting))
         (translate-fn [0 (* mount-1u curvature-centerrow) keyboard-z-offset])
         (translate-fn (get finger-tweak-late-translation [column row] [0 0 0])))))

(def finger-key-position
  "A function that outputs coordinates for a key matrix position.
  Using this wrapper, the ‘subject’ argument to finger-placement should be a
  single point in 3-dimensional space, typically an offset in mm from the
  middle of the indicated key."
  (partial finger-placement
    (partial map +)
    (fn [angle position]
      "Transform a set of coordinates as if in rotation around the X axis."
      (clojure.core.matrix/mmul
       [[1 0 0]
        [0 (Math/cos angle) (- (Math/sin angle))]
        [0 (Math/sin angle)    (Math/cos angle)]]
       position))
    (fn [angle position]
      "Same for the Y axis."
      (clojure.core.matrix/mmul
       [[(Math/cos angle)     0 (Math/sin angle)]
        [0                    1 0]
        [(- (Math/sin angle)) 0 (Math/cos angle)]]
       position))))

(def finger-key-place
  "A function that puts a passed shape in a specified key matrix position."
  (partial finger-placement
    translate
    (fn [angle obj] (rotate angle [1 0 0] obj))
    (fn [angle obj] (rotate angle [0 1 0] obj))))

(defn finger-plates [getopt]
  (apply union (map #(finger-key-place getopt % single-switch-plate)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-cutouts [getopt]
  (apply union (map #(finger-key-place getopt % single-switch-cutout)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-keycaps [getopt]
  (apply union (map #(finger-key-place getopt % (keycap 1))
                    (getopt :key-clusters :finger :derived :key-coordinates))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — Thumbs ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn thumb-origin [getopt]
  (let [by-col (getopt :key-clusters :finger :derived :coordinates-by-column)]
    (map + (finger-key-position
             getopt
             (first (by-col thumb-connection-column))
             [(/ mount-width -2) (/ mount-depth -2) 0])
           thumb-cluster-offset-from-fingers)))

(defn thumb-key-place [getopt [column row] shape]
  (let [offset (if (= -1 column) thumb-cluster-column-offset [0 0 0])]
    (->> shape
         (rotate (intrinsic-thumb-key-rotation [column row] [0 0 0]))
         (translate [(* column (+ mount-1u thumb-mount-separation))
                     (* row (+ mount-1u thumb-mount-separation))
                     0])
         (translate offset)
         (translate (get intrinsic-thumb-key-translation [column row] [0 0 0]))
         (rotate thumb-cluster-rotation)
         (translate (thumb-origin getopt)))))


(defn for-thumbs [getopt shape]
  (apply union (for [column all-thumb-columns
                     row all-thumb-rows]
                 (thumb-key-place getopt [column row] shape))))

(defn thumb-plates [getopt] (for-thumbs getopt single-switch-plate))

(defn thumb-cutouts [getopt] (for-thumbs getopt single-switch-cutout))

(defn thumb-keycaps [getopt] (for-thumbs getopt (keycap 1)))
