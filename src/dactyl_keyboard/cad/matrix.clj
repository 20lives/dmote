;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Matrix Utilities                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.matrix
  (:require [scad-tarmi.core :refer [π]]))

(defn coordinate-pairs
  ([columns rows] (for [column columns row rows] [column row]))
  ([columns rows selector] (filter selector (coordinate-pairs columns rows))))

(def compass-to-grid
  "Translation particles for each cardinal direction."
  (array-map
   :north {:dx 0,  :dy 1},
   :east  {:dx 1,  :dy 0},
   :south {:dx 0,  :dy -1},
   :west  {:dx -1, :dy 0}))

(def compass-radians
  {:north 0,
   :east  (/ π 2),
   :south π,
   :west  (/ π -2)})

(defn compass-delta
  "Find a coordinate axis delta for movement in any of the stated directions."
  [axis & directions]
  (let [value (get-in compass-to-grid [(first directions) axis])]
    (if (or (not (zero? value)) (= (count directions) 1))
      value
      (apply compass-delta axis (rest directions)))))

(defn compass-dx [& directions] (apply compass-delta :dx directions))
(defn compass-dy [& directions] (apply compass-delta :dy directions))

(defn left
  "Retrieve a direction keyword for turning left from ‘direction’."
  [direction]
  (ffirst (filter #(= direction (second %))
                  (partition 2 1 '(:north) (keys compass-to-grid)))))

(defn right [direction]
  (second (first (filter #(= direction (first %))
                         (partition 2 1 '(:north) (keys compass-to-grid))))))

(defn next-column
  "Each column runs along the y axis; changing columns changes x."
  [column direction]
  (+ column (compass-dx direction)))

(defn next-row
  "Each row runs along the x axis; changing rows changes y."
  [row direction]
  (+ row (compass-dy direction)))

(defn walk
  "A tuple describing the position an arbitrary orthogonal walk would lead to."
  [[column row] & directions]
  (if (empty? directions)
    [column row]
    (let [direction (first directions)]
      (apply (partial walk [(next-column column direction)
                            (next-row row direction)])
        (rest directions)))))

(defn- classify-corner
  "Classify the immediate surroundings of passed position looking ahead-left.
  Surveying must happen from an occupied position in the matrix.
  A checkered landscape (clear left, clear ahead, occluded diagonal) is not
  permitted."
  [occlusion-fn {:keys [coordinates direction] :as position}]
  {:pre [(occlusion-fn coordinates)]}
  (let [on-left (walk coordinates (left direction))
        ahead (walk coordinates direction)
        ahead-left (walk coordinates direction (left direction))
        landscape (vec (map occlusion-fn [on-left ahead-left ahead]))]
    (case landscape
      [false false false] :outer
      [false false true ] nil
      [false true  true ] :inner
      (throw (Exception.
               (format "Unforeseen landscape at %s: %s" position landscape))))))

(defn- step-clockwise
  "Pick the next position along the edge of a matrix.
  In an outer corner, turn right in place.
  When there is no corner, continue straight ahead.
  In an inner corner, jump diagonally ahead-left while also turning left."
  [occlusion-fn {:keys [coordinates direction] :as position}]
  (case (classify-corner occlusion-fn position)
    :outer (merge position {:direction (right direction)})
    nil    (merge position {:coordinates (walk coordinates direction)})
    :inner {:coordinates (walk coordinates direction (left direction))
            :direction (left direction)}))

(defn- back-clockwise
  "Pick the previous position along the edge of a matrix."
  [occlusion-fn {:keys [coordinates direction] :as position}]
  {:pre [(occlusion-fn coordinates)]}
  (let [on-left (walk coordinates (left direction))
        ahead (walk coordinates direction)
        ahead-left (walk coordinates direction (left direction))
        landscape (vec (map occlusion-fn [on-left ahead-left ahead]))]
    (case landscape
      [false false false] :outer
      [false false true ] nil
      [false true  true ] :inner
      (throw (Exception.
               (format "Unforeseen landscape at %s: %s" position landscape))))))

(defn trace-edge
  "Walk the edge of a matrix, clockwise. Return a lazy, infinite sequence.
  Annotate each position with a description of how the edge turns."
  [occlusion-fn position]
  (lazy-seq
    (cons (merge position {:corner (classify-corner occlusion-fn position)})
          (trace-edge occlusion-fn (step-clockwise occlusion-fn position)))))

(defn trace-between
  "Walk the edge of a matrix from one position to another. By default, take
  one complete lap starting at [0 0]. As in an exclusive range, the final
  position will not be part of the output."
  ([occlusion-fn]
   (trace-between occlusion-fn {:coordinates [0 0], :direction :north}))
  ([occlusion-fn start-position]
   (trace-between occlusion-fn start-position start-position))
  ([occlusion-fn start-position stop-position]
   (let [[p0 & pn] (trace-edge occlusion-fn start-position)
         salient (fn [{:keys [coordinates direction]}] [coordinates direction])
         stop (salient stop-position)
         pred (fn [p] (not= (salient p) stop))]
     (concat [p0] (take-while pred pn)))))
