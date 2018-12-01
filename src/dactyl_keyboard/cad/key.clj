;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Key Utilities — Switches and Keycaps                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.key
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [abs π]]
            [dactyl-keyboard.generics :as generics]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.param.access :refer [most-specific]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Core Definitions — All Switches ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-clusters
  "The identifiers of all defined key clusters."
  [getopt]
  (remove #(= :derived %) (keys (getopt :key-clusters))))

(defn- derived
  "A shortcut to look up a cluster-specific derived configuration detail."
  [getopt & keys]
  (apply (partial getopt :key-clusters :derived :by-cluster) keys))

;; ALPS-style switches:
(def alps-hole-x 15.5)
(def alps-hole-y 12.6)
(def alps-overhang-x 17.25)  ; Width of notches.
(def alps-overhang-y 14.25)
(def alps-overhang-z 1)  ; Height of notch above hole/plate.
(def alps-underhang-z 4.5)  ; Height of body up to plate top.

;; MX-style switches:
(def mx-hole-x 13.9954)
(def mx-hole-y 13.9954)
(def mx-overhang-x 15.494)
(def mx-overhang-y 15.494)
(def mx-overhang-z 1)  ; Estimated, dimension not included in datasheet.
(def mx-underhang-z 5.004)

(defn keyswitch-dimensions [getopt]
  (case (getopt :switches :style)
    :alps {:keyswitch-hole-x alps-hole-x
           :keyswitch-hole-y alps-hole-y
           :keyswitch-overhang-x alps-overhang-x
           :keyswitch-overhang-y alps-overhang-y
           :keyswitch-overhang-z alps-overhang-z
           :keyswitch-cutout-height alps-underhang-z}
    :mx   {:keyswitch-hole-x mx-hole-x
           :keyswitch-hole-y mx-hole-y
           :keyswitch-overhang-x mx-overhang-x
           :keyswitch-overhang-y mx-overhang-y
           :keyswitch-overhang-z mx-overhang-z
           :keyswitch-cutout-height mx-underhang-z}))

(defn resolve-flex
  "Resolve supported keywords in a coordinate pair to names.
  This allows for integers as well as the keywords :first and :last, meaning
  first and last in the column or row. Columns have priority."
  [getopt cluster [c0 r0]]
  (let [columns (derived getopt cluster :column-range)
        c1 (case c0 :first (first columns) :last (last columns) c0)
        rows (derived getopt cluster :row-indices-by-column c1)]
   [c1 (case r0 :first (first rows) :last (last rows) r0)]))

(defn match-flex
  "Check whether coordinate pairs are the same, with keyword support."
  [getopt cluster & coords]
  (apply = (map (partial resolve-flex getopt cluster) coords)))

(defn chart-cluster
  "Derive some properties about a key cluster from raw configuration info."
  [cluster getopt]
  (let [raws (getopt :key-clusters cluster)
        matrix (getopt :key-clusters cluster :matrix-columns)
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
        key-coordinates (matrix/coordinate-pairs
                          column-range row-range key-requested?)
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
   {:style (:style raws :standard)
    :last-column last-column
    :column-range column-range
    :row-range row-range
    :key-requested? key-requested?
    :key-coordinates key-coordinates
    :row-indices-by-column row-indices-by-column
    :coordinates-by-column coordinates-by-column
    :column-indices-by-row column-indices-by-row
    :coordinates-by-row coordinates-by-row}))

(defn derive-cluster-properties
  "Derive basic properties for each key cluster."
  [getopt]
  (let [by-cluster (fn [coll key] (assoc coll key (chart-cluster key getopt)))]
   {:by-cluster (reduce by-cluster {} (all-clusters getopt))}))

(defn derive-resolved-aliases
  "Unify cluster-specific key aliases into a single global map that preserves
  their cluster of origin and resolves symbolic coordinates to absolute values."
  [getopt]
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
        (all-clusters getopt)))})

(defn print-matrix
  "Print a schematic picture of a key cluster. For your REPL."
  [cluster getopt]
  (let [prop (partial derived getopt cluster)]
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
        plate (getopt :case :key-mount-thickness)
        coll {:pressed-cap-bottom (- resting-clearance travel)
              :resting-cap-bottom resting-clearance}]
   {:from-plate-top coll
    :from-plate-bottom (into {} (map (fn [[k v]] [k (+ plate v)]) coll))}))

