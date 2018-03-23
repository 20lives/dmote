;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Keyboard Case Model                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.case
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
  (remove nil?
    (web-shapes (coordinate-pairs columns rows) spotter placer corner-finder)))

(def finger-web
  (apply union
    (walk-and-web
      all-finger-columns
      all-finger-rows
      finger?
      finger-key-place
      mount-corner-post)))

(def thumb-web
  (apply union
    (walk-and-web
      all-thumb-columns
      all-thumb-rows
      thumb?
      thumb-key-place
      mount-corner-post)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wall-Building Utilities ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wall-segment-offset [segment cardinal-direction [xy-offset z-offset]]
  (let [{dx :dx dy :dy} (cardinal-direction compass-to-grid)]
   (case segment
     0 [0 0 0]
     1 [(* dx wall-thickness)
        (* dy wall-thickness)
        (/ z-offset (abs z-offset))]
     2 [(* dx xy-offset)
        (* dy xy-offset)
        z-offset]
     3 [(* dx (+ xy-offset wall-thickness))
        (* dy (+ xy-offset wall-thickness))
        z-offset]
     4 [(* dx (+ xy-offset))
        (* dy (+ xy-offset))
        (+ z-offset (/ z-offset (abs z-offset)))]
        )))

(defn wall-element [segment [placer direction post offsets]]
  (placer (translate (wall-segment-offset segment direction offsets) post)))

(defn dropping-bevel [point0 point1]
  "The bevelled portion of a wall at the very top."
  (hull
    (wall-element 0 point0)
    (wall-element 1 point0)
    (wall-element 0 point1)
    (wall-element 1 point1)))

(defn wall-skirt [point0 point1]
  "The portion of a wall that follows what it’s built around."
  (hull
    (wall-element 0 point0)
    (wall-element 1 point0)
    (wall-element 2 point0)
    (wall-element 3 point0)
    (wall-element 0 point1)
    (wall-element 1 point1)
    (wall-element 2 point1)
    (wall-element 3 point1)))

(defn wall-hem [point0 point1]
  "The hem of a wall’s skirt including a vertical section to the floor."
  (bottom-hull
    (wall-element 2 point0)
    (wall-element 3 point0)
    (wall-element 2 point1)
    (wall-element 3 point1)))

(defn wall-to-ground [point0 point1]
  (union
    (wall-skirt point0 point1)
    (wall-hem point0 point1)))

(defn finger-wall-corner-offset [coordinates directions]
  "Combined [x y z] offset from the center of a switch mount.
  This goes to one corner of the hem of the mount’s skirt of walling
  and is used mainly for finding the base of walls."
  (vec
    (map +
      (wall-segment-offset
        3 (first directions) (finger-key-wall-offsets coordinates directions))
      (mount-corner-offset directions))))

(defn finger-wall-corner-position [coordinates directions]
  "Absolute position of the lower wall around a finger key."
  (finger-key-position coordinates
    (finger-wall-corner-offset coordinates directions)))

(defn finger-wall-offset [coordinates direction]
  "Combined [x y z] offset to the center of a wall.
  Computed as the arithmetic average of its two corners."
  (letfn [(c [turn]
            (finger-wall-corner-offset coordinates [direction (turn direction)]))]
    (vec (map / (vec (map + (c turning-left) (c turning-right))) [2 2 2]))))

(defn key-wall-deref [placer offsetter post-finder [coordinates direction turn]]
  (let [corner [direction (turn direction)]]
   [(partial placer coordinates)
     direction
     (post-finder corner)
     (offsetter coordinates corner)]))

(defn bevel-only [placer offsetter post-finder anchors]
  (apply dropping-bevel (map (partial key-wall-deref placer offsetter post-finder) anchors)))

(defn key-wall-skirt-only [placer offsetter post-finder anchors]
  (apply wall-skirt (map (partial key-wall-deref placer offsetter post-finder) anchors)))

(defn key-wall-to-ground [placer offsetter post-finder anchors]
  (apply wall-to-ground (map (partial key-wall-deref placer offsetter post-finder) anchors)))

;; Functions for specifying parts of a perimeter wall. These all take the
;; edge-walking algorithm’s position and direction upon seeing the need for
;; each part.

(defn wall-straight-body [[coordinates direction]]
  "The part of a case wall that runs along the side of a key mount on and edge of the board."
  (let [facing (turning-left direction)]
    [[coordinates facing turning-right] [coordinates facing turning-left]]))

(defn wall-straight-join [[coordinates direction]]
  "The part of a case wall that runs between two key mounts in a straight line."
  (let [next-coord (walk-matrix coordinates direction)
        facing (turning-left direction)]
    [[coordinates facing turning-right] [next-coord facing turning-left]]))

(defn wall-outer-corner [[coordinates direction]]
  "The part of a case wall that smooths out an outer, sharp corner."
  (let [original-facing (turning-left direction)]
    [[coordinates original-facing turning-right] [coordinates direction turning-left]]))

(defn wall-inner-corner [[coordinates direction]]
  "The part of a case wall that covers any gap in an inner corner."
  (let [opposite (walk-matrix coordinates (turning-left direction) direction)
        reverse (turning-left (turning-left direction))]
    [[coordinates direction turning-left] [opposite reverse turning-left]]))

;; Edge walking.

(defn walk-and-wall [start stop occlusion-fn bracer]
  "Walk the edge of the populated key matrix clockwise. Wall it in.

  Return a vector of shapes.

  Stop when the passed terminus function meets the current position.
  Assume the matrix doesn’t have any holes in it.

  "
  (loop [place-and-direction start
         shapes []]
    (let [[coordinates direction] place-and-direction
          left (walk-matrix coordinates (turning-left direction))
          ahead (walk-matrix coordinates direction)
          ahead-left (walk-matrix coordinates direction (turning-left direction))
          landscape (vec (map occlusion-fn [left ahead-left ahead]))
          situation (case landscape
            [false false false] :outer-corner
            [false false true] :straight
            [false true true] :inner-corner
            (throw (Exception. (str "Unforeseen landscape at " place-and-direction ": " landscape))))]
      (if (and (= place-and-direction stop) (not (empty? shapes)))
        shapes
        (recur
          (case situation
            :outer-corner  ; Turn right in place.
              [coordinates (turning-right direction)]
            :straight
              [ahead direction]
            :inner-corner  ; Jump diagonally ahead-left while also turning left.
              [ahead-left (turning-left direction)])
          (conj
            shapes
            (bracer (wall-straight-body place-and-direction))
            (case situation
              :outer-corner
                (bracer (wall-outer-corner place-and-direction))
              :straight
                (bracer (wall-straight-join place-and-direction))
              :inner-corner
                (bracer (wall-inner-corner place-and-direction)))))))))

;;;;;;;;;;;;;;;;
;; Case Walls ;;
;;;;;;;;;;;;;;;;

;; Refer to tweaks.clj for the bridge between the finger and thumb clusters.

(def case-walls-for-the-fingers
  (apply union
    (walk-and-wall
      [(first-in-column 0) :north]
      [(first-in-column 2) :south]
      finger?
      (partial key-wall-to-ground
        finger-key-place finger-key-wall-offsets mount-corner-post))
    (walk-and-wall
      [(first-in-column 2) :south]
      [(first-in-column 2) :north]
      finger?
      (partial key-wall-skirt-only
        finger-key-place finger-key-wall-offsets mount-corner-post))))

(def case-walls-for-the-thumbs
  (apply union
    (walk-and-wall
      [[-1 0] :east]
      [[-1 -2] :north]
      thumb?
      (partial key-wall-skirt-only
        thumb-key-place thumb-key-wall-offsets mount-corner-post))))
