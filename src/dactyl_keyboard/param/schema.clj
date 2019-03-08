;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Shape Parameter Parsers and Validators                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Parameter metadata imitates clojure.tools.cli with extras.

(ns dactyl-keyboard.param.schema
  (:require [clojure.spec.alpha :as spec]
            [dactyl-keyboard.generics :as generics]))


;;;;;;;;;;;;;
;; Parsers ;;
;;;;;;;;;;;;;

(defn string-corner
  "For use with YAML, where string values are not automatically converted."
  [string]
  ((keyword string) generics/keyword-to-directions))

(defn tuple-of
  "A maker of parsers for vectors."
  [item-parser]
  (fn [candidate] (into [] (map item-parser candidate))))

(defn map-like
  "Return a parser of a map where the exact keys are known."
  [key-value-parsers]
  (letfn [(parse-item [[key value]]
            (if-let [value-parser (get key-value-parsers key)]
              [key (value-parser value)]
              (throw (Exception. (format "Invalid key: %s" key)))))]
    (fn [candidate] (into {} (map parse-item candidate)))))

(defn map-of
  "Return a parser of a map where the general type of key is known."
  [key-parser value-parser]
  (letfn [(parse-item [[key value]]
            [(key-parser key) (value-parser value)])]
    (fn [candidate] (into {} (map parse-item candidate)))))

(defn keyword-or-integer
  "A parser that takes a number as an integer or a string as a keyword.
  This works around a peculiar facet of clj-yaml, wherein integer keys to
  maps are parsed as keywords."
  [candidate]
  (try
    (int candidate)  ; Input like “1”.
    (catch ClassCastException _
      (try
        (Integer/parseInt (name candidate))  ; Input like “:1” (clj-yaml key).
        (catch java.lang.NumberFormatException _
          (keyword candidate))))))           ; Input like “:first” or “"first"”.

(defn case-tweak-position
  "Parse notation for a tweak position.
  This is normally a range of wall segments off a specific key corner, but
  anything down to a single alias without further specification is allowed."
  ([alias]
   [(keyword alias) nil 0 0])
  ([alias corner]
   (case-tweak-position alias corner 0 0))  ; Default to segment 0.
  ([alias corner segment]
   (case-tweak-position alias corner segment segment))
  ([alias corner s0 s1]
   [(keyword alias) (string-corner corner) (int s0) (int s1)]))

(defn case-tweaks [candidate]
  "Parse a tweak. This can be a lazy sequence describing a single
  point, a lazy sequence of such sequences, or a map. If it is a
  map, it may contain a similar nested structure."
  (if (string? (first candidate))
    (apply case-tweak-position candidate)
    (if (map? candidate)
      ((map-like {:chunk-size int
                  :to-ground boolean
                  :highlight boolean
                  :hull-around case-tweaks})
       candidate)
      (map case-tweaks candidate))))

(def anchored-2d-positions
  (tuple-of
    (map-like
      {:anchor keyword
       :corner string-corner
       :offset vec})))

(def anchored-polygons
  (tuple-of
    (map-like
      {:points anchored-2d-positions})))

(def nameable-spline
  (tuple-of
    (map-like
      {:position (tuple-of num)
       :alias keyword})))


;;;;;;;;;;;;;;;;
;; Validators ;;
;;;;;;;;;;;;;;;;

;; Used with spec/keys, making the names sensitive:
(spec/def ::anchor keyword?)
(spec/def ::alias (spec/and keyword?
                            #(not (= :origin %))
                            #(not (= :rear-housing %))))
(spec/def ::highlight boolean?)
(spec/def ::to-ground boolean?)
(spec/def ::chunk-size (spec/and int? #(> % 1)))
(spec/def ::hull-around (spec/coll-of (spec/or :leaf ::tweak-plate-leaf
                                               :map ::tweak-plate-map)))
(spec/def ::spline-point
  (spec/keys :req-un [::position]
             :opt-un [::alias]))

;; Users thereof:
(spec/def ::foot-plate (spec/keys :req-un [::points]))
(spec/def ::anchored-2d-position
  (spec/keys :opt-un [::anchor ::corner ::offset]))
(spec/def ::anchored-2d-list (spec/coll-of ::anchored-2d-position))
(spec/def ::points ::anchored-2d-list)
(spec/def ::tweak-plate-map
  (spec/keys :req-un [::hull-around]
             :opt-un [::highlight ::chunk-size ::to-ground]))
(spec/def ::nameable-spline (spec/coll-of ::spline-point))

;; Other:
(spec/def ::key-cluster #(not (= :derived %)))
(spec/def ::switch-style #{:alps :mx})
(spec/def ::cluster-style #{:standard :orthographic})
(spec/def ::cap-style #{:flat :socket :button})
(spec/def ::plate-installation-style #{:threads :inserts})
(spec/def ::mcu-type #{:promicro})
(spec/def ::mcu-support-style #{:lock :stop})
(spec/def ::wrist-rest-style #{:threaded :solid})
(spec/def ::wrist-position-style #{:case-side :mutual})
(spec/def ::wrist-block #{:case-side :plinth-side})
(spec/def ::column-disposition
  (spec/keys ::opt-un [::rows-below-home ::rows-above-home]))
(spec/def ::flexcoord (spec/or :absolute int? :extreme #{:first :last}))
(spec/def ::flexcoord-2d (spec/coll-of ::flexcoord :count 2))
(spec/def ::key-coordinates ::flexcoord-2d)  ; Exposed for unit testing.
(spec/def ::point-2d (spec/coll-of number? :count 2))
(spec/def ::point-3d (spec/coll-of number? :count 3))
(spec/def ::corner (set (vals generics/keyword-to-directions)))
(spec/def ::direction (set (map first (vals generics/keyword-to-directions))))
(spec/def ::wall-segment (spec/int-in 0 5))
(spec/def ::wall-extent (spec/or :partial ::wall-segment :full #{:full}))
(spec/def ::tweak-plate-leaf
  (spec/tuple keyword? (spec/nilable ::corner) ::wall-segment ::wall-segment))
(spec/def ::foot-plate-polygons (spec/coll-of ::foot-plate))

(spec/def ::descriptor  ; Parameter metadata descriptor.
  #{:heading-template :help :default :parse-fn :validate :resolve-fn})
(spec/def ::parameter-spec (spec/map-of ::descriptor some?))
