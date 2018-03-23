;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Miscellaneous CAD Utilities                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Functions useful in more than one scad-clj project.

(ns dactyl-keyboard.cad.misc
  (:require [clojure.core.matrix]
            [scad-clj.model :exclude [use import] :refer :all]))

(defn pairwise-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 2 1 shapes))))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(defn bottom-extrusion [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom-extrusion 0.001 p)))

(defn pair-bottom-hulls [& shapes]
  (apply union
         (map (partial apply bottom-hull)
              (partition 2 1 shapes))))

(defn rotate-around-x [angle position]
  (clojure.core.matrix/mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (clojure.core.matrix/mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn rotator-vector [[x y z]]
  "Create an anonymous rotation function for passed vector of three angles.
  This emulates OpenSCAD’s rotate(a=[...]). scad-clj’s ‘rotatev’ was unable to
  implement that form in version 0.4.0 of the module."""
  (fn [obj]
    (->> obj
      (rotate x [1 0 0])
      (rotate y [0 1 0])
      (rotate z [0 0 1]))))

(defn swing-callables [translator radius rotator obj]
  "Rotate passed object with passed radius, not around its own axes.

  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.

  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that."
  (if (vector? rotator)
    (if (= (count rotator) 3)
      (swing-callables translator radius (rotator-vector rotator) obj)
      (swing-callables translator radius (partial rotate (first rotator) (second rotator)) obj))
    ;; Else assume the rotator is usable as a function and apply it.
    (->> obj
      (translator [0 0 (- radius)])
      rotator
      (translator [0 0 radius]))))

(defn swing-radius [radius rotator obj]
  "A simplification that uses ‘translate’."
  (swing-callables translate radius rotator obj))
