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
        row-indices-by-column
          (apply hash-map
            (mapcat
              (fn [c] [c (filter #(key-requested? [c %]) row-range)])
              column-range))
        column-indices-by-row
          (apply hash-map
            (mapcat
              (fn [r] [r (filter #(key-requested? [% r]) column-range)])
              row-range))]
   {:last-column last-column
    :column-range column-range
    :row-range row-range
    :key-requested? key-requested?
    :key-coordinates key-coordinates
    :row-indices-by-column row-indices-by-column
    :column-indices-by-row column-indices-by-row}))

(defn print-matrix [cluster getopt]
  "Print a schematic picture of a key cluster. For your REPL."
  (let [prop (partial getopt :key-clusters cluster :derived)]
    (doseq [row (reverse (prop :row-range)) column (prop :column-range)]
      (if ((prop :key-requested?) [column row]) (print "□") (print "·"))
      (if (= column (prop :last-column)) (println)))))

(defn finger-row-indices [column]
  "Return the range of row indices valid for passed column index."
  (range (- (get rows-below-home column rows-default))
         (+ (get rows-above-home column rows-default) 1)))
                                 ; range is exclusive ^

(defn first-in-column [column]
  [column (first (finger-row-indices column))])

(defn last-in-column [column]
  [column (last (finger-row-indices column))])


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

(defn finger-placement [translate-fn rotate-x-fn rotate-y-fn [column row] shape]
  "Place and tilt passed ‘shape’ as if it were a key."
  (let [curvature-centerrow (finger-column-curvature-centerrow column)
        pitch-angle (* (progressive-pitch [column row]) (- row curvature-centerrow))
        pitch-radius (+ (/ (/ (+ mount-1u finger-mount-separation-y) 2)
                           (Math/sin (/ (progressive-pitch [column row]) 2)))
                        cap-bottom-height)]
    (->> shape
         (translate-fn (get finger-tweak-early-translation [column row] [0 0 0]))
         (rotate-x-fn (get finger-intrinsic-pitch [column row] 0))
         (stylist column-style translate-fn (partial rotate-x-fn pitch-angle) pitch-radius rotate-y-fn [column row])
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

(defn finger-plates [getopt]
  (apply union (map #(finger-key-place % single-switch-plate)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-cutouts [getopt]
  (apply union (map #(finger-key-place % single-switch-cutout)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-keycaps [getopt]
  (apply union (map #(finger-key-place % (keycap 1))
                    (getopt :key-clusters :finger :derived :key-coordinates))))


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
         (rotate (intrinsic-thumb-key-rotation [column row] [0 0 0]))
         (translate [(* column (+ mount-1u thumb-mount-separation))
                     (* row (+ mount-1u thumb-mount-separation))
                     0])
         (translate offset)
         (translate (get intrinsic-thumb-key-translation [column row] [0 0 0]))
         (rotate thumb-cluster-rotation)
         (translate thumb-origin))))


(defn for-thumbs [shape]
  (apply union (for [column all-thumb-columns
                     row all-thumb-rows]
                 (thumb-key-place [column row] shape))))

(def thumb-plates (for-thumbs single-switch-plate))

(def thumb-cutouts (for-thumbs single-switch-cutout))

(def thumb-keycaps (for-thumbs (keycap 1)))
