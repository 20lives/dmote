;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Main Module — CLI, Final Composition and Outputs                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [scad-clj.scad :refer [write-scad]]
            [scad-clj.model :as model]
            [scad-app.core :refer [filter-by-name
                                   refine-asset refine-all build-all]]
            [scad-tarmi.core :refer [π]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.dfm :refer [error-fn]]
            [dactyl-keyboard.generics :as generics]
            [dactyl-keyboard.sandbox :as sandbox]
            [dactyl-keyboard.param.access :as access]
            [dactyl-keyboard.param.tree.cluster]
            [dactyl-keyboard.param.tree.nested]
            [dactyl-keyboard.param.tree.main]
            [dactyl-keyboard.cad.auxf :as auxf]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.bottom :as bottom]
            [dactyl-keyboard.cad.key :as key]
            [dactyl-keyboard.cad.place :as place]
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

(defn build-plinth-right
  "Right-hand-side non-preview wrist-rest plinth model."
  [getopt]
  (maybe/difference
    (maybe/union
      (body/mask getopt (getopt :wrist-rest :bottom-plate :include)
        (wrist/plinth-plastic getopt))
      (bottom/wrist-anchors-positive getopt)
      (when (and (getopt :wrist-rest :preview)
                 (getopt :wrist-rest :bottom-plate :include))
        (bottom/wrist-positive getopt)))
    (when (getopt :wrist-rest :bottom-plate :include)
      (bottom/wrist-negative getopt))))

(defn- masked-inner-positive
  "Parts of the keyboard that are subject to a mask and all negatives."
  [getopt]
  (body/mask getopt (getopt :case :bottom-plate :include)
    (key/metacluster key/cluster-plates getopt)
    (key/metacluster body/cluster-web getopt)
    (key/metacluster body/cluster-wall getopt)
    (when (and (getopt :wrist-rest :include)
               (= (getopt :wrist-rest :style) :threaded))
      (wrist/all-case-blocks getopt))
    (when (and (getopt :mcu :include)
               (= (getopt :mcu :support :style) :stop))
      (auxf/mcu-stop getopt))
    (when (getopt :connection :include)
      (auxf/connection-positive getopt))
    (when (getopt :case :back-plate :include)
      (auxf/backplate-block getopt))
    (when (getopt :case :rear-housing :include)
      (body/rear-housing getopt))
    (body/wall-tweaks getopt)
    (when (getopt :case :bottom-plate :include)
      (bottom/case-anchors-positive getopt))
    (auxf/foot-plates getopt)))

(defn- midlevel-positive
  "Parts of the keyboard that go outside the mask but should still be subject
  to all negatives."
  [getopt]
  (maybe/union
    (masked-inner-positive getopt)
    (when (and (getopt :wrist-rest :include)
               (getopt :wrist-rest :preview))
      (body/mask getopt (getopt :wrist-rest :include)
        (wrist/unified-preview getopt)))
    (when (and (getopt :wrist-rest :include)
               (not (getopt :wrist-rest :preview))
               (= (getopt :wrist-rest :style) :solid))
      (body/mask getopt (getopt :wrist-rest :include)
        (build-plinth-right getopt)))
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
    (sandbox/positive getopt)))

(defn build-keyboard-right
  "Right-hand-side keyboard model."
  [getopt]
  (maybe/union
    (maybe/difference
      (maybe/union
        (maybe/difference
          (midlevel-positive getopt)
          ;; First-level negatives:
          (key/metacluster key/cluster-cutouts getopt)
          (key/metacluster key/cluster-channels getopt)
          (when (getopt :connection :include)
            (auxf/connection-negative getopt))
          (when (getopt :mcu :include)
            (auxf/mcu-negative getopt))
          (when (getopt :mcu :include)
            (auxf/mcu-alcove getopt))
          (when (and (getopt :mcu :include)
                     (= (getopt :mcu :support :style) :lock))
            (auxf/mcu-lock-sink getopt))
          (when (getopt :case :leds :include) (auxf/led-holes getopt))
          (when (getopt :case :back-plate :include)
            (auxf/backplate-fastener-holes getopt))
          (when (and (getopt :wrist-rest :include)
                     (= (getopt :wrist-rest :style) :threaded))
            (wrist/all-fasteners getopt))
          (sandbox/negative getopt))
        ;; Outer positives, subject only to outer negatives:
        (when (and (getopt :mcu :include)
                   (= (getopt :mcu :support :style) :lock))
          ;; MCU support features outside the alcove.
          (auxf/mcu-lock-fixture-composite getopt)))
      ;; Outer negatives:
      (when (getopt :case :bottom-plate :include)
        (bottom/case-negative getopt))
      (when (and (getopt :wrist-rest :include)
                 (getopt :wrist-rest :preview)
                 (getopt :wrist-rest :bottom-plate :include))
        (bottom/wrist-negative getopt)))
    ;; The remaining elements are visualizations for use in development.
    (when (getopt :keys :preview)
      (key/metacluster key/cluster-keycaps getopt))
    (when (and (getopt :mcu :include) (getopt :mcu :preview))
      (auxf/mcu-visualization getopt))
    (when (and (getopt :mcu :include)
               (getopt :mcu :support :preview)
               (= (getopt :mcu :support :style) :lock))
      (auxf/mcu-lock-bolt getopt))))

