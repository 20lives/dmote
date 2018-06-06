;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Tweaks and Workarounds                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module contains models that are too complicated for the params file
;;; and are dependent upon utilities that are in turn dependent upon the params
;;; file, yet may need to be adjusted following changes therein.

(ns dactyl-keyboard.tweaks
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.params :refer :all]
            [dactyl-keyboard.cad.misc :refer :all]
            [dactyl-keyboard.cad.matrix :refer :all]
            [dactyl-keyboard.cad.key :refer :all]
            [dactyl-keyboard.cad.body :refer :all]))


(defn- post [key directions segment & segments]
  "A convenience function for specifying a wall segment."
  (let [[getopt placer offsetter coordinates] key
        offsets (offsetter coordinates directions)]
   (if (empty? segments)
     (placer getopt coordinates
       (translate
         (wall-segment-offset segment (first directions) offsets)
         (mount-corner-post directions)))
     (apply hull (map (partial post key directions) (conj segments segment))))))

(defn key-cluster-bridge [getopt]
  "Walling and webbing between the thumb cluster and the finger cluster.
  This makes strict assumptions about the selected keyboard layout and is
  difficult to parameterize."
  (let [by-col (getopt :key-clusters :finger :derived :coordinates-by-column)
        first-row (fn [column] (first (by-col column)))
        f0 [getopt finger-key-place finger-key-wall-offsets (first-row 0)]
        f1 [getopt finger-key-place finger-key-wall-offsets (first-row 1)]
        f2 [getopt finger-key-place finger-key-wall-offsets (first-row 2)]
        t0 [getopt thumb-key-place thumb-key-wall-offsets [-1 -2]]
        t1 [getopt thumb-key-place thumb-key-wall-offsets [-1 -1]]
        t2 [getopt thumb-key-place thumb-key-wall-offsets [-1 0]]
        t3 [getopt thumb-key-place thumb-key-wall-offsets [0 0]]
        t4 [getopt thumb-key-place thumb-key-wall-offsets [0 -1]]
        t5 [getopt thumb-key-place thumb-key-wall-offsets [0 -2]]]
    (union
      ;; An extension of the wall of t0:
      (hull
        (post t0 WSW 0 1 3)
        (post t0 WNW 0 1))
      ;; An extension of the wall of t2:
      (hull
        (post t2 NNW 0 1 2 3 4)
        (post t2 WNW 0 1 2 3 4))
      ;; A big chunk where t2 looms over f0:
      (hull
        (post f0 SSE 0)
        (post f0 WSW 0)
        (post t1 WNW 1)
        (post t2 WSW 1)
        (post t2 WNW 1 2 3 4))
      ;; Joinery:
      (triangle-hulls
        (post f0 SSE 0)
        (post t2 NNW 4)
        (post f1 WNW 0 1)
        (post t2 NNE 4)
        (hull
          (post f1 WSW 0 1 2 3)
          (post f1 SSW 0 1 2))
        (post t3 WNW 4)
        (hull
          (post f1 SSE 0)
          (post f2 NNW 0))
        (hull
          (post f2 WSW 0 1)
          (post f2 SSW 0 1))
        (post t3 NNW 4)
        (hull
          (post t3 ENE 4)
          (post t3 NNE 4)))
      ;; Plating extending down the user-facing side.
      (triangle-hulls
        (post f2 SSW 0 1)
        (post f2 SSE 0 1)
        (post t3 NNE 4)
        (hull
          (post f2 SSE 3 4)
          (post f2 ESE 3 4))
        (post t3 ENE 4)
        (post t3 ESE 4)
        (post f2 ESE 3 4)
        (post f2 ENE 3 4))
      ;; The back plate of the outermost corner of t5:
      (hull
        (post t5 ENE 3 4)
        (post t5 ESE 3 4)
        (post t5 SSE 3 4)
        (post t5 SSW 3 4))
      ;; A large plate coming down the wall on the user-facing side.
      (hull
        (post f2 ENE 3 4)
        (post t3 SSE 4)
        (post t3 ESE 4)
        (post t5 NNE 4)
        (post t5 ENE 4))
      ;; Lower wall:
      (pair-bottom-hulls
        (hull
          (post f0 WSW 0 1 2 3)
          (post t2 WSW 1)
          (post t1 WNW 1)
          (post t0 WNW 1))
        (post t0 WSW 2 3)
        (post t0 ESE 2 3)
        (post t5 WSW 2 3)
        (post t5 ENE 4)
        (post f2 ENE 2 3 4)))))

(defn key-cluster-bridge-cutouts [getopt]
  (let [by-col (getopt :key-clusters :finger :derived :coordinates-by-column)
        first-row (fn [column] (first (by-col column)))]
    (union
      (finger-key-place getopt (first-row 1) negative-cap-linear)
      (finger-key-place getopt (first-row 2) negative-cap-linear))))

(def finger-case-tweaks
  "Workarounds for aesthetics."
  ;; Nothing here at the moment; see DMOTE v0.1.0 for example usage.
  nil)
