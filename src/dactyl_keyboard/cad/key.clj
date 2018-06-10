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
          (M (fn [[r cols]] [r (for [c cols] [c r])]) column-indices-by-row)
        resolve-coordinates
          (fn [[column row]]
            "Resolve the keywords :first and :last to absolute indices."
            (let [rc (case column
                       :first (first column-range)
                       :last (last column-range)
                       column)
                  rr (case row
                       :first (first (row-indices-by-column rc))
                       :last (last (row-indices-by-column rc))
                       row)]
              [rc rr]))]
   {:last-column last-column
    :column-range column-range
    :row-range row-range
    :key-requested? key-requested?
    :key-coordinates key-coordinates
    :row-indices-by-column row-indices-by-column
    :coordinates-by-column coordinates-by-column
    :column-indices-by-row column-indices-by-row
    :coordinates-by-row coordinates-by-row
    :resolve-coordinates resolve-coordinates}))

(defn most-specific-option [getopt end-path cluster coord]
  ((most-specific-getter getopt end-path) cluster coord))

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

(defn keycap-properties [getopt]
  (let [height (getopt :keycaps :body-height)
        travel (getopt :switches :travel)
        resting-clearance (getopt :keycaps :resting-clearance)
        pressed-clearance (- resting-clearance travel)
        plate plate-thickness
        bottom-to-bottom (+ plate resting-clearance)
        bottom-to-middle (+ bottom-to-bottom (/ height 2))
        bottom-to-pressed-bottom (+ plate pressed-clearance)]
   {:from-plate-top {:resting-cap-middle bottom-to-middle
                     :resting-cap-bottom resting-clearance
                     :pressed-cap-bottom pressed-clearance}
    :from-plate-bottom {:resting-cap-bottom bottom-to-bottom
                        :pressed-cap-bottom bottom-to-pressed-bottom}}))

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
        z (getopt :keycaps :derived :from-plate-bottom :resting-cap-middle)]
   (->>
     (square base-width base-depth)
     (extrude-linear {:height (getopt :keycaps :body-height)
                      :scale 0.73})  ; Based on DSA.
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

(defn stylist [translate-fn pitcher pitch-radius rotate-y-fn getopt cluster [column row] obj]
  "Produce a closure that will apply a specific key cluster style."
  (let [style (getopt :key-clusters cluster :style)
        column-curvature-offset (- curvature-centercol column)
        roll-angle (* β column-curvature-offset)
        radius-base (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)
        column-radius (+ radius-base
                         (/ (/ (+ mount-1u finger-mount-separation-x) 2)
                            (Math/sin (/ β 2))))]
   (case style
     :standard
       (->> obj
         (swing-callables translate-fn pitch-radius pitcher)
         (swing-callables translate-fn column-radius (partial rotate-y-fn roll-angle))
         (translate-fn (finger-column-translation column)))
     :orthographic
       (let [column-x-delta (+ -1 (- (* column-radius (Math/sin β))))
             x (- (* column-curvature-offset column-x-delta))
             radius-coefficient (- 1 (Math/cos (* β column-curvature-offset)))
             z (* radius-coefficient column-radius)]
         (->> obj
           (swing-callables translate-fn pitch-radius pitcher)
           (rotate-y-fn roll-angle)
           (translate-fn [x 0 z])
           (translate-fn (finger-column-translation column))))
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

(defn finger-placement [translate-fn rotate-x-fn rotate-y-fn getopt coord subject]
  "Place and tilt passed ‘subject’ as if it were a key or coordinate vector."
  (let [[column row] coord
        pitch-base (most-specific-option getopt [:layout :pitch-base] :finger coord)
        neutral (most-specific-option getopt [:layout :neutral-pitch-row] :finger coord)
        pitch-prog (* (progressive-pitch coord) (- row neutral))
        cap-height (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)
        pitch-radius (+ cap-height
                        (/ (/ (+ mount-1u finger-mount-separation-y) 2)
                           (Math/sin (/ (progressive-pitch coord) 2))))
        y-offset (* mount-1u neutral)
        z-offset (getopt :key-clusters :finger :vertical-offset)]
    (->> subject
         (translate-fn (get finger-tweak-early-translation coord [0 0 0]))
         (rotate-x-fn (get finger-intrinsic-pitch coord 0))
         (stylist translate-fn (partial rotate-x-fn pitch-prog) pitch-radius rotate-y-fn getopt :finger coord)
         (rotate-x-fn pitch-base)
         (rotate-y-fn (getopt :key-clusters :finger :tenting))
         (translate-fn [0 y-offset z-offset])
         (translate-fn (get finger-tweak-late-translation coord [0 0 0])))))

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

(defn coordinate-pair-matcher [getopt cluster]
  "Return a function that checks if two coordinates match.
  This allows for integers as well as the keywords :first and :last, meaning
  first and last in the column or row."
  (let [rows (getopt :key-clusters cluster :derived :row-range)
        columns (getopt :key-clusters cluster :derived :column-range)]
    (fn [[config-column config-row] [subject-column subject-row]]
      (and (or (= config-column subject-column)
               (and (= config-column :first)
                    (= subject-column (first columns)))
               (and (= config-column :last)
                    (= subject-column (last columns))))
           (or (= config-row subject-row)
               (and (= config-row :first)
                    (= subject-row (first rows)))
               (and (= config-row :last)
                    (= subject-row (last))))))))

(defn finger-plates [getopt]
  (apply union (map #(finger-key-place getopt % single-switch-plate)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-cutouts [getopt]
  (apply union (map #(finger-key-place getopt % single-switch-cutout)
                    (getopt :key-clusters :finger :derived :key-coordinates))))

(defn finger-channels [getopt]
  (letfn [(modeller [coord]
            (letfn [(most [path]
                      (most-specific-option getopt path :finger coord))]
              (negative-cap-shape getopt
                {:top-width (most [:channel :top-width])
                 :height (most [:channel :height])
                 :margin (most [:channel :margin])})))]
    (apply union (map #(finger-key-place getopt % (modeller %))
                      (getopt :key-clusters :finger :derived :key-coordinates)))))

(defn finger-keycaps [getopt]
  (apply union (map #(finger-key-place getopt % (keycap-model getopt 1))
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

(defn thumb-keycaps [getopt] (for-thumbs getopt (keycap-model getopt 1)))
