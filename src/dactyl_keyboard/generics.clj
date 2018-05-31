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

(defn string-corner [string]
  "For use with YAML, where string values are not automatically converted."
  (let [directions ((keyword string) keyword-to-directions)]
   (if (nil? directions)
     (do (println (format "Unknown corner ID string: “%s”." string))
         (System/exit 1))
     directions)))

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

(defn soft-merge [& maps]
  "Take mappings. Merge them depth-first so as to retain all leaves
  from a mapping except where specifically overridden by the next."
  (letfn [(f [old new]
            (if (map? old)
              (soft-merge old new)
              new))]
   (apply (partial merge-with f) maps)))
