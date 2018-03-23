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

(def key-cluster-bridge
  "Walling and webbing between the thumb cluster and the finger cluster.
  This makes strict assumptions about the selected keyboard layout and is
  difficult to parameterize."
  (letfn [(post [key directions segment]
            (let [[placer offsetter coordinates] key
                  offsets (offsetter coordinates directions)]
             (placer coordinates
               (translate
                 (wall-segment-offset segment (first directions) offsets)
                 (mount-corner-post directions)))))]
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
        (post t0 WSW 0)
        (post t0 WSW 1)
        (post t0 WSW 3)
        (post t0 WNW 0)
        (post t0 WNW 1))
      ;; The upper left-hand-side wall:
      (hull
        (post f0 WSW 0)
        (post f0 WSW 1)
        (post f0 WSW 3)
        (post t1 WNW 0)
        (post t2 WSW 0))
      ;; Bevel on t2:
      (hull
        (post t2 NNW 0)
        (post t2 WNW 0)
        (post t2 WSW 0)
        (post t2 NNW 1)
        (post t2 WNW 1)
        (post t2 WSW 1))
      ;; A big chunk where t2 looms over f0:
      (hull
        (post t2 WSW 1)
        (post t2 WNW 1)
        (post t2 WNW 2)
        (post t2 WNW 3)
        (post t2 WNW 4)
        (post t2 NNW 1)
        (post t2 NNW 2)
        (post t2 NNW 3)
        (post t2 NNW 4)
        (post f0 SSW 0)
        (post f0 SSE 0))
      ;; Pillar for a silicone foot:
      (bottom-hull
        (post t1 WNW 2)
        (post t1 WNW 1)
        (post f0 WSW 0)
        (post f0 WSW 2))
      ;; Completion of the skirt around f1:
      (hull
        (post f1 WNW 0)
        (post f1 WSW 0)
        (post f1 WSW 1)
        (post f1 WSW 2)
        (post f1 WSW 3))
      ;; Forward joinery:
      (triangle-hulls
        (post f0 SSE 0)
        (hull
          (post f1 WNW 0)
          (hull
            (post f1 WSW 0)
            (post f1 WSW 1)
            (post f1 WSW 2)
            (post f1 WSW 3)))
        (hull
          (post t2 NNW 3)
          (post t2 NNW 4))
        (hull
          (post f1 WSW 0)
          (post f1 WSW 2))
        (hull
          (post t2 NNE 3)
          (post t2 NNE 4))
        (hull
          (post f1 SSW 0)
          (post f1 SSW 2)))
      ;; Rearward top plating:
      (triangle-hulls
        (hull
          (post f1 SSW 0)
          (post f1 SSW 2))
        (hull
          (post f1 SSE 0)
          (post f2 NNW 0))
        (hull
          (post t2 NNE 3)
          (post t2 NNE 4))
        (hull
          (post t3 NNW 3)
          (post t3 NNW 4))
        (hull
          (post f1 SSE 0)
          (post f2 NNW 0))
        (hull
          (post f2 WSW 0)
          (post f2 WSW 1))
        (hull
          (post t3 NNW 3)
          (post t3 NNW 4))
        (hull
          (post t3 ENE 3)
          (post t3 ENE 4)
          (post t3 NNE 3)
          (post t3 NNE 4))
        (hull
          (post f2 SSW 0)
          (post f2 SSW 1))
        (post f2 SSW 3)
        (hull
          (post t3 NNE 3)
          (post f2 WSW 4)
          (post t3 ENE 3)
          (post f2 SSW 4))
        (post t3 ENE 3)
        (post f2 NNW 4)
        (hull
          (post t5 NNE 2)
          (post t5 NNE 3)
          (post t5 ENE 2)
          (post t5 ENE 3)))
      ;; A bevel running down the right-hand side, just for aesthetics.
      (hull
        (post t3 ENE 3)
        (post t3 ENE 4)
        (post t5 ENE 3)
        (post t5 ENE 4))
      ;; The back plate of the outermost corner of t5:
      (hull
        (post t5 ENE 3)
        (post t5 ENE 4)
        (post t5 ESE 3)
        (post t5 ESE 4)
        (post t5 SSE 3)
        (post t5 SSE 4)
        (post t5 SSW 3)
        (post t5 SSW 4))
      ;; The back plate of f2:
      (apply hull
        (for [segment [3 4]
              north-south [:north :south]
              east-west [:east :west]]
          (union
            (post f2 [north-south east-west] segment)
            (post f2 [east-west north-south] segment))))
      ;; Lower wall:
      (pair-bottom-hulls
        (hull
          (post f2 ENE 2)
          (post f2 ENE 3)
          (post f2 ENE 4))
        (hull
          (post f2 NNW 3)
          (post f2 NNW 4))
        (hull
          (post t5 NNE 2)
          (post t5 NNE 3)
          (post t5 ENE 2)
          (post t5 ENE 3))
        (hull
          (post t5 WSW 2)
          (post t5 WSW 3))
        (hull
          (post t0 ESE 2)
          (post t0 ESE 3))
        (hull
          (post t0 WSW 2)
          (post t0 WSW 3))
        (hull
          (post t0 WNW 0)
          (post t0 WNW 1))
        (hull
          (post t1 WNW 0)
          (post t1 WNW 1))
        (hull
          (post f0 WSW 2)
          (post f0 WSW 3)))))))

(def key-cluster-bridge-cutouts
  (union
    (finger-key-place (first-in-column 1) negative-cap-linear)
    (finger-key-place (first-in-column 2) negative-cap-linear)))

(def finger-case-tweaks
  "A collection of ugly workarounds for aesthetics."
  (letfn [(post [coordinates corner segment]
            (finger-key-place coordinates
              (translate
                (wall-segment-offset segment
                  (first corner) (finger-key-wall-offsets coordinates corner))
                (mount-corner-post corner))))
          (top [coordinates corner]
            (hull (post coordinates corner 0) (post coordinates corner 1)))
          (bottom [coordinates corner]
            (hull (post coordinates corner 2) (post coordinates corner 3)))]
   (union
     ;; The corners of finger key [4, 1] look strange because of the
     ;; irregular angle and placement of the key.
     (hull
       (top [4 1] ESE)
       (top [4 1] SSE)
       (bottom [4 1] ESE)
       (bottom [4 1] SSE))
     (bottom-hull
       (bottom [4 1] ESE)
       (bottom [4 1] SSE))
     (hull
       (top [4 1] SSE)
       (top [4 1] SSW)
       (top [4 1] WSW)
       (top [4 1] WNW)
       (top [4 1] NNW))
     ;; A tidy connection to the neighbouring key.
     (hull
       (top [4 1] NNW)
       (top [4 1] WNW)
       (top [3 2] NNE)
       (top [3 2] ENE)
       (bottom [3 2] ENE)
       (top [3 2] ESE)
       (bottom [3 2] ESE)))))
