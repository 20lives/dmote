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
    (throw (Exception. "Bad type in configuration master."))))

(defn- hard-defaults
  "Pick a user-supplied value over a default value.
  This is the default method for resolving overlap between built-in defaults
  and the user configuration, at the leaf level."
  [nominal candidate]
  (or candidate (:default nominal)))


;;;;;;;;;;;;;;;
;; Interface ;;
;;;;;;;;;;;;;;;

(defn soft-defaults
  "Prioritize a user-supplied value over a default value, but make it spongy.
  This is an alternate method for resolving overlap, intended for use with
  defaults that are so complicated the user will not want to write a complete,
  explicit replacement every time."
  [nominal candidate]
  (generics/soft-merge (:default nominal) candidate))

(defn inflate
  "Recursively assemble a tree from flat specifications.
  Skip the first entry (an introduction)."
  [flat]
  (reduce coalesce (ordered-map) (rest flat)))

(defn parse-leaf
  "Resolve differences between default values and user-supplied values.
  Run the result through a specified parsing function and return it."
  [nominal candidate]
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
                         :original-exception e}))))))

(declare validate-leaf validate-branch)

(defn validate-node
  "Validate a fragment of a configuration received through the UI."
  [nominal candidate key]
  (assert (not (spec/valid? ::schema/descriptor key)))
  (if (contains? nominal key)
    (if (spec/valid? ::schema/parameter-spec (key nominal))
      (try
        (assoc candidate key (validate-leaf (key nominal) (key candidate)))
        (catch clojure.lang.ExceptionInfo e
          ;; Add the current key for richer logging at a higher level.
          ;; This would work better if the call stack were deep.
          (let [data (ex-data e)
                keys (get data :keys ())
                new-data (assoc data :keys (conj keys key))]
           (throw (ex-info (.getMessage e) new-data)))))
      (assoc candidate key (validate-branch (key nominal) (key candidate))))
    (throw (ex-info "Superfluous configuration key"
                    {:type :superfluous-key
                     :keys (list key)
                     :accepted-keys (keys nominal)}))))

(defn validate-branch
  "Validate a section of a configuration received through the UI."
  [nominal candidate]
  (reduce (partial validate-node nominal)
          candidate
          (remove #(= :metadata %)
            (distinct (apply concat (map keys [nominal candidate]))))))

(defn validate-leaf
  "Validate a specific parameter received through the UI."
  ;; Exposed for unit testing.
  [nominal candidate]
  (assert (spec/valid? ::schema/parameter-spec nominal))
  (reduce
    (fn [unvalidated validator]
      (if (spec/valid? validator unvalidated)
        unvalidated
        (throw (ex-info "Value out of range"
                        {:type :validation-error
                         :parsed-value unvalidated
                         :raw-value candidate
                         :spec-explanation (spec/explain-str validator unvalidated)}))))
    (parse-leaf nominal candidate)
    (get nominal :validate [some?])))

(defn extract-defaults
  "Fetch default values for a broad section of the configuration."
  [flat]
  (validate-branch (inflate flat) {}))
