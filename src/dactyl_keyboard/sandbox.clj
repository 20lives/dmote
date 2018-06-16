;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Tweaks and Workarounds                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module contains hooks for shapes not otherwise supported by the
;;; application. It is intended for personal use only.

(ns dactyl-keyboard.sandbox
  (:require [scad-clj.model :exclude [use import] :refer :all]))

(defn positive [getopt]
  #_(cube 100 100 100))

(defn negative [getopt]
  #_(cylinder 33 150))
