;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — Final Composition and Outputs                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.dactyl
  (:require [scad-clj.scad :refer [write-scad]]
            [scad-clj.model :exclude [use import] :refer :all]
            [dactyl-keyboard.params :as params]
            [dactyl-keyboard.tweaks :as tweaks]
            [dactyl-keyboard.cad.aux :as aux]
            [dactyl-keyboard.cad.key :as key]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.wrist :as wrist]))

(defn new-scad []
  "Reload this namespace with any changed dependencies. Redraw .scad files."
  (clojure.core/use 'dactyl-keyboard.dactyl :reload-all))

(defn print-finger-matrix []
  "Print a picture of the finger layout. No thumb keys. For your REPL."
  (for [row (reverse key/all-finger-rows)
        column key/all-finger-columns]
    (do (if (key/finger? [column row]) (print "□") (print "·"))
        (if (= column key/last-finger-column) (println)))))

(def wrist-rest-dual-view
  (union
    (wrist/rest-right false)
    (translate [-100 0 0] (wrist/rest-right true))))

(def keyboard-right
  "Right-hand-side keyboard model."
  (union
    (difference
      (union
        (difference body/finger-walls aux/rj9-space)
        (if params/include-wrist-rest
          (case params/wrist-rest-style
            :solid wrist/case-hook
            :threaded wrist/case-plate))
        body/thumb-walls
        tweaks/key-cluster-bridge
        tweaks/finger-case-tweaks
        aux/mcu-support
        key/finger-plates
        body/finger-web
        key/thumb-plates
        body/thumb-web
        aux/rj9-holder
        (if params/include-feet aux/foot-plates)
        (if params/include-backplate-block aux/backplate-block))
      tweaks/key-cluster-bridge-cutouts
      aux/mcu-negative
      key/finger-cutouts
      key/thumb-cutouts
      (if params/include-led-housings aux/led-holes)
      (if params/include-backplate-block aux/backplate-fastener-holes)
      (if params/include-wrist-rest
        (if (= params/wrist-rest-style :threaded) wrist/connecting-rods-and-nuts))
      (translate [0 0 -500] (cube 1000 1000 1000)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    #_(wrist/rest-right false)
    #_mcu-visualization
    #_key/finger-keycaps
    #_key/thumb-keycaps))

(spit "things/right-hand.scad"
      (write-scad keyboard-right))

(spit "things/left-hand.scad"
      (write-scad (mirror [-1 0 0] keyboard-right)))

(if params/include-wrist-rest
  (do
    (spit "things/right-wrist.scad"
          (write-scad wrist-rest-dual-view))
    (spit "things/left-wrist.scad"
          (write-scad (mirror [-1 0 0] wrist-rest-dual-view)))))

(defn -main [dum] 1)  ; Dummy to make it easier to batch.
