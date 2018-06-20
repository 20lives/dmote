;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Key Utilities — Switches and Keycaps                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.key
  (:require [clojure.core.matrix :as matrixmath]
            [scad-clj.model :exclude [use import] :refer :all]
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

(defn resolve-flex [getopt cluster [c0 r0]]
  "Resolve supported keywords in a coordinate pair to names.
  This allows for integers as well as the keywords :first and :last, meaning
  first and last in the column or row. Columns have priority."
  (let [columns (getopt :key-clusters cluster :derived :column-range)
        c1 (case c0 :first (first columns) :last (last columns) c0)
        rows (getopt :key-clusters cluster :derived :row-indices-by-column c1)]
   [c1 (case r0 :first (first rows) :last (last rows) r0)]))

(defn match-flex [getopt cluster & coords]
  "Check if coordinate pairs are the same, with keyword support."
  (apply = (map (partial resolve-flex getopt cluster) coords)))

(defn most-specific-getter [getopt end-path]
  "Return a function that will find the most specific configuration value
  available for a key on the keyboard."
  (let [get-in-section
          (fn [section-path]
            (let [full-path (concat [:by-key] section-path [:parameters] end-path)]
              (apply getopt full-path)))
        get-default (fn [] (get-in-section []))
        try-get
          (fn [section-path]
            (try
              (get-in-section section-path)
              (catch clojure.lang.ExceptionInfo e
                (if-not (= (:type (ex-data e)) :missing-parameter)
                  (throw e)))))
        find-index (fn [pred coll]
                     (first (keep-indexed #(when (pred %2) %1) coll)))]
    (fn [cluster [column row]]
      "Check, in order: Key-specific values favouring first/last row;
      column-specific values favouring first/last column;
      cluster-specific values; and finally the base section, where a
      value is required to exist."
      (let [columns (getopt :key-clusters cluster :derived :column-range)
            by-col (getopt :key-clusters cluster :derived :row-indices-by-column)
            rows (by-col column)
            first-column (= (first columns) column)
            last-column (= (last columns) column)
            first-row (= (first rows) row)
            last-row (= (last rows) row)
            sources
              [[[] []]
               [[first-column] [:columns :first]]
               [[last-column] [:columns :last]]
               [[] [:columns column]]
               [[first-column first-row] [:columns :first :rows :first]]
               [[first-column last-row] [:columns :first :rows :last]]
               [[last-column first-row] [:columns :last :rows :first]]
               [[last-column last-row] [:columns :last :rows :last]]
               [[first-row] [:columns column :rows :first]]
               [[last-row] [:columns column :rows :last]]
               [[] [:columns column :rows row]]]
            good-source
              (fn [coll [requirements section-path]]
                (if (every? boolean requirements)
                  (conj coll (concat [:clusters cluster] section-path))
                  coll))
            prio (reduce good-source [] (reverse sources))]
        (if-let [non-default (some try-get prio)] non-default (get-default))))))

(defn most-specific-option [getopt end-path cluster coord]
  ((most-specific-getter getopt end-path) cluster coord))

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

(defn resolve-aliases [getopt]
  "Unify cluster-specific key aliases into a single global map that preserves
  their cluster of origin and resolves symbolic coordinates to absolute values."
  {:aliases
    (into {}
      (mapcat
        (fn [cluster]
          (into {}
            (map
              (fn [[alias flex]]
                [alias {:cluster cluster
                        :coordinates (resolve-flex getopt cluster flex)}]))
            (getopt :key-clusters cluster :aliases)))
        [:finger :thumb]))})

(defn print-matrix [cluster getopt]
  "Print a schematic picture of a key cluster. For your REPL."
  (let [prop (partial getopt :key-clusters cluster :derived)]
    (doseq [row (reverse (prop :row-range)) column (prop :column-range)]
      (if ((prop :key-requested?) [column row]) (print "□") (print "·"))
      (if (= column (prop :last-column)) (println)))))


;;;;;;;;;;;;;;;;;;;
;; Keycap Models ;;
;;;;;;;;;;;;;;;;;;;

(defn keycap-properties [getopt]
  (let [height (getopt :keycaps :body-height)
        travel (getopt :switches :travel)
        resting-clearance (getopt :keycaps :resting-clearance)
        plate plate-thickness
        coll {:pressed-cap-bottom (- resting-clearance travel)
              :resting-cap-bottom resting-clearance}]
   {:from-plate-top coll
    :from-plate-bottom (into {} (map (fn [[k v]] [k (+ plate v)]) coll))}))

(defn negative-cap-shape [getopt {h3 :height w3 :top-width m :margin}]
  "The shape of a channel for a keycap to move in."
  (let [step (fn [h w] (translate [0 0 h] (cube w w 1)))
        h1 (getopt :keycaps :derived :from-plate-top :pressed-cap-bottom)
        w1 (+ key-width-1u m)
        h2 (getopt :keycaps :derived :from-plate-top :resting-cap-bottom)]
   (color [0.75 0.75 1 1]
     (translate [0 0 plate-thickness]
       (pairwise-hulls
         ;; A bottom plate for ease of mounting a switch:
         (step 0.5 (max keyswitch-hole-x keyswitch-hole-y))
         ;; Space for the keycap’s edges in travel:
         (step h1 w1)
         (step h2 w1)
         ;; Space for the upper body of a keycap at rest:
         (step h3 w3))))))

(defn key-length [units] (- (* units mount-1u) (* 2 key-margin)))

(defn keycap-model [getopt units]
  "The shape of one keycap, rectangular base, ’units’ in width, at rest."
  (let [base-width (key-length units)
        base-depth (key-length 1)
        z (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)]
   (->>
     (square base-width base-depth)
     (extrude-linear {:height (getopt :keycaps :body-height)
                      :center false :scale 0.73})  ; Based on DSA.
     (translate [0 0 z])
     (color [220/255 163/255 163/255 1]))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Placement Functions — General ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def single-switch-plate
  (translate [0 0 (/ plate-thickness 2)]
    (cube mount-width mount-depth plate-thickness)))

(def single-switch-cutout
  "Negative space for the insertion of a key switch through a mounting plate."
  (let [h (- (* 2 keyswitch-cutout-height) plate-thickness)
        trench-scale 2.5]
   (translate [0 0 (/ plate-thickness 2)]
     (union
       ;; Space for the part of a switch above the mounting hole.
       (translate [0 0 plate-thickness]
         (cube keyswitch-overhang-x keyswitch-overhang-y plate-thickness))
       ;; The hole through the plate.
       (cube keyswitch-hole-x keyswitch-hole-y h)
       ;; ALPS-specific space for wings to flare out.
       (translate [0 0 -1.5]
         (cube (+ keyswitch-hole-x 1) keyswitch-hole-y plate-thickness))))))

(defn mount-corner-offset [directions]
  "Produce a translator for getting to one corner of a switch mount."
  (general-corner
    mount-width mount-depth web-thickness plate-thickness directions))

(defn mount-corner-post [directions]
  "A post shape that comes offset for one corner of a key mount."
  (translate (mount-corner-offset directions) web-post))

(defn curver [subject dimension-n rotate-type delta-fn orthographic
              translate-fn rotate-fn getopt cluster coord obj]
  "Given an angle for progressive curvature, apply it. Else lay keys out flat."
  (let [index (nth coord dimension-n)
        most #(most-specific-option getopt % cluster coord)
        angle-factor (most [:layout rotate-type :progressive])
        neutral (most [:layout :matrix :neutral subject])
        separation (most [:layout :matrix :separation subject])
        delta-f (delta-fn index neutral)
        delta-r (delta-fn neutral index)
        angle-product (* angle-factor delta-f)
        flat-distance (* mount-1u (- index neutral))
        cap-height (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)
        radius (+ cap-height
                  (/ (/ (+ mount-1u separation) 2)
                     (Math/sin (/ angle-factor 2))))
        ortho-x (- (* delta-r (+ -1 (- (* radius (Math/sin angle-factor))))))
        ortho-z (* radius (- 1 (Math/cos angle-product)))]
   (if (zero? angle-factor)
     (translate-fn (assoc [0 0 0] dimension-n flat-distance) obj)
     (if orthographic
       (->> obj
            (rotate-fn angle-product)
            (translate-fn [ortho-x 0 ortho-z]))
       (swing-callables translate-fn radius (partial rotate-fn angle-product) obj)))))

(defn put-in-column [translate-fn rotate-fn getopt cluster coord obj]
  "Place a key in relation to its column."
  (curver :row 1 :pitch #(- %1 %2) false
          translate-fn rotate-fn getopt cluster coord obj))

(defn put-in-row [translate-fn rotate-fn getopt cluster coord obj]
  "Place a key in relation to its row."
  (let [style (getopt :key-clusters cluster :style)]
   (curver :column 0 :roll #(- %2 %1) (= style :orthographic)
           translate-fn rotate-fn getopt cluster coord obj)))

(declare cluster-position)

(defn cluster-placement [translate-fn [rot-x rot-y rot-z]
                         getopt cluster coord subject]
  "Place and tilt passed ‘subject’ as if into a key cluster."
  (let [[column row] coord
        most #(most-specific-option getopt % cluster coord)
        center (most [:layout :matrix :neutral :row])
        bridge
          (if (= cluster :finger)
            identity
            (fn [obj]
              (let [section (partial getopt :key-clusters cluster :position)
                    alias (getopt :key-clusters :derived :aliases (section :key-alias))
                    finger-pos (cluster-position  getopt (:cluster alias)
                                 (:coordinates alias) [0 0 0])
                    final (vec (map + finger-pos (section :offset)))]
               (translate-fn final obj))))]
    (->> subject
         (translate-fn (most [:layout :translation :early]))
         (rot-x (most [:layout :pitch :intrinsic]))
         (put-in-column translate-fn rot-x getopt cluster coord)
         (put-in-row translate-fn rot-y getopt cluster coord)
         (translate-fn (most [:layout :translation :mid]))
         (rot-x (most [:layout :pitch :base]))
         (rot-y (most [:layout :roll :base]))
         (rot-z (most [:layout :yaw :base]))
         (translate-fn [0 (* mount-1u center) 0])
         (translate-fn (most [:layout :translation :late]))
         (bridge))))

(def cluster-place
  "A function that puts a passed shape in a specified key matrix position."
  (partial cluster-placement
    translate
    [(fn [angle obj] (rotate angle [1 0 0] obj))
     (fn [angle obj] (rotate angle [0 1 0] obj))
     (fn [angle obj] (rotate angle [0 0 1] obj))]))

(def cluster-position
  "Get coordinates for a key cluster position.
  Using this wrapper, the ‘subject’ argument to cluster-placement should be a
  single point in 3-dimensional space, typically an offset in mm from the
  middle of the indicated key."
  (partial cluster-placement
    (partial map +)
    [(fn [angle position]
       "Transform a set of coordinates as if in rotation around the X axis."
       (matrixmath/mmul
        [[1 0 0]
         [0 (Math/cos angle) (- (Math/sin angle))]
         [0 (Math/sin angle)    (Math/cos angle)]]
        position))
     (fn [angle position]
       "Same for the Y axis."
       (matrixmath/mmul
        [[(Math/cos angle)     0 (Math/sin angle)]
         [0                    1 0]
         [(- (Math/sin angle)) 0 (Math/cos angle)]]
        position))
     (fn [angle position]
       "Same for the Z axis."
       (matrixmath/mmul
        [[(Math/cos angle) (- (Math/sin angle)) 0]
         [(Math/sin angle) (Math/cos angle)     0]
         [0                0                    1]]
        position))]))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interface Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster-plates [getopt cluster]
  (apply union (map #(cluster-place getopt cluster % single-switch-plate)
                    (getopt :key-clusters cluster :derived :key-coordinates))))

(defn cluster-cutouts [getopt cluster]
  (apply union (map #(cluster-place getopt cluster % single-switch-cutout)
                    (getopt :key-clusters cluster :derived :key-coordinates))))

(defn cluster-channels [getopt cluster]
  (letfn [(modeller [coord]
            (letfn [(most [path]
                      (most-specific-option getopt path cluster coord))]
              (negative-cap-shape getopt
                {:top-width (most [:channel :top-width])
                 :height (most [:channel :height])
                 :margin (most [:channel :margin])})))]
    (apply union (map #(cluster-place getopt cluster % (modeller %))
                      (getopt :key-clusters cluster :derived :key-coordinates)))))

(defn cluster-keycaps [getopt cluster]
  (apply union (map #(cluster-place getopt cluster % (keycap-model getopt 1))
                    (getopt :key-clusters cluster :derived :key-coordinates))))