(defn build-rubber-casting-mould-right
  "A thin shell that fits on top of the right-hand-side wrist-rest model.
  This is for casting silicone into, “in place”. If the wrist rest has
  180° rotational symmetry around the z axis, one mould should
  be enough for both halves’ wrist rests. To be printed upside down."
  ;; WARNING: This will not render correctly in OpenSCAD 2015. It will in
  ;; a nightly build as of 2018-12-17.
  [getopt]
  (place/wrist-undo getopt
    (model/difference
      (wrist/mould-polyhedron getopt)
      (wrist/unified-preview getopt)
      (bottom/wrist-anchors-positive getopt)
      (when (= (getopt :wrist-rest :style) :solid)
        (body/wall-tweaks getopt)))))

(defn build-rubber-pad-right
  "Right-hand-side wrist-rest pad model. Useful in visualization and
  prototyping, but you would not normally include a print of this in your
  final product, at least not in a hard plastic."
  [getopt]
  (place/wrist-undo getopt
    (maybe/difference
      (body/mask getopt (getopt :wrist-rest :bottom-plate :include)
        (wrist/rubber-insert-positive getopt))
      (bottom/wrist-anchors-positive getopt)
      (when (= (getopt :wrist-rest :style) :solid)
        (body/wall-tweaks getopt)))))

(defn- collect-anchors
  "Gather names and properties for the placement of keyboard features relative
  to one another."
  [getopt]
  {:anchors (merge {:origin {:type :origin}
                    :rear-housing {:type :rear-housing}}
                   (key/collect-key-aliases getopt)
                   (wrist/collect-point-aliases getopt)
                   (wrist/collect-block-aliases getopt)
                   (into {} (for [[k v] (getopt :secondaries)]
                              [k (merge v {:type :secondary})])))})


