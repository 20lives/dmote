;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameter Basics                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.param.base
  (:require [clojure.spec.alpha :as spec]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.generics :as generics]
            [dactyl-keyboard.param.schema :as schema]))


;;;;;;;;;;;;;;
;; Internal ;;
;;;;;;;;;;;;;;

(defn- coalesce
  "Assemble one branch in a tree structure from flat specifications."
  [coll [type path & metadata]]
  (case type
    :section
      (assoc-in coll path
        (ordered-map :metadata {:help (apply str metadata)}))
    :parameter
      (assoc-in coll path
        (assoc (first metadata) :help (apply str (rest metadata))))
    (throw (Exception.
             (format "Bad type in ‘%s’ configuration master." type)))))

(defn inclusive-or
  "A merge strategy for configuration keys. Take everything.
  Exposed for unit testing."
  [nominal candidate]
  (apply concat (map keys [nominal candidate])))

(defn- explicit-only
  "A merge strategy for configuration keys. Take only what the user provides."
  [nominal candidate]
  (keys candidate))

(defn- leaf? [node] (spec/valid? ::schema/parameter-spec node))

(defn- hard-defaults
  "Pick a user-supplied value over a default value.
  This is the default method for resolving overlap between built-in defaults
  and the user configuration, at the leaf level."
  [nominal candidate]
  (or candidate (:default nominal)))

(defn- expand-exception
  [exception key]
  (let [data (ex-data exception)
        new-data (assoc data :keys (cons key (get data :keys ())))]
   (throw (ex-info (.getMessage exception) new-data))))

(defmacro expand-any-exception
  [key call]
  (let [sym (gensym)]
    `(try
       ~call
       (catch clojure.lang.ExceptionInfo ~sym
         (expand-exception ~sym ~key)))))


;;;;;;;;;;;;;;;
;; Interface ;;
;;;;;;;;;;;;;;;

(defn soft-defaults
  "Prioritize a user-supplied value over a default value, but make it spongy.
  This is an alternate method for resolving overlap, intended for use with
  defaults that are so complicated the user will not want to write a complete,
  explicit replacement every time."
  ;; WARNING: Unused as of v0.3.0 but preserved for possible reuse.
  [nominal candidate]
  (generics/soft-merge (:default nominal) candidate))

(defn inflate
  "Recursively assemble a tree from flat specifications.
  Skip the first entry (an introduction)."
  [flat]
  (reduce coalesce (ordered-map) (rest flat)))


;; Parsing:

(defn parse-leaf
  "Resolve differences between default values and user-supplied values.
  Run the result through a specified parsing function and return it."
  [nominal candidate]
  {:pre [(leaf? nominal)]}
  (let [resolve-fn (get nominal :resolve-fn hard-defaults)
        parse-fn (get nominal :parse-fn identity)
        merged (resolve-fn nominal candidate)]
   (try
     (parse-fn merged)
     (catch Exception e
       (throw (ex-info "Could not cast value to correct data type"
                       {:type :parsing-error
                        :raw-value candidate
                        :merged-value merged
                        :parser parse-fn
                        :original-exception e}))))))

(declare parse-node)

(defn parse-branch
  "Parse a section of a configuration received through the UI."
  [key-picker nominal candidate]
  (if-not (map? candidate)
    (throw (ex-info "Non-mapping section in configuration file"
                    {:type :structural-error
                     :raw-value candidate})))
  (reduce
    (partial parse-node key-picker nominal)
    candidate
    (remove #(= :metadata %) (distinct (key-picker nominal candidate)))))

(defn parse-node
  "Parse a branch or leaf. Raise an exception on superfluous entries."
  [key-picker nominal candidate key]
  {:pre [(not (spec/valid? ::schema/descriptor key))]}
  (if (contains? nominal key)
    (expand-any-exception key
      (assoc candidate key
        (if (leaf? (key nominal))
          (parse-leaf (key nominal) (key candidate))
          (parse-branch key-picker (key nominal) (get candidate key {})))))
    (throw (ex-info "Superfluous configuration key"
                    {:type :superfluous-key
                     :keys (list key)
                     :accepted-keys (keys nominal)}))))


;; Validation:

(declare validate-leaf validate-branch)

(defn validate-node
  "Validate a fragment of a configuration received through the UI."
  [nominal candidate key]
  {:pre [(not (spec/valid? ::schema/descriptor key))]}
  (expand-any-exception key
    (if (leaf? (key nominal))
      (validate-leaf (key nominal) (key candidate))
      (validate-branch (key nominal) (key candidate)))))

(defn validate-branch
  "Validate a section of a configuration received through the UI.
  The return value should not be used."
  [nominal candidate]
  (mapv #(validate-node nominal candidate %) (keys candidate)))

(defn validate-leaf
  "Validate a specific parameter received through the UI.
  The return value should not be used."
  ;; Exposed for unit testing.
  [nominal candidate]
  {:pre [(leaf? nominal)]}
  (mapv
    (fn [validator]
      (if-not (spec/valid? validator candidate)
        (throw (ex-info "Value out of range"
                        {:type :validation-error
                         :parsed-value candidate
                         :spec-explanation (spec/explain-str validator candidate)}))))
    (get nominal :validate [some?])))

(defn delegated-validation
  "Make a function to delegate the validation of a branch."
  [raws]
  (fn [candidate] (validate-branch (inflate raws) candidate)))


;; Both/other:

(defn consume-branch
  "Parse a branch and then validate it.
  Trust validation failure to raise an exception."
  [nominal candidate]
  (let [parsed (parse-branch inclusive-or nominal candidate)]
    (validate-branch nominal parsed)
    parsed))

(defn parser-with-defaults
  "Make a function to parse a branch. Close over its raw specifications."
  [raws]
  (fn [candidate] (parse-branch inclusive-or (inflate raws) candidate)))

(defn parser-wo-defaults
  "Make a function to parse a branch without its default values.
  This is useful for parts of the configuration that can be overridden."
  [raws]
  (fn [candidate] (parse-branch explicit-only (inflate raws) candidate)))

(defn extract-defaults
  "Fetch default values for a broad section of the configuration."
  [raws]
  ((parser-with-defaults raws) {}))
