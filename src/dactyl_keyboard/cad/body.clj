;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Keyboard Case Model                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.body
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [mean]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.threaded :as threaded]
            [scad-tarmi.util :refer [loft]]
            [dactyl-keyboard.generics :refer [NNE ENE ESE WSW WNW NNW colours]]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.cad.key :as key]
            [dactyl-keyboard.param.access :refer [most-specific]]))


;;;;;;;;;;;;;
;; Masking ;;
;;;;;;;;;;;;;

(defn mask
  "Implement overall limits on passed shapes."
  [getopt with-plate & shapes]
  (let [plate (if with-plate (getopt :case :bottom-plate :thickness) 0)]
    (intersection
      (maybe/translate [0 0 plate]
        (translate (getopt :mask :center) (apply cube (getopt :mask :size))))
      (apply union shapes))))


;;;;;;;;;;;;;;;;;;;;;;;
;; Key Mount Webbing ;;
;;;;;;;;;;;;;;;;;;;;;;;

;; This connects switch mount plates to one another.

(defn web-shapes
  "A vector of shapes covering the interstices between points in a matrix."
  [coordinate-sequence spotter placer corner-finder]
  (loop [remaining-coordinates coordinate-sequence
         shapes []]
    (if (empty? remaining-coordinates)
      shapes
      (let [coord-here (first remaining-coordinates)
            coord-north (matrix/walk coord-here :north)
            coord-east (matrix/walk coord-here :east)
            coord-northeast (matrix/walk coord-here :north :east)
            fill-here (spotter coord-here)
            fill-north (spotter coord-north)
            fill-east (spotter coord-east)
            fill-northeast (spotter coord-northeast)]
       (recur
         (rest remaining-coordinates)
         (conj
           shapes
           ;; Connecting columns.
           (when (and fill-here fill-east)
             (loft 3
               [(placer coord-here (corner-finder ENE))
                (placer coord-east (corner-finder WNW))
                (placer coord-here (corner-finder ESE))
                (placer coord-east (corner-finder WSW))]))
           ;; Connecting rows.
           (when (and fill-here fill-north)
             (loft 3
               [(placer coord-here (corner-finder WNW))
                (placer coord-north (corner-finder WSW))
                (placer coord-here (corner-finder ENE))
                (placer coord-north (corner-finder ESE))]))
           ;; Selectively filling the area between all four possible mounts.
           (loft 3
             [(when fill-here (placer coord-here (corner-finder ENE)))
              (when fill-north (placer coord-north (corner-finder ESE)))
              (when fill-east (placer coord-east (corner-finder WNW)))
              (when fill-northeast (placer coord-northeast (corner-finder WSW)))])))))))

(defn walk-and-web [columns rows spotter placer corner-finder]
  (web-shapes (matrix/coordinate-pairs columns rows) spotter placer corner-finder))

(defn cluster-web [getopt cluster]
  (apply union
    (walk-and-web
      (getopt :key-clusters :derived :by-cluster cluster :column-range)
      (getopt :key-clusters :derived :by-cluster cluster :row-range)
      (getopt :key-clusters :derived :by-cluster cluster :key-requested?)
      (partial place/cluster-place getopt cluster)
      (partial key/mount-corner-post getopt))))


;;;;;;;;;;;;;;;;;;;
;; Wall-Building ;;
;;;;;;;;;;;;;;;;;;;

;; Functions for specifying parts of a perimeter wall. These all take the
;; edge-walking algorithm’s map output with position and direction, upon
;; seeing the need for each part.

(defn wall-straight-body
  "The part of a case wall that runs along the side of a key mount on the
  edge of the board."
  [{:keys [coordinates direction]}]
  (let [facing (matrix/left direction)]
    [[coordinates facing matrix/right] [coordinates facing matrix/left]]))

(defn wall-straight-join
  "The part of a case wall that runs between two key mounts in a straight line."
  [{:keys [coordinates direction]}]
  (let [next-coord (matrix/walk coordinates direction)
        facing (matrix/left direction)]
    [[coordinates facing matrix/right] [next-coord facing matrix/left]]))

(defn wall-outer-corner
  "The part of a case wall that smooths out an outer, sharp corner."
  [{:keys [coordinates direction]}]
  (let [original-facing (matrix/left direction)]
    [[coordinates original-facing matrix/right]
     [coordinates direction matrix/left]]))

(defn wall-inner-corner
  "The part of a case wall that covers any gap in an inner corner.
  In this case, it is import to pick not only the right corner but the right
  direction moving out from that corner."
  [{:keys [coordinates direction]}]
  (let [opposite (matrix/walk coordinates (matrix/left direction) direction)
        reverse (matrix/left (matrix/left direction))]
    [[coordinates (matrix/left direction) (constantly direction)]
     [opposite reverse matrix/left]]))

(defn connecting-wall
  [{:keys [corner] :as position}]
  (case corner
    :outer (wall-outer-corner position)
    nil (wall-straight-join position)
    :inner (wall-inner-corner position)))

