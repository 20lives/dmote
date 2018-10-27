;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Constants and Minor Utility Functions                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; These are potentially useful in parameters and have very little to do
;;; with CAD or the keyboard.

(ns dactyl-keyboard.generics
  (:require [unicode-math.core :refer :all]))

;; Each switch mount has four corners with offsets in two directions.
;; Capitals in symbol names are reserved for these shorthand definitions
;; of the four corners. In each case, the cardinal direction naming the side
;; of the key comes first. The second item names one end of that side.
(def NNE [:north :east])  ; North by north-east.
(def ENE [:east :north])
(def SSE [:south :east])
(def ESE [:east :south])
(def SSW [:south :west])
(def WSW [:west :south])
(def NNW [:north :west])
(def WNW [:west :north])

(def keyword-to-directions
  "Decode sets of directions from configuration data."
  {:NNE NNE
   :ENE ENE
   :SSE SSE
   :ESE ESE
   :SSW SSW
   :WSW WSW
   :NNW NNW
   :WNW WNW})

(defn directions-to-unordered-corner [tuple]
  "Reduce directional corner code to non-directional corner code, as
  used for rear housing."
  (cond
    (#{NNE ENE} tuple) :ne
    (#{SSE ESE} tuple) :se
    (#{SSW WSW} tuple) :sw
    (#{NNW WNW} tuple) :nw))

(defn abs [n]
  "The absolute of n."
  (max n (- n)))

(defn soft-merge [& maps]
  "Take mappings. Merge them depth-first so as to retain all leaves
  from a mapping except where specifically overridden by the next."
  (apply (partial merge-with
           (fn [old new] (if (map? old) (soft-merge old new) new)))
         maps))
