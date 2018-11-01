;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Matrix Utilities                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.matrix
  (:require [unicode-math.core :refer [π]]))

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

(defn compass-delta [axis & directions]
  "Find a coordinate axis delta for movement in any of the stated directions."
  (let [value (get-in compass-to-grid [(first directions) axis])]
    (if (or (not (zero? value)) (= (count directions) 1))
      value
      (apply compass-delta axis (rest directions)))))

(defn compass-dx [& directions] (apply compass-delta :dx directions))
(defn compass-dy [& directions] (apply compass-delta :dy directions))

(defn left [direction]
  "Retrieve a direction keyword for turning left from ‘direction’."
  (ffirst (filter #(= direction (second %))
                  (partition 2 1 '(:north) (keys compass-to-grid)))))

(defn right [direction]
  (second (first (filter #(= direction (first %))
                         (partition 2 1 '(:north) (keys compass-to-grid))))))

(defn next-column [column direction]
  "Each column runs along the y axis; changing columns changes x."
  (+ column (compass-dx direction)))

(defn next-row [row direction]
  "Each row runs along the x axis; changing rows changes y."
  (+ row (compass-dy direction)))

(defn walk [[column row] & directions]
  "A tuple describing the key position an arbitrary orthogonal walk would lead to."
  (if (empty? directions)
    [column row]
    (let [direction (first directions)]
      (apply (partial walk [(next-column column direction) (next-row row direction)])
        (rest directions)))))
