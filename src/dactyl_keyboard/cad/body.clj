;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Keyboard Case Model                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.body
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.params :refer :all]
            [dactyl-keyboard.cad.misc :refer :all]
            [dactyl-keyboard.cad.matrix :refer :all]
            [dactyl-keyboard.cad.key :refer :all]))


;;;;;;;;;;;;;;;;;;;;;;;
;; Key Mount Webbing ;;
;;;;;;;;;;;;;;;;;;;;;;;

;; This connects switch mount plates to one another.

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
              (placer coord-here (corner-finder ENE))
              (placer coord-here (corner-finder ESE))
              (placer coord-east (corner-finder WNW))
              (placer coord-east (corner-finder WSW))))
           ;; Connecting rows.
           (if (and fill-here fill-north)
             (hull
              (placer coord-here (corner-finder WNW))
              (placer coord-here (corner-finder ENE))
              (placer coord-north (corner-finder WSW))
              (placer coord-north (corner-finder ESE))))
           ;; Selectively filling the area between all four possible mounts.
           (hull
             (if fill-here (placer coord-here (corner-finder ENE)))
             (if fill-north (placer coord-north (corner-finder ESE)))
             (if fill-east (placer coord-east (corner-finder WNW)))
             (if fill-northeast (placer coord-northeast (corner-finder WSW))))))))))

(defn walk-and-web [columns rows spotter placer corner-finder]
  (web-shapes (coordinate-pairs columns rows) spotter placer corner-finder))

(defn finger-web [getopt]
  (apply union
    (walk-and-web
      (getopt :key-clusters :finger :derived :column-range)
      (getopt :key-clusters :finger :derived :row-range)
      (getopt :key-clusters :finger :derived :key-requested?)
      (partial cluster-place getopt :finger)
      mount-corner-post)))

(defn thumb-web [getopt]
  (apply union
    (walk-and-web
      (getopt :key-clusters :thumb :derived :column-range)
      (getopt :key-clusters :thumb :derived :row-range)
      (getopt :key-clusters :thumb :derived :key-requested?)
      (partial cluster-place getopt :thumb)
      mount-corner-post)))


;;;;;;;;;;;;;;;;;;;
;; Wall-Building ;;
;;;;;;;;;;;;;;;;;;;

(defn wall-segment-offset [getopt cluster coord cardinal-direction segment]
  (let [most #(most-specific-option getopt (concat [:wall] %) cluster coord)
        thickness (most [:thickness])
        bevel-factor (most [:bevel])
        parallel (most [cardinal-direction :parallel])
        perpendicular (most [cardinal-direction :perpendicular])
        {dx :dx dy :dy} (cardinal-direction compass-to-grid)
        bevel
          (if (zero? perpendicular)
            bevel-factor
            (* bevel-factor
               (/ perpendicular (abs perpendicular))))]
   (case segment
     0 [0 0 0]
     1 [(* dx thickness) (* dy thickness) bevel]
     2 [(* dx parallel) (* dy parallel) perpendicular]
     3 [(* dx (+ parallel thickness))
        (* dy (+ parallel thickness))
        perpendicular]
     4 [(* dx (+ parallel))
        (* dy (+ parallel))
        (+ perpendicular bevel)])))

(defn wall-corner-offset [getopt cluster coordinates directions]
  "Combined [x y z] offset from the center of a switch mount.
  This goes to one corner of the hem of the mount’s skirt of walling
  and is used mainly for finding the base of full walls."
  (vec
    (map +
      (wall-segment-offset getopt cluster coordinates (first directions) 3)
      (mount-corner-offset directions))))

(defn wall-corner-position [getopt cluster coordinates directions]
  "Absolute position of the lower wall around a finger key."
  (cluster-position getopt cluster coordinates
    (if (nil? directions)
      [0 0 0]
      (wall-corner-offset getopt cluster coordinates directions))))

(defn wall-slab-center-offset [getopt cluster coordinates direction]
  "Combined [x y z] offset to the center of a vertical wall.
  Computed as the arithmetic average of its two corners."
  (letfn [(c [turning-fn]
            (wall-corner-offset getopt cluster coordinates
              [direction (turning-fn direction)]))]
    (vec (map / (vec (map + (c turning-left) (c turning-right))) [2 2 2]))))

;; Functions for specifying parts of a perimeter wall. These all take the
;; edge-walking algorithm’s position and direction upon seeing the need for
;; each part.

(defn wall-straight-body [[coordinates direction]]
  "The part of a case wall that runs along the side of a key mount on the
  edge of the board."
  (let [facing (turning-left direction)]
    [[coordinates facing turning-right]
     [coordinates facing turning-left]]))

(defn wall-straight-join [[coordinates direction]]
  "The part of a case wall that runs between two key mounts in a straight line."
  (let [next-coord (walk-matrix coordinates direction)
        facing (turning-left direction)]
    [[coordinates facing turning-right]
     [next-coord facing turning-left]]))

