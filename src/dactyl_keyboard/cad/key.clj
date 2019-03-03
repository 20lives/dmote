;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Key Utilities — Switches and Keycaps                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.key
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [abs π]]
            [scad-tarmi.util :refer [loft]]
            [dactyl-keyboard.generics :as generics]
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

(defn collect-key-aliases
  "Unify cluster-specific key aliases into a single global map that preserves
  their cluster of origin and resolves symbolic coordinates to absolute values."
  [getopt]
  (into {}
    (mapcat
      (fn [cluster]
        (into {}
          (map
            (fn [[alias flex]]
              [alias {:type :key
                      :cluster cluster
                      :coordinates (resolve-flex getopt cluster flex)}]))
          (getopt :key-clusters cluster :aliases)))
      (all-clusters getopt))))

(defn derive-switch-properties [getopt]
  "Store switch properties needed outside of switch cutout models."
  (case (getopt :switches :style)
    :alps
      {:hole-x 15.5
       :hole-y 12.6}
    :mx
      (let [hole 13.9954]
        {:hole-x hole
         :hole-y hole})))

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

(defn- cap-clearance
  "Compute keycap clearance from key mounting plate."
  [getopt cluster coord
   & {:keys [from state] :or {from :plate-top, state :resting}}]
  (case from
    :plate-top
      (case state
        :resting
          (most-specific getopt [:keycap :resting-clearance] cluster coord)
        :pressed (- (cap-clearance getopt cluster coord)
                    (getopt :switches :travel)))
    :plate-bottom
      (+ (cap-clearance getopt cluster coord :from :plate-top :state state)
         (getopt :case :key-mount-thickness))))

(defn cap-channel-negative
  "The shape of a channel for a keycap to move in."
  [getopt cluster coord {h3 :height wd3 :top-width m :margin}]
  (let [step (fn [w d h] (translate [0 0 h] (cube w d 1)))
        wk (+ (getopt :switches :derived :hole-x) m)
        dk (+ (getopt :switches :derived :hole-y) m)
        most #(most-specific getopt % cluster coord)
        waist #(+ (place/key-length (most [:keycap %])) m)
        w1 (waist :width)
        d1 (waist :depth)
        h1 (cap-clearance getopt cluster coord :state :pressed)
        h2 (cap-clearance getopt cluster coord :from :plate-top :state :pressed)]
   (color (:cap-negative generics/colours)
     (translate [0 0 (getopt :case :key-mount-thickness)]
       (loft
         [(step wk dk 0.5) ; A bottom plate for ease of mounting a switch.
          (step w1 d1 h1) ; Space for the keycap’s edges in travel.
          (step w1 d1 h2)
          (step wd3 wd3 h3)]))))) ; Space for the upper body of a keycap at rest.

(defn cap-positive
  "The shape of one keycap. Rectangular base, at rest, size measured in units."
  [getopt cluster coord]
  (let [most #(most-specific getopt [:keycap %] cluster coord)]
   (->>
     (apply square (map place/key-length [(most :width) (most :depth)]))
     (extrude-linear  ; Scale based on DSA incline.
       {:height (most :body-height), :center false, :scale 0.73})
     (translate [0 0 (cap-clearance getopt cluster coord)])
     (color (:cap-body generics/colours)))))


;;;;;;;;;;;;;;;;;;;;;;
;; Keyswitch Models ;;
;;;;;;;;;;;;;;;;;;;;;;

;; These models are intended solely for use as cutouts and therefore lack
;; features that would not interact with key mounts.

(defn- plate-cutout-height
  [getopt switch-height-to-plate-top]
  (- (* 2 switch-height-to-plate-top) (getopt :case :key-mount-thickness)))

(defn alps-switch
  "One ALPS-compatible cutout model."
  [getopt]
  (let [thickness (getopt :case :key-mount-thickness)
        hole-x (getopt :switches :derived :hole-x)
        hole-y (getopt :switches :derived :hole-y)
        overhang-x 17.25  ; Width of notches.
        overhang-y 14.25]
    (translate [0 0 (/ (getopt :case :key-mount-thickness) 2)]
      (union
        ;; Space for the part of a switch above the mounting hole.
        ;; The actual height of the notches is 1 mm, and it’s not a full cuboid.
        (translate [0 0 thickness]
          (cube overhang-x overhang-y thickness))
        ;; The hole through the plate.
        (cube hole-x hole-y (plate-cutout-height getopt 4.5))
        ;; ALPS-specific space for wings to flare out.
        (translate [0 0 -1.5]
          (cube (inc hole-x) hole-y thickness))))))

(defn mx-switch
  "One MX Cherry-compatible cutout model."
  [getopt]
  (let [thickness (getopt :case :key-mount-thickness)
        hole (getopt :switches :derived :hole-x)  ; MX holes are square.
        overhang 15.494
        nub (->> (cylinder 1 2.75)
                 (with-fn 20)
                 (rotate [(/ π 2) 0 0])
                 (translate [(+ (/ hole 2)) 0 1])
                 (hull
                   (translate [(+ 3/4 (/ hole 2)) 0 (/ thickness 2)]
                     (cube 1.5 2.75 thickness))))]
    (difference
      (translate [0 0 (/ (getopt :case :key-mount-thickness) 2)]
        (union
          ;; Space for the part of a switch above the mounting hole.
          (translate [0 0 thickness]
            (cube overhang overhang thickness))
          ;; The hole through the plate.
          (cube hole hole (plate-cutout-height getopt 5.004))))
      ;; MX-specific nubs that hold the keyswitch in place.
      nub
      (mirror [0 1 0] (mirror [1 0 0] nub)))))

(defn single-switch
  "Negative space for the insertion of a key switch through a mounting plate."
  [getopt]
  (case (getopt :switches :style)
    :alps (alps-switch getopt)
    :mx (mx-switch getopt)))


;;;;;;;;;;;;;;;;;;
;; Other Models ;;
;;;;;;;;;;;;;;;;;;

(defn- single-plate [getopt]
  (let [t (getopt :case :key-mount-thickness)]
   (translate [0 0 (/ t 2)]
     (cube place/mount-width place/mount-depth t))))

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
                       (single-plate getopt))
                    (derived getopt cluster :key-coordinates))))

(defn cluster-cutouts [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (single-switch getopt))
                    (derived getopt cluster :key-coordinates))))

(defn cluster-channels [getopt cluster]
  (letfn [(modeller [coord]
            (letfn [(most [path]
                      (most-specific getopt path cluster coord))]
              (cap-channel-negative getopt cluster coord
                {:top-width (most [:channel :top-width])
                 :height (most [:channel :height])
                 :margin (most [:channel :margin])})))]
    (apply union (map #(place/cluster-place getopt cluster % (modeller %))
                      (derived getopt cluster :key-coordinates)))))

(defn cluster-keycaps [getopt cluster]
  (apply union (map #(place/cluster-place getopt cluster %
                       (cap-positive getopt cluster %))
                    (derived getopt cluster :key-coordinates))))

(defn metacluster
  "Apply passed modelling function to all key clusters."
  [function getopt]
  (apply union (map #(function getopt %) (all-clusters getopt))))
