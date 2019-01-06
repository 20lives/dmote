;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [file make-parents]]
            [clj-yaml.core :as yaml]
            [scad-clj.scad :refer [write-scad]]
            [scad-clj.model :as model]
            [scad-tarmi.core :refer [π]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.dfm :refer [error-fn]]
            [dactyl-keyboard.generics :as generics]
            [dactyl-keyboard.sandbox :as sandbox]
            [dactyl-keyboard.param.access :as access]
            [dactyl-keyboard.param.tree.cluster]
            [dactyl-keyboard.param.tree.nested]
            [dactyl-keyboard.param.tree.main]
            [dactyl-keyboard.cad.aux :as aux]
            [dactyl-keyboard.cad.key :as key]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.bottom :as bottom]
            [dactyl-keyboard.cad.wrist :as wrist])
  (:gen-class :main true))

(defn pprint-settings
  "Show settings as assembled from (possibly multiple) files."
  [header raws]
  (println header)
  (pprint raws)
  (println))

(defn document-settings
  "Show documentation for settings."
  [{section :describe-parameters}]
  (access/print-markdown-section
    (case section
      :main dactyl-keyboard.param.tree.main/raws
      :clusters dactyl-keyboard.param.tree.cluster/raws
      :nested dactyl-keyboard.param.tree.nested/raws
      :wrist-rest-mounts dactyl-keyboard.param.tree.restmnt/raws
      (do (println "ERROR: Unknown section of parameters.")
          (System/exit 1))))
  (println)
  (println "⸻")
  (println)
  (println "This document was generated from the application CLI."))

(def module-map
  "A mapping naming OpenSCAD modules and the functions that make them."
  {"sprue_negative" {:model-fn wrist/sprue-negative}
   "bottom_plate_anchor_positive" {:model-fn bottom/anchor-positive}
   "bottom_plate_anchor_negative" {:model-fn bottom/anchor-negative,
                                   :chiral true}})

(defn build-keyboard-right
  "Right-hand-side keyboard model."
  [getopt]
  (maybe/union
    (maybe/difference
      (maybe/union
        (maybe/difference
          (maybe/union
            (body/mask getopt (getopt :case :bottom-plate :include)
              ;; The innermost positives, subject to the mask and all negatives:
              (key/metacluster key/cluster-plates getopt)
              (key/metacluster body/cluster-web getopt)
              (key/metacluster body/cluster-wall getopt)
              (when (and (getopt :wrist-rest :include)
                         (= (getopt :wrist-rest :style) :threaded))
                (wrist/all-case-blocks getopt))
              (when (= (getopt :mcu :support :style) :stop)
                (aux/mcu-stop getopt))
              (aux/connection-positive getopt)
              (when (getopt :case :back-plate :include)
                (aux/backplate-block getopt))
              (when (getopt :case :rear-housing :include)
                (body/rear-housing getopt))
              (body/wall-tweaks getopt)
              (when (getopt :case :bottom-plate :include)
                (bottom/case-anchors-positive getopt))
              (aux/foot-plates getopt))
            ;; Stuff that goes outside the mask but
            ;; should be subject to all negatives:
            (when (and (getopt :wrist-rest :include)
                       (getopt :wrist-rest :preview))
              (body/mask getopt (getopt :wrist-rest :include)
                (wrist/unified-preview getopt)))
            (when (and (getopt :case :bottom-plate :include)
                       (getopt :case :bottom-plate :preview))
              (if (and (getopt :wrist-rest :include)
                       (getopt :wrist-rest :bottom-plate :include)
                       (getopt :case :bottom-plate :combine))
                (bottom/combined-positive getopt)
                (maybe/union
                  (bottom/case-positive getopt)
                  (when (and (getopt :wrist-rest :include)
                             (getopt :wrist-rest :preview)
                             (getopt :wrist-rest :bottom-plate :include))
                    (bottom/wrist-positive getopt)))))
            (sandbox/positive getopt))
          ;; First-level negatives:
          (key/metacluster key/cluster-cutouts getopt)
          (key/metacluster key/cluster-channels getopt)
          (aux/connection-negative getopt)
          (aux/mcu-negative getopt)
          (aux/mcu-alcove getopt)
          (when (= (getopt :mcu :support :style) :lock)
            (aux/mcu-lock-sink getopt))
          (when (getopt :case :leds :include) (aux/led-holes getopt))
          (when (getopt :case :back-plate :include)
            (aux/backplate-fastener-holes getopt))
          (when (and (getopt :wrist-rest :include)
                     (= (getopt :wrist-rest :style) :threaded))
            (wrist/all-fasteners getopt))
          (sandbox/negative getopt))
        ;; Outer positives, subject only to outer negatives:
        (when (= (getopt :switches :style) :mx)
          (key/metacluster key/cluster-nubs getopt))
        (when (= (getopt :mcu :support :style) :lock) ; Outside the alcove.
          (aux/mcu-lock-fixture-composite getopt)))
      ;; Outer negatives:
      (when (getopt :case :bottom-plate :include)
        (bottom/case-negative getopt))
      (when (and (getopt :wrist-rest :include)
                 (getopt :wrist-rest :preview)
                 (getopt :wrist-rest :bottom-plate :include))
        (bottom/wrist-negative getopt)))
    ;; The remaining elements are visualizations for use in development.
    (when (getopt :keycaps :preview)
      (key/metacluster key/cluster-keycaps getopt))
    (when (getopt :mcu :preview)
      (aux/mcu-visualization getopt))
    (when (and (= (getopt :mcu :support :style) :lock)
               (getopt :mcu :support :preview))
      (aux/mcu-lock-bolt getopt))))

