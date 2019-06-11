;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameter Accessors                                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This file describes the interpretation of configuration files for the
;;; application. It parses and validates deserialized data and builds
;;; functions used throughout the application to access that data.

(ns dactyl-keyboard.param.access
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [dactyl-keyboard.param.base :as base]
            [dactyl-keyboard.param.schema :as schema]
            [dactyl-keyboard.param.tree.main :as main]))


;;;;;;;;;;;;;;
;; Internal ;;
;;;;;;;;;;;;;;

(defn- print-markdown-fragment
  "Print a description of a node in the settings structure using Markdown."
  [node level]
  (let [heading-style
          (fn [text]
            (if (< level 7)  ; Markdown only supports up to h6 tags.
              (str (string/join "" (repeat level "#")) " " text)
              (str "###### " text " at level " level)))]
    (doseq [key (remove #(= :metadata %) (keys node))]
      (println)
      (if (spec/valid? ::schema/parameter-spec (key node))
        (do (println
              (heading-style
                (format (get-in node [key :heading-template] "Parameter `%s`")
                        (name key))))
            (println)
            (println (get-in node [key :help] "Undocumented.")))
        (do (println (heading-style (format "Section `%s`" (name key))))
            (println)
            (println (get-in node [key :metadata :help] "Undocumented."))
            (print-markdown-fragment (key node) (inc level)))))))

(defn- specific-getter
  "Return a function that will find the most specific configuration value
  available for a key on the keyboard."
  [getopt end-path]
  (let [get-in-section
          (fn [section-path]
            (let [full-path (concat [:by-key] section-path [:parameters] end-path)]
              (apply getopt full-path)))
        get-default (fn [] (get-in-section []))
        try-get
          (fn [section-path]
            (try
              (get-in-section section-path)
              (catch clojure.lang.ExceptionInfo e
                (if-not (= (:type (ex-data e)) :missing-parameter)
                  (throw e)))))
        find-index (fn [pred coll]
                     (first (keep-indexed #(when (pred %2) %1) coll)))]
    (fn [cluster [column row]]
      "Check, in order: Key-specific values favouring first/last row;
      column-specific values favouring first/last column;
      cluster-specific values; and finally the base section, where a
      value is required to exist if we get there."
      (let [columns (getopt :key-clusters :derived :by-cluster cluster :column-range)
            by-col (getopt :key-clusters :derived :by-cluster cluster :row-indices-by-column)
            rows (by-col column)
            first-column (= (first columns) column)
            last-column (= (last columns) column)
            first-row (= (first rows) row)
            last-row (= (last rows) row)
            sources
              [[[]                       []]
               [[first-column]           [:columns :first]]
               [[last-column]            [:columns :last]]
               [[]                       [:columns column]]
               [[first-column first-row] [:columns :first :rows :first]]
               [[first-column last-row]  [:columns :first :rows :last]]
               [[last-column first-row]  [:columns :last :rows :first]]
               [[last-column last-row]   [:columns :last :rows :last]]
               [[first-row]              [:columns column :rows :first]]
               [[last-row]               [:columns column :rows :last]]
               [[]                       [:columns column :rows row]]]
            good-source
              (fn [coll [requirements section-path]]
                (if (every? boolean requirements)
                  (conj coll (concat [:clusters cluster] section-path))
                  coll))
            prio (reduce good-source [] (reverse sources))]
        (if-let [non-default (some try-get prio)] non-default (get-default))))))


;;;;;;;;;;;;;;;
;; Interface ;;
;;;;;;;;;;;;;;;

(defn print-markdown-section
  "Print documentation for a section based on its flat specifications.
  Use the first entry as an introduction."
  [flat]
  (println (apply str (first flat)))
  (print-markdown-fragment (base/inflate flat) 2))

(defn checked-configuration
  "Attempt to describe any errors in the user configuration."
  [candidate]
  (try
    (base/consume-branch (base/inflate main/raws) candidate)
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
       (println "Configuration error:" (.getMessage e))
       (println "    At key(s):" (string/join " → " (map name (:keys data))))
       (if (:accepted-keys data)
         (println "    Accepted key(s) there:"
                  (string/join ", " (map name (:accepted-keys data)))))
       (if (contains? data :raw-value)
         (println "    Value before parsing:" (:raw-value data)))
       (if (contains? data :parsed-value)
         (println "    Value after parsing:" (:parsed-value data)))
       (if (contains? data :parser)
         (println "    Parser:" (:parser data)))
       (if (:spec-explanation data)
         (println "    Validator output:" (:spec-explanation data)))
       (if (:original-exception data)
         (do (println "    Caused by:")
             (print "      ")
             (println
               (string/join "\n      "
                 (string/split-lines (pr-str (:original-exception data)))))))
       (System/exit 1)))))

(defn option-accessor
  "Close over a—potentially incomplete—user configuration."
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

(defn most-specific [getopt end-path cluster coord]
  ((specific-getter getopt end-path) cluster coord))

(defn resolve-anchor
  "Resolve the name of a feature using derived settings."
  ([getopt name]
   {:pre [(keyword? name)]}
   (getopt :derived :anchors name))
  ([getopt name predicate]
   (let [properties (resolve-anchor getopt name)]
     (if (predicate properties)
       properties
       (throw (ex-info "Named anchor cannot be used for subject feature"
                       {:name name, :properties properties,
                        :predicate predicate}))))))

(defn get-key-alias [getopt alias]
  (resolve-anchor getopt alias #(= (:type %) :key)))

(defn key-properties
  "The properties of a specific key, including derived data."
  [getopt cluster coord]
  (getopt :keys :derived (most-specific getopt [:key-style] cluster coord)))

(defn tweak-data
  "Retrieve payload data for case tweaks without their names."
  [getopt]
  (apply concat (vals (getopt :case :tweaks))))