;; Edge walking.

(defn wall-edge-post
  "Run wall-edge-sequence with a web post as its subject."
  [getopt cluster upper edge]
  (place/wall-edge-sequence getopt cluster upper edge (key/web-post getopt)))

(defn wall-slab
  "Produce a single shape joining some (two) edges."
  [getopt cluster edges]
  (let [upper (map (partial wall-edge-post getopt cluster true) edges)
        lower (map (partial wall-edge-post getopt cluster false) edges)]
   (union
     (apply hull upper)
     (apply misc/bottom-hull lower))))

(defn cluster-wall
  "Walk the edge of a key cluster, walling it in."
  [getopt cluster]
  (apply union
    (reduce
      (fn [coll position]
        (conj coll
          (wall-slab getopt cluster (wall-straight-body position))
          (wall-slab getopt cluster (connecting-wall position))))
      []
      (matrix/trace-between
        (getopt :key-clusters :derived :by-cluster cluster :key-requested?)))))


;;;;;;;;;;;;;;;;;;
;; Rear Housing ;;
;;;;;;;;;;;;;;;;;;

(defn- housing-cube [getopt]
  (let [t (getopt :case :web-thickness)]
   (cube t t t)))

(defn housing-properties
  "Derive characteristics from parameters for the rear housing."
  [getopt]
  (let [cluster (getopt :case :rear-housing :position :cluster)
        row (last (getopt :key-clusters :derived :by-cluster cluster :row-range))
        coords (getopt :key-clusters :derived :by-cluster cluster :coordinates-by-row row)
        pairs (into [] (for [coord coords corner [NNW NNE]] [coord corner]))
        getpos (fn [[coord corner]]
                 (place/cluster-place getopt cluster coord
                   (place/mount-corner-offset getopt corner)))
        y-max (apply max (map #(second (getpos %)) pairs))
        getoffset (partial getopt :case :rear-housing :position :offsets)
        y-roof-s (+ y-max (getoffset :south))
        y-roof-n (+ y-roof-s (getoffset :north))
        z (getopt :case :rear-housing :height)
        roof-sw [(- (first (getpos (first pairs))) (getoffset :west)) y-roof-s z]
        roof-se [(+ (first (getpos (last pairs))) (getoffset :east)) y-roof-s z]
        roof-nw [(first roof-sw) y-roof-n z]
        roof-ne [(first roof-se) y-roof-n z]]
   {:west-end-coord (first coords)
    :east-end-coord (last coords)
    :coordinate-corner-pairs pairs
    ;; [x y z] coordinates of the corners of the topmost part of the roof:
    :sw roof-sw
    :se roof-se
    :nw roof-nw
    :ne roof-ne}))

(defn- housing-roof
  "A cuboid shape between the four corners of the rear housing’s roof."
  [getopt]
  (let [getcorner (partial getopt :case :rear-housing :derived)]
    (apply hull
      (map #(maybe/translate (getcorner %) (housing-cube getopt))
           [:nw :ne :se :sw]))))

(defn housing-pillar-functions
  "Make functions that determine the exact positions of rear housing walls.
  This is an awkward combination of reckoning functions for building the
  bottom plate in 2D and placement functions for building the case walls in
  3D. Because they’re specialized, the ultimate return values are disturbingly
  different."
  [getopt]
  (let [cluster (getopt :case :rear-housing :position :cluster)
        cluster-pillar
          (fn [coord-key direction housing-turning-fn cluster-turning-fn]
            ;; Make a function for a part of the cluster wall.
            (fn [reckon upper]
              (let [coord (getopt :case :rear-housing :derived coord-key)
                    subject (if reckon [0 0 0] (key/web-post getopt))
                    ;; For reckoning, return a 3D coordinate vector.
                    ;; For building, return a sequence of web posts.
                    picker (if reckon #(first (take-last 2 %)) identity)]
                (picker
                  (place/wall-edge-sequence getopt cluster upper
                    [coord direction housing-turning-fn] subject)))))
        housing-pillar
          (fn [opposite directions]
            ;; Make a function for a part of the rear housing.
            ;; For reckoning, return a 3D coordinate vector.
            ;; For building, return a hull of housing cubes.
            (fn [reckon upper]
              (let [subject
                      (if reckon
                        (place/housing-vertex-offset getopt
                          (if opposite
                            [(first directions)
                             (matrix/left (matrix/left (second directions)))]
                            directions))
                        (housing-cube getopt))]
                (apply (if reckon mean hull)
                  (map #(place/housing-place getopt directions % subject)
                       (if upper [0 1] [1]))))))]
    [(cluster-pillar :west-end-coord :west matrix/right matrix/left)
     (housing-pillar true WSW)
     (housing-pillar false WNW)
     (housing-pillar false NNW)
     (housing-pillar false NNE)
     (housing-pillar false ENE)
     (housing-pillar true ESE)
     (cluster-pillar :east-end-coord :east matrix/left matrix/right)]))

(defn- housing-wall-shape-level
  "The west, north and east walls of the rear housing with connections to the
  ordinary case wall."
  [getopt is-upper-level joiner]
  (loft
    (reduce
      (fn [coll function] (conj coll (joiner (function false is-upper-level))))
      []
      (housing-pillar-functions getopt))))

(defn- housing-outer-wall
  "The complete walls of the rear housing: Vertical walls and a bevelled upper
  level that meets the roof."
  [getopt]
  (union
    (housing-wall-shape-level getopt true identity)
    (housing-wall-shape-level getopt false misc/bottom-hull)))

(defn- housing-web
  "An extension of a key cluster’s webbing onto the roof of the rear housing."
  [getopt]
  (let [cluster (getopt :case :rear-housing :position :cluster)
        pos-corner (fn [coord corner]
                     (place/cluster-place getopt cluster coord
                       (place/mount-corner-offset getopt corner)))
        sw (getopt :case :rear-housing :derived :sw)
        se (getopt :case :rear-housing :derived :se)
        x (fn [coord corner]
            (max (first sw)
                 (min (first (pos-corner coord corner))
                      (first se))))
        y (second sw)
        z (getopt :case :rear-housing :height)]
   (loft
     (reduce
       (fn [coll [coord corner]]
         (conj coll
           (hull (place/cluster-place getopt cluster coord
                   (key/mount-corner-post getopt corner))
                 (translate [(x coord corner) y z]
                   (housing-cube getopt)))))
       []
       (getopt :case :rear-housing :derived :coordinate-corner-pairs)))))

(defn- housing-mount-place [getopt side shape]
  (let [d (getopt :case :rear-housing :fasteners :diameter)
        offset (getopt :case :rear-housing :fasteners side :offset)
        n (getopt :case :rear-housing :position :offsets :north)
        t (getopt :case :web-thickness)
        h (threaded/datum d :hex-nut-height)
        [sign base] (case side
                      :west [+ (getopt :case :rear-housing :derived :sw)]
                      :east [- (getopt :case :rear-housing :derived :se)])
        near (mapv + base [(+ (- (sign offset)) (sign d)) d (/ (+ t h) -2)])
        far (mapv + near [0 (- n d d) 0])]
   (hull
     (translate near shape)
     (translate far shape))))

(defn- housing-mount-positive [getopt side]
  (let [d (getopt :case :rear-housing :fasteners :diameter)
        w (* 2.2 d)]
   (housing-mount-place getopt side
     (cube w w (threaded/datum d :hex-nut-height)))))

(defn- housing-mount-negative [getopt side]
  (let [d (getopt :case :rear-housing :fasteners :diameter)
        compensator (getopt :dfm :derived :compensator)
        mount-side (* 2.2 d)]
   (union
     (housing-mount-place getopt side
       (cylinder (/ d 2) 20))
     (if (getopt :case :rear-housing :fasteners :bosses)
       (housing-mount-place getopt side
         (threaded/nut :iso-size d :compensator compensator :negative true))))))

(defn rear-housing
  "A squarish box at the far end of a key cluster."
  [getopt]
  (let [prop (partial getopt :case :rear-housing :fasteners)
        pair (fn [function]
               (union
                 (if (prop :west :include) (function getopt :west))
                 (if (prop :east :include) (function getopt :east))))]
   (difference
     (union
       (housing-roof getopt)
       (housing-web getopt)
       (housing-outer-wall getopt)
       (if (prop :bosses) (pair housing-mount-positive)))
     (pair housing-mount-negative))))


;;;;;;;;;;;;;;;;;;;
;; Tweak Plating ;;
;;;;;;;;;;;;;;;;;;;

(defn- tweak-posts
  "(The hull of) one or more corner posts from a single key mount."
  [getopt alias directions first-segment last-segment]
  (if (= first-segment last-segment)
    (place/reckon-from-anchor getopt alias
      {:subject (key/web-post getopt), :corner directions, :segment first-segment})
    (apply hull (map #(tweak-posts getopt alias directions %1 %1)
                     (range first-segment (inc last-segment))))))

(declare tweak-plating)

(defn- tweak-map
  "Treat a map-type node in the configuration."
  [getopt node]
  (let [parts (get node :chunk-size)
        at-ground (get node :at-ground false)
        prefix (if (get node :highlight) -# identity)
        shapes (reduce (partial tweak-plating getopt) [] (:hull-around node))]
    (when (get node :above-ground true)
      (prefix
        (apply (if parts union (if at-ground misc/bottom-hull hull))
          (if parts
            (map (partial apply (if at-ground misc/bottom-hull hull))
                 (partition parts 1 shapes))
            shapes))))))

(defn- tweak-plating
  "A reducer."
  [getopt coll node]
  (conj coll
    (if (map? node)
      (tweak-map getopt node)
      (apply (partial tweak-posts getopt) node))))

(defn wall-tweaks
  "User-requested additional shapes."
  [getopt]
  (apply union
    (reduce (partial tweak-plating getopt) [] (getopt :case :tweaks))))
