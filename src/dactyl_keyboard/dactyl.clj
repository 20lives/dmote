;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.dactyl
  (:require [clojure.tools.cli :refer [parse-opts]]
            [yaml.core :as yaml]
            [scad-clj.scad :refer [write-scad]]
            [scad-clj.model :exclude [use import] :refer :all]
            [dactyl-keyboard.generics :as generics]
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

(defn build-keyboard-right [getopt]
  "Right-hand-side keyboard model."
  (union
    (difference
      (union
        body/finger-walls
        body/thumb-walls
        body/finger-web
        body/thumb-web
        key/finger-plates
        key/thumb-plates
        tweaks/key-cluster-bridge
        tweaks/finger-case-tweaks
        (if (getopt :wrist-rest :include)
          (case params/wrist-rest-style
            :solid wrist/case-hook
            :threaded wrist/case-plate))
        aux/mcu-support
        aux/rj9-positive
        (aux/foot-plates getopt)
        (if params/include-backplate-block aux/backplate-block))
      key/finger-cutouts
      key/thumb-cutouts
      tweaks/key-cluster-bridge-cutouts
      aux/rj9-negative
      aux/mcu-negative
      (if params/include-led-housings aux/led-holes)
      (if params/include-backplate-block aux/backplate-fastener-holes)
      (if (getopt :wrist-rest :include)
        (if (= params/wrist-rest-style :threaded) wrist/connecting-rods-and-nuts))
      (translate [0 0 -500] (cube 1000 1000 1000)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    #_key/finger-keycaps
    #_key/thumb-keycaps
    #_aux/mcu-visualization
    #_wrist/unified-preview))

(defn author-scad [filename model]
  (spit (str "things/" filename) (write-scad model)))

(defn build-all [build-options]
  (letfn [(getopt [& keys] (apply (partial generics/chain-get build-options) keys))]
   (assert build-options)

   (author-scad "right-hand.scad" (build-keyboard-right getopt))
   (author-scad "left-hand.scad"
     (mirror [-1 0 0] (build-keyboard-right getopt)))

   (if (getopt :wrist-rest :include)
     (do
       ;; Items that can be used for either side.
       (author-scad "ambilateral-wrist-mould.scad" wrist/rubber-casting-mould)
       (author-scad "ambilateral-wrist-insert.scad" wrist/rubber-insert)

       ;; Items that cannot.
       (author-scad "right-wrist-base.scad" wrist/plinth-plastic)
       (author-scad "left-wrist-base.scad"
         (mirror [-1 0 0] wrist/plinth-plastic))))))

(def cli-options
  "Define command-line interface."
  [["-f" "--options-file PATH" "Path to parameter file in YAML format"
    :default "resources/opt/default.yaml"]
   ["-h" "--help"]])

(defn -main [& raw]
  (let [args (parse-opts raw cli-options)
        file (:options-file (:options args))
        build-options (yaml/from-file file)]
    (if (and (nil? (:errors args)) (not (:help (:options args))))
      (if (some? build-options)
        ; TODO: Multiple files.
        (build-all (generics/soft-merge-maps params/serialized-base build-options))
        (do (println "Please specify a build options file.")
            (System/exit 1)))
      (do (println (:summary args))
          (System/exit 1)))))
