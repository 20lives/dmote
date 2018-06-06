;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
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

(defn build-keyboard-right [getopt]
  "Right-hand-side keyboard model."
  (union
    (difference
      (union
        (body/finger-walls getopt)
        (body/thumb-walls getopt)
        (body/finger-web getopt)
        (body/thumb-web getopt)
        (key/finger-plates getopt)
        (key/thumb-plates getopt)
        (tweaks/key-cluster-bridge getopt)
        tweaks/finger-case-tweaks
        (if (getopt :wrist-rest :include)
          (case (getopt :wrist-rest :style)
            :solid (wrist/case-hook getopt)
            :threaded (wrist/case-plate getopt)))
        (aux/mcu-support getopt)
        (aux/rj9-positive getopt)
        (aux/foot-plates getopt)
        (if params/include-backplate-block (aux/backplate-block getopt)))
      (key/finger-cutouts getopt)
      (key/thumb-cutouts getopt)
      (tweaks/key-cluster-bridge-cutouts getopt)
      (aux/rj9-negative getopt)
      (aux/mcu-negative getopt)
      (if params/include-led-housings (aux/led-holes getopt))
      (if params/include-backplate-block (aux/backplate-fastener-holes getopt))
      (if (getopt :wrist-rest :include)
        (if (= (getopt :wrist-rest :style) :threaded)
          (wrist/connecting-rods-and-nuts getopt)))
      (translate [0 0 -500] (cube 1000 1000 1000)))
    ;; The remaining elements are visualizations for use in development.
    ;; Do not render these to STL. Use the ‘#_’ macro or ‘;’ to hide them.
    (if (getopt :key-clusters :finger :preview) (key/finger-keycaps getopt))
    #_(key/thumb-keycaps getopt)
    #_(aux/mcu-visualization getopt)
    (if (and (getopt :wrist-rest :include) (getopt :wrist-rest :preview))
      (wrist/unified-preview getopt))))

(defn author-scad [filename model]
  (spit (str "things/" filename) (write-scad model)))

(defn build-option-accessor [build-options]
  "Close over a user configuration."
  (letfn [(value-at [path] (get-in build-options path))
          (valid? [path] (not (nil? (value-at path))))  ; “false” is OK.
          (step [path key]
            (let [next-path (conj path key)]
             (if (valid? next-path) next-path path)))
          (backtrack [path] (reduce step [] path))]
    (fn [& path]
      (if (valid? path)
        (value-at path)
        (throw (ex-info (format "Missing configuration at %s" path)
                        {:last-good (backtrack path)
                         :at-last-good (keys (value-at (backtrack path)))}))))))

(defn enrich-option-metadata [build-options]
  "Derive certain properties that are implicit in the user configuration.
  Store these results under the ”:derived” key in each section."
  (reduce
    (fn [coll [path callable]]
      (assoc-in coll (conj path :derived) (callable (build-option-accessor coll))))
    build-options
    ;; Mind the order. One of these may depend upon earlier steps.
    [[[:key-clusters :finger] (partial key/cluster-properties :finger)]
     [[:wrist-rest] wrist/derive-properties]]))

(defn build-all [build-options]
  (let [getopt (build-option-accessor (enrich-option-metadata build-options))]
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
   [nil "--describe-parameters"
    "Print a Markdown document specifying what a configuration file may contain"]
   ["-d" "--debug"]
   ["-h" "--help"]])

(defn -main [& raw]
  (let [args (parse-opts raw cli-options)
        options (:options args)]
   (cond
     (some? (:errors args)) (do (println (first (:errors args)))
                                (println (:summary args))
                                (System/exit 1))
     (:help options) (println (:summary args))
     (:describe-parameters options) (params/print-markdown-documentation)
     :else
       (let [config (params/load-configuration (:configuration-file options))]
        (if (:debug options) (do (println "Merged options:") (pprint config)))
        (build-all config)))))
