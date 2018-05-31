;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.core
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
            [dactyl-keyboard.cad.wrist :as wrist])
  (:gen-class :main true))

(defn new-scad []
  "Reload this namespace with any changed dependencies. Redraw .scad files."
  (clojure.core/use 'dactyl-keyboard.core :reload-all))

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
          (case (getopt :wrist-rest :style)
            :solid (wrist/case-hook getopt)
            :threaded (wrist/case-plate getopt)))
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
        (if (= (getopt :wrist-rest :style) :threaded)
          (wrist/connecting-rods-and-nuts getopt)))
      (translate [0 0 -500] (cube 1000 1000 1000)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    #_key/finger-keycaps
    #_key/thumb-keycaps
    #_aux/mcu-visualization
    (if (and (getopt :wrist-rest :include) (getopt :wrist-rest :preview))
      (wrist/unified-preview getopt))))

(defn author-scad [filename model]
  (spit (str "things/" filename) (write-scad model)))

(defn build-all [build-options]
  (letfn [(getopt [& keys]
            (let [value (get-in build-options keys)]
             (if (nil? value)
               (do (println (format "Missing configuration value: “%s”." keys))
                   (System/exit 1))
               (if (string? value) (keyword value) value))))]
   (assert build-options)

   (author-scad "right-hand.scad" (build-keyboard-right getopt))
   (author-scad "left-hand.scad"
     (mirror [-1 0 0] (build-keyboard-right getopt)))

   (if (getopt :wrist-rest :include)
     (do
       ;; Items that can be used for either side.
       (author-scad "ambilateral-wrist-mould.scad"
         (wrist/rubber-casting-mould getopt))
       (author-scad "ambilateral-wrist-insert.scad"
         (wrist/rubber-insert getopt))

       ;; Items that cannot.
       (author-scad "right-wrist-base.scad" (wrist/plinth-plastic getopt))
       (author-scad "left-wrist-base.scad"
         (mirror [-1 0 0] (wrist/plinth-plastic getopt)))))))

(def cli-options
  "Define command-line interface."
  [["-c" "--configuration-file PATH" "Path to parameter file in YAML format"
    :default ["resources/opt/default.yaml"]
    :assoc-fn (fn [m k new] (update-in m [k] (fn [old] (conj old new))))]
   ["-d" "--debug"]
   ["-h" "--help"]])

(defn load-configuration [filepaths]
  "Read and combine YAML from files, in the order given."
  (let [load (fn [path]
                (let [data (yaml/from-file path)]
                 (if (some? data)
                   data
                   (do (println (format "Failed to load file “%s”." path))
                       (System/exit 1)))))
        onto-base (partial generics/soft-merge params/serialized-base)]
   (apply onto-base (map load filepaths))))

(defn -main [& raw]
  (let [args (parse-opts raw cli-options)]
   (if (:help (:options args))
     (do (println (:summary args))
         (System/exit 0))
     (if (some? (:errors args))
       (do (println (first (:errors args)))
           (println (:summary args))
           (System/exit 1))
       (let [config (load-configuration (:configuration-file (:options args)))]
        (if (:debug (:options args)) (println "Building with" config))
        (build-all config))))))