(defn negative-cap-shape
  "The shape of a channel for a keycap to move in."
  [getopt {h3 :height w3 :top-width m :margin}]
  (let [step (fn [h w] (translate [0 0 h] (cube w w 1)))
        h1 (getopt :keycaps :derived :from-plate-top :pressed-cap-bottom)
        w1 (+ place/key-width-1u m)
        h2 (getopt :keycaps :derived :from-plate-top :resting-cap-bottom)
        keyswitch-hole-x (getopt :switches :derived :keyswitch-hole-x)
        keyswitch-hole-y (getopt :switches :derived :keyswitch-hole-y)]
   (color (:cap-negative generics/colours)
     (translate [0 0 (getopt :case :key-mount-thickness)]
       (misc/pairwise-hulls
         ;; A bottom plate for ease of mounting a switch:
         (step 0.5 (max keyswitch-hole-x keyswitch-hole-y))
         ;; Space for the keycap’s edges in travel:
         (step h1 w1)
         (step h2 w1)
         ;; Space for the upper body of a keycap at rest:
         (step h3 w3))))))

(defn keycap-model
  "The shape of one keycap, rectangular base, ’units’ in width, at rest."
  [getopt units]
  (let [base-width (place/key-length units)
        base-depth (place/key-length 1)
        z (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)]
   (->>
     (square base-width base-depth)
     (extrude-linear {:height (getopt :keycaps :body-height)
                      :center false :scale 0.73})  ; Based on DSA.
     (translate [0 0 z])
     (color (:cap-body generics/colours)))))


;;;;;;;;;;;;;;;;;;
;; Other Models ;;
;;;;;;;;;;;;;;;;;;

(defn single-switch-plate [getopt]
  (let [t (getopt :case :key-mount-thickness)]
   (translate [0 0 (/ t 2)]
     (cube place/mount-width place/mount-depth t))))

(defn single-switch-cutout
  "Negative space for the insertion of a key switch through a mounting plate."
  [getopt]
  (let [t (getopt :case :key-mount-thickness)
        keyswitch-style (getopt :switches :style)
        keyswitch-hole-x (getopt :switches :derived :keyswitch-hole-x)
        keyswitch-hole-y (getopt :switches :derived :keyswitch-hole-y)
        keyswitch-overhang-x (getopt :switches :derived :keyswitch-overhang-x)
        keyswitch-overhang-y (getopt :switches :derived :keyswitch-overhang-y)
        keyswitch-cutout-height (getopt :switches :derived :keyswitch-cutout-height)
        h (- (* 2 keyswitch-cutout-height) t)
        trench-scale 2.5]
   (translate [0 0 (/ t 2)]
     (union
       ;; Space for the part of a switch above the mounting hole.
       (translate [0 0 t]
         (cube keyswitch-overhang-x keyswitch-overhang-y t))
       ;; The hole through the plate.
       (cube keyswitch-hole-x keyswitch-hole-y h)
       ;; ALPS-specific space for wings to flare out.
       (if (= keyswitch-style :alps)
         (translate [0 0 -1.5]
                    (cube (+ keyswitch-hole-x 1) keyswitch-hole-y t)))))))

(defn single-switch-nubs
  "MX-specific nubs that hold the keyswitch in place."
  [getopt]
  (let [t (getopt :case :key-mount-thickness)
        keyswitch-hole-x (getopt :switches :derived :keyswitch-hole-x)
        keyswitch-hole-y (getopt :switches :derived :keyswitch-hole-y)
        nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                 (rotate (/ π 2) [1 0 0])
                 (translate [(+ (/ keyswitch-hole-x 2)) 0 1])
                 (hull (->> (cube 1.5 2.75 t)
                            (translate [(+ (/ 1.5 2) (/ keyswitch-hole-y 2))
                                        0
                                        (/ t 2)]))))]
    (union nub
           (->> nub
                (mirror [1 0 0])
                (mirror [0 1 0])))))

(defn web-post
  "A shape for attaching things to a corner of a switch mount."
  [getopt]
  (cube (getopt :case :key-mount-corner-margin)
        (getopt :case :key-mount-corner-margin)
        (getopt :case :web-thickness)))

(defn mount-corner-post
  "A post shape that comes offset for one corner of a key mount."
  [getopt directions]
  (translate (place/mount-corner-offset getopt directions) (web-post getopt)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interface Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster-plates [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (single-switch-plate getopt))
                    (derived getopt cluster :key-coordinates))))

(defn cluster-cutouts [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (single-switch-cutout getopt))
                    (derived getopt cluster :key-coordinates))))

(defn cluster-nubs [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (single-switch-nubs getopt))
                    (derived getopt cluster :key-coordinates))))

(defn cluster-channels [getopt cluster]
  (letfn [(modeller [coord]
            (letfn [(most [path]
                      (most-specific getopt path cluster coord))]
              (negative-cap-shape getopt
                {:top-width (most [:channel :top-width])
                 :height (most [:channel :height])
                 :margin (most [:channel :margin])})))]
    (apply union (map #(place/cluster-place getopt cluster % (modeller %))
                      (derived getopt cluster :key-coordinates)))))

(defn cluster-keycaps [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (keycap-model getopt 1))
                    (derived getopt cluster :key-coordinates))))

(defn metacluster
  "Apply passed modelling function to all key clusters."
  [function getopt]
  (apply union (map #(function getopt %) (all-clusters getopt))))