(defn build-plinth-right
  "Right-hand-side non-preview wrist-rest plinth model."
  [getopt]
  (maybe/difference
    (maybe/union
      (body/mask getopt (getopt :wrist-rest :bottom-plate :include)
        (wrist/plinth-plastic getopt))
      (when (and (getopt :wrist-rest :preview)
                 (getopt :wrist-rest :bottom-plate :include))
        (bottom/wrist-positive getopt)))
    (when (getopt :wrist-rest :bottom-plate :include)
      (bottom/wrist-negative getopt))))

(defn- collect-anchors
  "Gather names and properties for the placement of keyboard features relative
  to one another."
  [getopt]
  {:anchors (merge {:origin {:type :origin}
                    :rear-housing {:type :rear-housing}}
                   (key/collect-key-aliases getopt)
                   (wrist/collect-point-aliases getopt)
                   (wrist/collect-block-aliases getopt))})

(def derivers-static
  "A vector of configuration locations and functions for expanding them."
  ;; Mind the order. One of these may depend upon earlier steps.
  [[[:dfm] (fn [getopt] {:compensator (error-fn (getopt :dfm :error))})]
   [[:key-clusters] key/derive-cluster-properties]
   [[] collect-anchors]
   [[:keycaps] key/keycap-properties]
   [[:switches] key/keyswitch-dimensions]
   [[:case :rear-housing] body/housing-properties]
   [[:mcu] aux/derive-mcu-properties]
   [[:wrist-rest] wrist/derive-properties]])

