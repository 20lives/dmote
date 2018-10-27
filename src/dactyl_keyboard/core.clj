;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [make-parents]]
            [clj-yaml.core :as yaml]
            [scad-clj.scad :refer [write-scad]]
            [scad-clj.model :exclude [use import] :refer :all]
            [dactyl-keyboard.generics :as generics]
            [dactyl-keyboard.params :as params]
            [dactyl-keyboard.sandbox :as sandbox]
            [dactyl-keyboard.cad.aux :as aux]
            [dactyl-keyboard.cad.key :as key]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.wrist :as wrist])
  (:gen-class :main true))

(defn pprint-settings
  "Show settings as assembled from (possibly multiple) files."
  [raws]
  (println "Merged settings:")
  (pprint raws))

(defn new-scad
  "Reload this namespace with any changed dependencies. Redraw .scad files."
  []
  (clojure.core/use 'dactyl-keyboard.core :reload-all))

(defn metacluster
  "Apply passed function to all key clusters."
  [function getopt]
  (apply union (map (partial function getopt) key/clusters)))

(defn build-keyboard-right
  "Right-hand-side keyboard model."
  [getopt]
  (union
    (body/mask getopt
      (difference
        (union
          (metacluster key/cluster-plates getopt)
          (metacluster body/cluster-web getopt)
          (metacluster body/cluster-wall getopt)
          (if (getopt :wrist-rest :include)
            (case (getopt :wrist-rest :style)
              :solid (wrist/case-hook getopt)
              :threaded (wrist/case-plate getopt)))
          (if (= (getopt :mcu :support :style) :stop)
            (aux/mcu-stop getopt))
          (aux/connection-positive getopt)
          (aux/foot-plates getopt)
          (if (getopt :case :back-plate :include) (aux/backplate-block getopt))
          (if (getopt :case :rear-housing :include) (body/rear-housing getopt))
          (body/wall-tweaks getopt)
          (sandbox/positive getopt))
        (metacluster key/cluster-cutouts getopt)
        (metacluster key/cluster-channels getopt)
        (aux/connection-negative getopt)
        (aux/mcu-negative getopt)
        (aux/mcu-alcove getopt)
        (if (= (getopt :mcu :support :style) :lock)
          (aux/mcu-lock-sink getopt))
        (if (getopt :case :leds :include) (aux/led-holes getopt))
        (if (getopt :case :back-plate :include)
          (aux/backplate-fastener-holes getopt))
        (if (getopt :wrist-rest :include)
          (if (= (getopt :wrist-rest :style) :threaded)
            (wrist/threaded-fasteners getopt)))
        (sandbox/negative getopt))
      (if (= (getopt :switches :style) :mx)
        (metacluster key/cluster-nubs getopt))
      (if (= (getopt :mcu :support :style) :lock) ; Outside the alcove.
        (aux/mcu-lock-fixture-composite getopt)))
    ;; The remaining elements are visualizations for use in development.
    (if (getopt :keycaps :preview) (metacluster key/cluster-keycaps getopt))
    (if (getopt :mcu :preview) (aux/mcu-visualization getopt))
    (if (and (= (getopt :mcu :support :style) :lock)
             (getopt :mcu :support :preview))
      (aux/mcu-lock-bolt getopt))
    (if (and (getopt :wrist-rest :include) (getopt :wrist-rest :preview))
      (wrist/unified-preview getopt))))

(defn build-option-accessor
  "Close over a user configuration."
  [build-options]
  (letfn [(value-at [path] (get-in build-options path ::none))
          (path-exists? [path] (not (= ::none (value-at path))))
          (valid? [path] (and (path-exists? path)
                              (not (nil? (value-at path)))))  ; “false” is OK.
          (step [path key]
            (let [next-path (conj path key)]
             (if (path-exists? next-path) next-path path)))
          (backtrack [path] (reduce step [] path))]
    (fn [& path]
      (let [exc {:path path
                 :last-good (backtrack path)
                 :at-last-good (value-at (backtrack path))}]
        (if-not (path-exists? path)
          (throw (ex-info "Configuration lacks key"
                          (assoc exc :type :missing-parameter)))
          (if-not (valid? path)
            (throw (ex-info "Configuration lacks value for key"
                            (assoc exc :type :unset-parameter)))
            (value-at path)))))))

(defn enrich-option-metadata
  "Derive certain properties that are implicit in the user configuration.
  Store these results under the “:derived” key in each section."
  [build-options]
  (reduce
    (fn [coll [path callable]]
      (assoc-in coll (conj path :derived) (callable (build-option-accessor coll))))
    build-options
    ;; Mind the order. One of these may depend upon earlier steps.
    [[[:key-clusters :finger] (partial key/cluster-properties :finger)]
     [[:key-clusters :thumb] (partial key/cluster-properties :thumb)]
     [[:key-clusters :aux0] (partial key/cluster-properties :aux0)]
     [[:key-clusters] key/resolve-aliases]
     [[:keycaps] key/keycap-properties]
     [[:switches] key/keyswitch-dimensions]
     [[:case :rear-housing] body/housing-properties]
     [[:mcu] aux/derive-mcu-properties]
     [[:wrist-rest] wrist/derive-properties]]))

(defn- from-file
  "Parse raw settings out of a YAML file."
  [filepath]
  (try
    (yaml/parse-string (slurp filepath))
    (catch java.io.FileNotFoundException _
      (do (println (format "Failed to load file “%s”." filepath))
          (System/exit 1)))))

(defn- parse-build-opts
  "Parse model parameters. Return an accessor for them."
  [options]
  (let [raws (apply generics/soft-merge
               (map from-file (:configuration-file options)))]
   (if (:debug options) (pprint-settings raws))
   (build-option-accessor
     (enrich-option-metadata
       (params/validate-configuration raws)))))

(defn- render-to-stl
  "Call OpenSCAD to render an SCAD file to STL."
  [renderer path-scad path-stl]
  (make-parents path-stl)
  (if (zero? (:exit (sh renderer "-o" path-stl path-scad)))
    (println "Rendered" path-stl)
    (do
      (println "Rendering" path-stl "failed")
      (System/exit 1))))

(defn- author
  "Describe a model in one or more output files."
  [[basename model {:keys [render renderer]}]]
  (let [scad (str "things/scad/" basename ".scad")
        stl (str "things/stl/" basename ".stl")]
    (println "Transpiling" scad)
    (make-parents scad)
    (spit scad (write-scad model))
    (if render (render-to-stl renderer scad stl))))

(defn collect-models
  "Make an option accessor function and assemble models with it.
  Return a vector of vectors suitable for calling the author function."
  [{:keys [whitelist] :as options}]
  (let [getopt (parse-build-opts options)
        single (fn [basename model] [basename model options])
        producer (fn [{:keys [condition pair basename model-fn]
                       :or {condition true pair false}}]
                  (if (and (re-find whitelist basename) condition)
                    (let [model (model-fn getopt)]
                     (if pair
                       [(single (str "right-hand-" basename) model)
                        (single
                          (str "left-hand-" basename)
                          (mirror [-1 0 0] model))]
                       [(single basename model)]))))]
   (reduce
     (fn [coll model-info] (concat coll (producer model-info)))
     []
     ;; What follows is the central roster of files and the models that go
     ;; into each. Some depend on special configuration values and some come
     ;; in pairs (left and right).
     [{:basename "preview-keycap"
       :model-fn key/all-keycaps}
      {:basename "case"
       :model-fn build-keyboard-right
       :pair true}
      {:condition (= (getopt :mcu :support :style) :lock)
       :basename "mcu-lock-bolt"
       :model-fn aux/mcu-lock-bolt}
      {:condition (getopt :wrist-rest :include)
       :basename "pad-mould"
       :model-fn wrist/rubber-casting-mould
       :pair true}
      {:condition (getopt :wrist-rest :include)
       :basename "pad-shape"
       :model-fn wrist/rubber-insert
       :pair true}
      {:condition (getopt :wrist-rest :include)
       :basename "plinth"
       :model-fn wrist/plinth-plastic
       :pair true}])))

(def cli-options
  "Define command-line interface."
  [["-c" "--configuration-file PATH" "Path to parameter file in YAML format"
    :default ["resources/opt/default.yaml"]
    :assoc-fn (fn [m k new] (update-in m [k] (fn [old] (conj old new))))]
   [nil "--describe-parameters"
    "Print a Markdown document specifying what a configuration file may contain"]
   [nil "--render" "Produce STL in addition to SCAD files"]
   [nil "--renderer PATH" "Path to OpenSCAD" :default "openscad"]
   ["-w" "--whitelist RE"
    "Limit output to files whose names match the regular expression RE"
    :default #"" :parse-fn re-pattern]
   ["-d" "--debug"]
   ["-h" "--help"]])

(defn -main
  "Act on command-line arguments, authoring files in parallel."
  [& raw]
  (let [args (parse-opts raw cli-options)
        options (:options args)]
   (cond
     (some? (:errors args)) (do (println (first (:errors args)))
                                (println (:summary args))
                                (System/exit 1))
     (:help options) (println (:summary args))
     (:describe-parameters options) (params/print-markdown-documentation)
     :else
       (try
         (doall (pmap author (collect-models options)))
         (catch clojure.lang.ExceptionInfo e
           ;; Likely raised by getopt.
           (println "An exception occurred:" (.getMessage e))
           (pprint (ex-data e))
           (System/exit 1))))))
