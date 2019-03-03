;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Miscellaneous CAD Utilities                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Functions useful in more than one scad-clj project.

(ns dactyl-keyboard.cad.misc
  (:require [scad-clj.model :as model]
            [scad-tarmi.maybe :as maybe]))


(defn pad-to-3d
  "Pad a coordinate vector to three dimensions."
  ([coordinates] (pad-to-3d coordinates 0))
  ([coordinates padding]
   {:pre [(vector? coordinates)
          (<= (count coordinates) 3)]}
   (if (= (count coordinates) 3)
     coordinates
     (pad-to-3d (conj coordinates padding) padding))))

(defn z0
  "No relation to Cicero."
  [coordinates]
  {:pre [(vector? coordinates)]}
  (pad-to-3d (subvec coordinates 0 2) 0))

(defn bottom-extrusion [height p]
  (model/extrude-linear {:height height, :twist 0, :convexity 0, :center false}
    (model/project p)))

(defn bottom-hull [& p]
  (model/hull p (bottom-extrusion 0.001 p)))

(defn swing-callables
  "Rotate passed object with passed radius, not around its own axes.
  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.
  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that."
  [translator radius rotator obj]
  (if (vector? rotator)
    (if (= (count rotator) 3)
      (swing-callables translator radius (partial maybe/rotate rotator) obj)
      (swing-callables translator radius
        (partial maybe/rotate (first rotator) (second rotator))
        obj))
    ;; Else assume the rotator is usable as a function and apply it.
    (->> obj
      (translator [0 0 (- radius)])
      rotator
      (translator [0 0 radius]))))