(def derivers-static
  "A vector of configuration locations and functions for expanding them."
  ;; Mind the order. One of these may depend upon earlier steps.
  [[[:dfm] (fn [getopt] {:compensator (error-fn (getopt :dfm :error-general))})]
   [[:keys] key/derive-style-properties]
   [[:key-clusters] key/derive-cluster-properties]
   [[] collect-anchors]
   [[:case :rear-housing] body/housing-properties]
   [[:mcu] auxf/derive-mcu-properties]
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

(defn- output-filepath-fn
  [base suffix]
  "Produce a relative file path for e.g. SCAD or STL.
  This upholds Dactyl tradition with “things” over the scad-app default."
  (io/file "things" suffix (str base "." suffix)))

(defn- parse-build-opts
  "Parse model parameters. Return an accessor for them."
  [{:keys [debug configuration-file] :as options}]
  (let [raws (apply generics/soft-merge
               (conj (map from-file configuration-file) {}))]
   (if debug
     (pprint-settings "Received settings without built-in defaults:" raws))
   (let [validated (access/checked-configuration raws)]
     (if debug (pprint-settings "Resolved and validated settings:" validated))
     (access/option-accessor (enrich-option-metadata validated)))))

(def module-asset-list
  "OpenSCAD modules and the functions that make them."
  [{:name "sprue_negative"
    :model-precursor wrist/sprue-negative}
   {:name "bottom_plate_anchor_positive"
    :model-precursor bottom/anchor-positive}
   {:name "bottom_plate_anchor_negative"
    :model-precursor bottom/anchor-negative,
    :chiral true}])

(defn module-asset-map
  "Convert module-asset-list to a hash map with fully resolved models.
  Add a variable number of additional modules based on key styles."
  [getopt]
  (merge
    (reduce  ; Static.
      (fn [coll {:keys [name model-precursor] :as asset}]
        (assoc coll name
          (assoc asset :model-main (model-precursor getopt))))
      {}
      module-asset-list)
    (reduce  ; Dynamic.
      (fn [coll key-style]
        (let [prop (getopt :keys :derived key-style)
              {:keys [switch-type module-keycap module-switch]} prop]
          (assoc coll
            module-keycap
            {:name module-keycap
             :model-main (key/single-cap getopt key-style false)}
            module-switch  ;; Uniqueness of input not guaranteed.
            {:name module-switch
             :model-main (key/single-switch getopt switch-type)})))
      {}
      (keys (getopt :keys :styles)))))

(defn- get-key-modules
  "Produce a sorted vector of module name strings for user-defined key styles."
  [getopt & property-keys]
  (sort
    (into []
      (reduce
        (fn [coll data] (apply (partial conj coll) (map data property-keys)))
        #{}
        (vals (getopt :keys :derived))))))

(defn get-static-precursors
  "Make the central roster of files and the models that go into each.
  The schema used to describe them is a superset of the scad-app
  asset schema, adding dependencies on special configuration values and
  rotation for ease of printing. The models themselves are described with
  unary precursors that take a completed “getopt” function."
  [getopt]
  [{:name "preview-keycap-clusters"
    :modules (get-key-modules getopt :module-keycap)
    :model-precursor (partial key/metacluster key/cluster-keycaps)}
   {:name "case-main"
    :modules (concat
               [(when (getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_positive")
                (when (getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_negative")
                (when (getopt :wrist-rest :sprues :include)
                  "sprue_negative")]
               (get-key-modules getopt :module-keycap :module-switch))
    :model-precursor build-keyboard-right
    :chiral true}
   (when (and (getopt :mcu :include)
              (= (getopt :mcu :support :style) :lock))
     {:name "mcu-lock-bolt"
      :model-precursor auxf/mcu-lock-bolt
      :rotation [(/ π 2) 0 0]})
   ;; Wrist rest:
   (when (getopt :wrist-rest :include)
     {:name "pad-mould"
      :modules [(when (getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_positive")]
      :model-precursor build-rubber-casting-mould-right
      :rotation [π 0 0]
      :chiral true})  ; Chirality is possible but not guaranteed.
   (when (getopt :wrist-rest :include)
     {:name "pad-shape"
      :modules [(when (getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_positive")]
      :model-precursor build-rubber-pad-right
      :chiral true})
   (when (and (getopt :wrist-rest :include)
              (not (= (getopt :wrist-rest :style) :solid)))
     {:name "wrist-rest-main"
      :modules [(when (getopt :case :bottom-plate :include)
                  "bottom_plate_anchor_positive")
                (when (getopt :wrist-rest :bottom-plate :include)
                  "bottom_plate_anchor_negative")
                (when (getopt :wrist-rest :sprues :include)
                  "sprue_negative")]
      :model-precursor build-plinth-right
      :chiral true})
   ;; Bottom plate(s):
   (when (and (getopt :case :bottom-plate :include)
              (not (and (getopt :case :bottom-plate :combine)
                        (getopt :wrist-rest :bottom-plate :include))))
     {:name "bottom-plate-case"
      :modules ["bottom_plate_anchor_negative"]
      :model-precursor bottom/case-complete
      :rotation [0 π 0]
      :chiral true})
   (when (and (getopt :wrist-rest :include)
              (getopt :wrist-rest :bottom-plate :include)
              (not (and (getopt :case :bottom-plate :include)
                        (getopt :case :bottom-plate :combine))))
     {:name "bottom-plate-wrist-rest"
      :modules ["bottom_plate_anchor_negative"]
      :model-precursor bottom/wrist-complete
      :rotation [0 π 0]
      :chiral true})
   (when (and (getopt :case :bottom-plate :include)
              (getopt :case :bottom-plate :combine)
              (getopt :wrist-rest :include)
              (getopt :wrist-rest :bottom-plate :include))
     {:name "bottom-plate-combined"
      :modules ["bottom_plate_anchor_negative"]
      :model-precursor bottom/combined-complete
      :rotation [0 π 0]
      :chiral true})])

(defn get-all-precursors
  "Add dynamic elements to static precursors.
  This is currently all about keycaps, ignoring maquette-style caps as
  disinteresting to print."
  [getopt]
  (reduce
    (fn [coll key-style]
      (concat coll
        (if-not (= (getopt :keys :derived key-style :style) :maquette)
          [{:name (str "keycap-" (name key-style))
            :model-precursor #(key/single-cap % key-style true)}])))
    (get-static-precursors getopt)
    (keys (getopt :keys :styles))))

(defn- finalize-asset
  "Define scad-app asset(s) from a single proto-asset.
  Return a vector of one or two assets."
  [getopt module-map cli-options
   {:keys [model-precursor rotation modules]
    :or {rotation [0 0 0], modules []}
    :as proto-asset}]
  (refine-asset
    {:original-fn #(str "right-hand-" %),
     :mirrored-fn #(str "left-hand-" %)}
    (conj
      (select-keys proto-asset [:name :chiral])  ; Simplified base.
      [:model-main (maybe/rotate rotation (model-precursor getopt))]
      (when (getopt :resolution :include)
        [:minimum-face-size (getopt :resolution :minimum-face-size)]))
    (map (partial get module-map) (remove nil? modules))))

(defn- finalize-all
  [{:keys [debug] :as cli-options}]
  (let [getopt (parse-build-opts cli-options)
        precursors (get-all-precursors getopt)
        module-map (module-asset-map getopt)
        requested (remove nil? precursors)]
    (if debug (pprint-settings "Enriched settings:" (getopt)))
    (refine-all requested
      {:refine-fn (partial finalize-asset getopt module-map)})))

(def cli-options
  "Define command-line interface."
  [["-c" "--configuration-file PATH" "Path to parameter file in YAML format"
    :default []
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
         (build-all (filter-by-name (:whitelist options) (finalize-all options))
                    {:render (:render options)
                     :rendering-program (:renderer options)
                     :filepath-fn output-filepath-fn})
         (catch clojure.lang.ExceptionInfo e
           ;; Likely raised by getopt.
           (println "An exception occurred:" (.getMessage e))
           (pprint (ex-data e))
           (System/exit 1))))
   (if (:debug options) (println "Exiting without error."))
   (System/exit 0)))
