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
            [dactyl-keyboard.cad.case :refer :all]))


(defn- placed-segment [key directions segment]
  "A convenience function for specifying a wall segment."
  (let [[placer offsetter coordinates] key
        offsets (offsetter coordinates directions)]
   (placer coordinates
     (translate
       (wall-segment-offset segment (first directions) offsets)
       (mount-corner-post directions)))))

(defn- post [key directions segment & segments]
  "Just avoid complicating the SCAD code with a hull unnecessarily."
  (if (empty? segments)
    (placed-segment key directions segment)
    (apply hull (map (partial placed-segment key directions)
                     (conj segments segment)))))

(def key-cluster-bridge
  "Walling and webbing between the thumb cluster and the finger cluster.
  This makes strict assumptions about the selected keyboard layout and is
  difficult to parameterize."
  (let [f0 [finger-key-place finger-key-wall-offsets (first-in-column 0)]
        f1 [finger-key-place finger-key-wall-offsets (first-in-column 1)]
        f2 [finger-key-place finger-key-wall-offsets (first-in-column 2)]
        t0 [thumb-key-place thumb-key-wall-offsets [-1 -2]]
        t1 [thumb-key-place thumb-key-wall-offsets [-1 -1]]
        t2 [thumb-key-place thumb-key-wall-offsets [-1 0]]
        t3 [thumb-key-place thumb-key-wall-offsets [0 0]]
        t4 [thumb-key-place thumb-key-wall-offsets [0 -1]]
        t5 [thumb-key-place thumb-key-wall-offsets [0 -2]]]
    (union
      ;; An extension of the wall of t0:
      (hull
        (post t0 WSW 0 1 3)
        (post t0 WNW 0 1))
      ;; A big chunk where t2 looms over f0:
      (hull
        (post t2 WSW 1)
        (post t2 WNW 1 2 3 4)
        (post t2 NNW 1 2 3 4)
        (post f0 WSW 0)
        (post f0 SSE 0)
        (post t1 WNW 1)
        (post f0 WSW 0))
      ;; Forward joinery:
      (triangle-hulls
        (post f0 SSE 0)
        (hull
          (post f1 WNW 0)
          (post f1 WSW 0 2 3)
          (post f1 SSW 0 2))
        (post t2 NNW 4)
        (post f1 WSW 0 2)
        (post t2 NNE 4)
        (post f1 SSW 0 2))
      ;; Rearward top plating:
      (triangle-hulls
        (post f1 SSW 0 2)
        (hull
          (post f1 SSE 0)
          (post f2 NNW 0))
        (post t2 NNE 4)
        (post t3 NNW 4)
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

(def key-cluster-bridge-cutouts
  (union
    (finger-key-place (first-in-column 1) negative-cap-linear)
    (finger-key-place (first-in-column 2) negative-cap-linear)))

(def finger-case-tweaks
  "A collection of ugly workarounds for aesthetics."
  (let [weirdo [finger-key-place finger-key-wall-offsets [4 1]]
        neighbour-nw [finger-key-place finger-key-wall-offsets [3 2]]
        neighbour-w [finger-key-place finger-key-wall-offsets [3 1]]
        neighbour-s [finger-key-place finger-key-wall-offsets [4 0]]
        neighbour-se [finger-key-place finger-key-wall-offsets [5 0]]]
   (union
     ;; Upper bevel around the weirdo key.
     (hull
       (post weirdo NNW 0 1)
       (post weirdo WNW 0 1)
       (post weirdo WSW 0 1)
       (post weirdo SSW 0 1)
       (post weirdo SSE 0 1)
       (post weirdo ESE 0 1))
     ;; West wall-web hybrid.
     (hull
       (post neighbour-nw ESE 0)
       (post neighbour-w ENE 0)
       (post neighbour-w ESE 0)
       (post neighbour-s WNW 0)
       (post weirdo WNW 1)
       (post weirdo WSW 1)
       (post weirdo SSW 1))
     ;; South wall-web hybrid.
     (hull
       (post neighbour-s NNW 0)
       (post neighbour-s NNE 0)
       (post neighbour-se NNW 0)
       (post weirdo WSW 1)
       (post weirdo SSW 1)
       (post weirdo SSE 1)
       (post weirdo ESE 1))
     ;; A tidy connection to the neighbouring key.
     (hull
       (post neighbour-nw ENE 0 1 2)
       (post neighbour-nw ESE 0 1 2)
       (post weirdo NNW 0 1)
       (post weirdo WNW 0 1)))))
