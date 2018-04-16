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
(def mount-width 18.4)
(def mount-depth 18.4)

;; ALPS-style switches:
(def alps-width 15.5)
(def alps-depth 12.6)
(def alps-notch-height 1)
(def alps-height-below-notch 4.5)

;; Hardcode ALPS as our switch type.
(def keyswitch-depth alps-depth)
(def keyswitch-width alps-width)
(def keyswitch-cutout-height alps-height-below-notch)


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
  "Negative space for the insertion of a key switch and the movement of a cap.
  A cube centered on a switch plate, with some overshoot for clean previews,
  and a further, more narrow dip for the legs of the switch,
  and narrowing space above the plate for a keycap."
  (let [h (- (* 2 keyswitch-cutout-height) plate-thickness)
        trench-scale 2.5]
   (translate [0 0 (/ plate-thickness 2)]
     (union
       negative-cap-minimal
       (cube keyswitch-width keyswitch-depth h)
       ;; ALPS-specific space for wings to flare out.
       (translate [0 0 -1.5]
         (cube (+ keyswitch-width 1) keyswitch-depth plate-thickness))
       (if (not (zero? keyswitch-trench-depth))
         (translate [0 0 (- h)]
           (extrude-linear
             {:height keyswitch-trench-depth :center false :scale trench-scale}
             (square (/ keyswitch-width trench-scale) (/ keyswitch-depth trench-scale)))))))))

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

(defn row-radius [column row]
  (+ (/ (/ (+ mount-1u finger-mount-separation-y) 2)
           (Math/sin (/ (progressive-pitch [column row]) 2)))
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
        pitch-angle (* (progressive-pitch [column row]) (- row curvature-centerrow))
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
         (rotate (intrinsic-thumb-key-rotation [column row] [0 0 0]))
         (translate [(* column (+ mount-1u thumb-mount-separation))
                     (* row (+ mount-1u thumb-mount-separation))
                     0])
         (translate offset)
         (translate (get intrinsic-thumb-key-translation [column row] [0 0 0]))
         (rotate thumb-cluster-rotation)
         (translate thumb-origin)
         )))

(defn for-thumbs [shape]
  (apply union (for [column all-thumb-columns
                     row all-thumb-rows]
                 (thumb-key-place [column row] shape))))

(def thumb-plates (for-thumbs single-switch-plate))

(def thumb-cutouts (for-thumbs single-switch-cutout))

(def thumb-keycaps (for-thumbs (keycap 1)))