(defn wall-outer-corner [[coordinates direction]]
  "The part of a case wall that smooths out an outer, sharp corner."
  (let [original-facing (turning-left direction)]
    [[coordinates original-facing turning-right]
     [coordinates direction turning-left]]))

(defn wall-inner-corner [[coordinates direction]]
  "The part of a case wall that covers any gap in an inner corner.
  In this case, it is import to pick not only the right corner but the right
  direction moving out from that corner."
  (let [opposite (walk-matrix coordinates (turning-left direction) direction)
        reverse (turning-left (turning-left direction))]
     [[coordinates (turning-left direction) (fn [_] direction)]
      [opposite reverse turning-left]]))

;; Edge walking.

(defn wall-edge [getopt cluster upper [coord direction turning-fn]]
  "Produce a sequence of corner posts for the upper or lower part of the edge
  of one wall slab."
  (let [extent (most-specific-option getopt [:wall direction :extent]
                 cluster coord)
        last-upper-segment (case extent :full 4 :none 0 extent)
        to-ground (= extent :full)
        corner [direction (turning-fn direction)]
        offsetter (partial wall-segment-offset getopt cluster coord direction)
        post (fn [segment]
               (->> (mount-corner-post corner)
                    (translate (offsetter segment))
                    (cluster-place getopt cluster coord)))]
   (if-not (zero? last-upper-segment)
     (if upper
       (map post (range (inc last-upper-segment)))
       (if (= extent :full)
         (map post [2 3 4]))))))

(defn wall-slab [getopt cluster edges]
  "Produce a single shape joining some (two) edges."
  (let [upper (map (partial wall-edge getopt cluster true) edges)
        lower (map (partial wall-edge getopt cluster false) edges)]
   (union
     (apply hull upper)
     (apply bottom-hull lower))))

(defn cluster-wall [getopt cluster]
  "Walk the edge of a key cluster clockwise. Wall it in."
  (let [prop (partial getopt :key-clusters cluster :derived)
        occlusion-fn (prop :key-requested?)
        start [[0 0] :north]
        mason (fn [edge-locator place-and-direction]
                (wall-slab getopt cluster (edge-locator place-and-direction)))]
    (assert (occlusion-fn (first start)))
    (loop [place-and-direction start shapes []]
      (let [[coordinates direction] place-and-direction
            left (walk-matrix coordinates (turning-left direction))
            ahead (walk-matrix coordinates direction)
            ahead-left (walk-matrix coordinates direction (turning-left direction))
            landscape (vec (map occlusion-fn [left ahead-left ahead]))
            situation (case landscape
                        [false false false] :outer-corner
                        [false false true] :straight
                        [false true true] :inner-corner
                        (throw (Exception.
                                 (format "Unforeseen landscape at %s: %s"
                                   place-and-direction landscape))))]
        (if (and (= place-and-direction start) (not (empty? shapes)))
          (apply union shapes)
          (recur
            (case situation
              ; In an outer corner, turn right in place.
              ; In an inner corner, jump diagonally ahead-left, turning left.
              :outer-corner [coordinates (turning-right direction)]
              :straight     [ahead direction]
              :inner-corner [ahead-left (turning-left direction)])
            (conj
              shapes
              (mason wall-straight-body place-and-direction)
              (mason (case situation :outer-corner wall-outer-corner
                                     :straight wall-straight-join
                                     :inner-corner wall-inner-corner)
                     place-and-direction))))))))


;;;;;;;;;;;;;;;;;;;
;; Tweak Plating ;;
;;;;;;;;;;;;;;;;;;;

(defn- tweak-posts [getopt key-alias directions first-segment last-segment]
  "(The hull of) one or more corner posts from a single key mount."
  (if (= first-segment last-segment)
    (let [key (getopt :key-clusters :derived :aliases key-alias)
          {cluster :cluster coordinates :coordinates} key
          offset (wall-segment-offset getopt cluster coordinates
                  (first directions) first-segment)]
     (->> (mount-corner-post directions)
          (translate offset)
          (cluster-place getopt cluster coordinates)))
    (apply hull (map #(tweak-posts getopt key-alias directions %1 %1)
                     (range first-segment (inc last-segment))))))

(declare tweak-plating)

(defn- tweak-map [getopt node]
  "Treat a map-type node in the configuration."
  (let [parts (get node :chunk-size)
        to-ground (get node :to-ground false)
        combo (or to-ground parts)
        prefix (if (get node :highlight) -# identity)
        shapes (reduce (partial tweak-plating getopt) [] (:hull-around node))]
   (prefix
     (apply (if parts union (if to-ground bottom-hull hull))
       (if parts
         (map (partial apply (if to-ground bottom-hull hull))
              (partition parts 1 shapes))
         shapes)))))

(defn- tweak-plating [getopt coll node]
  "A reducer."
  (conj coll
    (if (map? node)
      (tweak-map getopt node)
      (apply (partial tweak-posts getopt) node))))

(defn wall-tweaks [getopt]
  "User-requested additional shapes."
  (apply union
    (reduce (partial tweak-plating getopt) [] (getopt :case :tweaks))))