(defn derivers-dynamic
  "Additions for more varied parts of a configuration."
  [getopt]
  (for [i (range (count (getopt :wrist-rest :mounts)))]
       [[:wrist-rest :mounts i] #(wrist/derive-mount-properties % i)]))

(defn enrich-option-metadata
  "Derive certain properties that are implicit in the user configuration.
  Use a gradually expanding but temporary build option accessor.
  Store the results under the “:derived” key in each section."
  [build-options]
  (reduce
    (fn [coll [path callable]]
      (generics/soft-merge
        coll
        (assoc-in coll (conj path :derived)
                       (callable (access/option-accessor coll)))))
    build-options
    (concat derivers-static
            (derivers-dynamic (access/option-accessor build-options)))))

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
  [{:keys [debug] :as options}]
  (let [raws (apply generics/soft-merge
               (map from-file (:configuration-file options)))]
   (if debug
     (pprint-settings "Received settings without built-in defaults:" raws))
   (let [validated (access/checked-configuration raws)]
    (if debug (pprint-settings "Resolved and validated settings:" validated))
    (access/option-accessor (enrich-option-metadata validated)))))

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
  [[basename modules model {:keys [debug render renderer]}]]
  (let [scad (file "things" "scad" (str basename ".scad"))
        stl (file "things" "stl" (str basename ".stl"))]
    (if debug (println "Started" scad))
    (make-parents scad)
    (spit scad (apply write-scad (conj modules model)))
    (if render (render-to-stl renderer (str scad) (str stl)))
    (if debug (println "Finished" scad))))

(defn- maybe-flip [mirrored model]
  (if mirrored (model/mirror [-1 0 0] model) model))

(defn- produce
  "Produce SCAD file(s) from a single model."
  [getopt cli-options
   {:keys [condition pair rotation basename modules model-fn]
    :or {condition true, pair true, rotation [0 0 0], modules []}}]
  (if (and (re-find (:whitelist cli-options) basename) condition)
    (let [predefine
            (fn [mirrored [module-condition module-name]]
              (let [module-properties (get module-map module-name)
                    basemodule-fn (:model-fn module-properties)
                    chiral (get module-properties :chiral false)
                    should-flip (and mirrored chiral)
                    model (maybe-flip should-flip (basemodule-fn getopt))]
                (when module-condition
                  (model/define-module module-name model))))
          basemodel (maybe/rotate rotation (model-fn getopt))
          single
            (fn [prefix mirrored]
              [(str prefix basename)
               (vec (map #(predefine mirrored %) modules))
               (maybe-flip mirrored basemodel)
               cli-options])]
      (if pair
        [(single "right-hand-" false)
         (single "left-hand-" true)]
        [(single "" false)]))))

(defn collect-models
  "Make an option accessor function and assemble models with it.
  Return a vector of vectors suitable for calling the author function."
  [{:keys [debug] :as options}]
  (let [getopt (parse-build-opts options)]
   (if debug (pprint-settings "Enriched settings:" (getopt)))
   (reduce
     (fn [coll model-info] (concat coll (produce getopt options model-info)))
     []
     ;; What follows is the central roster of files and the models that go
     ;; into each. Some depend on special configuration values and some come
     ;; in pairs (left and right). Some are rotated for ease of printing.
     [{:basename "preview-keycap"
       :model-fn (partial key/metacluster key/cluster-keycaps)
       :pair false}
      {:basename "case-main"
       :modules [[(getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_positive"]
                 [(getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_negative"]
                 [(getopt :wrist-rest :sprues :include)
                  "sprue_negative"]]
       :model-fn build-keyboard-right}
      {:condition (= (getopt :mcu :support :style) :lock)
       :basename "mcu-lock-bolt"
       :model-fn aux/mcu-lock-bolt
       :pair false
       :rotation [(/ π 2) 0 0]}
      ;; Wrist rest:
      {:condition (getopt :wrist-rest :include)
       :basename "pad-mould"
       :model-fn wrist/rubber-casting-mould}
      {:condition (getopt :wrist-rest :include)
       :basename "pad-shape"
       :model-fn wrist/rubber-insert}
      {:condition (getopt :wrist-rest :include)
       :basename "wrist-rest-main"
       :modules [[(getopt :wrist-rest :bottom-plate :include)
                  "bottom_plate_anchor_negative"]
                 [(getopt :wrist-rest :sprues :include)
                  "sprue_negative"]]
       :model-fn build-plinth-right}
      ;; Bottom plate(s):
      {:condition (and (getopt :case :bottom-plate :include)
                       (not (and (getopt :case :bottom-plate :combine)
                                 (getopt :wrist-rest :bottom-plate :include))))
       :basename "bottom-plate-case"
       :modules [[true "bottom_plate_anchor_negative"]]
       :model-fn bottom/case-complete
       :rotation [0 π 0]}
      {:condition (and (getopt :wrist-rest :include)
                       (getopt :wrist-rest :bottom-plate :include)
                       (not (and (getopt :case :bottom-plate :include)
                                 (getopt :case :bottom-plate :combine))))
       :basename "bottom-plate-wrist-rest"
       :modules [[true "bottom_plate_anchor_negative"]]
       :model-fn bottom/wrist-complete
       :rotation [0 π 0]}
      {:condition (and (getopt :case :bottom-plate :include)
                       (getopt :case :bottom-plate :combine)
                       (getopt :wrist-rest :include)
                       (getopt :wrist-rest :bottom-plate :include))
       :basename "bottom-plate-combined"
       :modules [[true "bottom_plate_anchor_negative"]]
       :model-fn bottom/combined-complete
       :rotation [0 π 0]}])))

(def cli-options
  "Define command-line interface."
  [["-c" "--configuration-file PATH" "Path to parameter file in YAML format"
    :default [(file "resources" "opt" "default.yaml")]
    :assoc-fn (fn [m k new] (update-in m [k] (fn [old] (conj old new))))]
   [nil "--describe-parameters SECTION"
    "Print a Markdown document specifying what a configuration file may contain"
    :default nil :parse-fn keyword]
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
     (:describe-parameters options) (document-settings options)
     :else
       (try
         (doall (pmap author (collect-models options)))
         (catch clojure.lang.ExceptionInfo e
           ;; Likely raised by getopt.
           (println "An exception occurred:" (.getMessage e))
           (pprint (ex-data e))
           (System/exit 1))))
   (if (:debug options) (println "Exiting without error."))
   (System/exit 0)))
