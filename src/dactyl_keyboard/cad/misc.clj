;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Miscellaneous CAD Utilities                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Functions useful in more than one scad-clj project.

(ns dactyl-keyboard.cad.misc
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [π √ maybe-rotate]]))

(defn- supported-threaded-fastener [mapping]
  (fn [size]
    (let [value (get mapping size)]
      (if (nil? value)
        (do (println (format "Unsupported threaded fastener dimension: ‘%s’." size))
            (System/exit 1))
        value))))

(def iso-socket-cap-diameter
  "A map of ISO bolt diameter to socket cap width in mm."
  {3 5.5
   4 7
   5 8.5
   6 10
   8 13})

(def iso-hex-nut-flat-to-flat
  "A map of ISO bolt diameter to hex nut width in mm.
  This is measuring flat to flat (i.e. short diagonal).
  Actual nuts tend to be a little smaller, in which case these standard
  sizes are good for a very tight fit in 3D printing, after accounting for
  printer inaccuracy and material shrinkage."
  (supported-threaded-fastener {3 5.5
                                4 7
                                5 8
                                6 10
                                8 13}))

(def iso-hex-nut-height
  "A map of ISO bolt diameter to (maximum) hex nut height."
  (supported-threaded-fastener {3 2.4
                                4 3.2
                                5 4.7
                                6 5.2
                                8 6.8}))

(defn iso-hex-nut-diameter [iso-size]
  "A formula for hex diameter (long diagonal)."
  (* 2 (/ (iso-hex-nut-flat-to-flat iso-size) (√ 3))))

(defn iso-hex-nut-model
  "A model of a hex nut for a boss or pocket. No through-hole."
  ([iso-size]
   (iso-hex-nut-model iso-size (iso-hex-nut-height iso-size)))
  ([iso-size height]
   (rotate [0 0 (/ π 6)]
     (with-fn 6
       (cylinder (/ (iso-hex-nut-diameter iso-size) 2) height)))))

(defn iso-bolt-model
  "A model of an ISO metric bolt modelled for printing inaccuracy.
  This is strictly for use as a negative. The model is not threaded.
  The top of the cap sits at [0 0 0] with the bolt pointing down.
  This supports a variety of cap styles."
  [style nominal-d length]
  (let [d (+ nominal-d 0.4)
        r (/ d 2)]
    (union
      (case style
        :flat
          (let [edge (Math/log d)]
            (hull
              (cylinder d edge)
              (translate [0 0 (- r)]
                (cylinder r edge))))
        :socket
          (let [nominal-cap-d (iso-socket-cap-diameter nominal-d)
                cap-d (+ nominal-cap-d 0.4)]
            (translate [0 0 (- r)]
              (cylinder (/ cap-d 2) d)))
        :button
          (let [h (* 0.55 d)
                w (* 1.75 d)]
            (translate [0 0 (/ h -2)]
              (cylinder (/ w 2) h))))
      (translate [0 0 (/ length -2)]
        (cylinder r length)))))

(defn pairwise-hulls [& shapes]
  (apply union (map (partial apply hull) (partition 2 1 shapes))))

(defn triangle-hulls [& shapes]
  (apply union (map (partial apply hull) (partition 3 1 shapes))))

(defn bottom-extrusion [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0 :center false})))

(defn bottom-hull [& p]
  (hull p (bottom-extrusion 0.001 p)))

(defn swing-callables [translator radius rotator obj]
  "Rotate passed object with passed radius, not around its own axes.
  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.
  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that."
  (if (vector? rotator)
    (if (= (count rotator) 3)
      (swing-callables translator radius (partial maybe-rotate rotator) obj)
      (swing-callables translator radius
        (partial maybe-rotate (first rotator) (second rotator))
        obj))
    ;; Else assume the rotator is usable as a function and apply it.
    (->> obj
      (translator [0 0 (- radius)])
      rotator
      (translator [0 0 radius]))))
