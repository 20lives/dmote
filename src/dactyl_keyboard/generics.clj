;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Constants and Minor Utility Functions                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; These are potentially useful in parameters and have very little to do
;;; with CAD or the keyboard.

(ns dactyl-keyboard.generics
  (:require [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  "Convert a number of degrees to radians."
  (* (/ degrees 180) π))

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

(defn abs [n]
  "The absolute of n."
  (max n (- n)))

(defn 𝒩 [x ￼￼μ σ]
  "The normal distribution’s probability density function with unicode-math."
  (let [v (ⁿ σ 2)]
    (* (/ 1 (√ (* 2 π v)))
       (ⁿ e (- (/ (ⁿ (- x ￼￼μ) 2) (* 2 v)))))))

(defn 𝒩′ [x ￼￼μ σ]
  "The first derivative of 𝒩."
  (* (/ (- x) (ⁿ σ 2)) (𝒩 x ￼￼μ σ)))
