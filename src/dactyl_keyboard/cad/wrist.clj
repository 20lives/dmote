;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Wrist Rest                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.wrist
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.params :as params]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.cad.body :as body]            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.key :as key]))

;;;;;;;;;;;;;;
;; Generics ;;
;;;;;;;;;;;;;;

(defn- case-south-wall-xy [[column corner]]
  "An [x y] coordinate pair at the south wall of the keyboard case."
  (take 2 (body/finger-wall-corner-position (key/first-in-column column) corner)))

(def placement-xy-keyboard
  (case-south-wall-xy [params/wrist-placement-column SSE]))

(def plinth-xy-nw (vec (map + placement-xy-keyboard params/wrist-placement-offset)))
(def plinth-xy-ne [(+ (first plinth-xy-nw) params/wrist-plinth-width)
                           (second plinth-xy-nw)])
(def plinth-xy-sw (vec (map - plinth-xy-nw [0 params/wrist-plinth-length])))
(def plinth-xy-se (vec (map - plinth-xy-ne [0 params/wrist-plinth-length])))

(def grid-unit-x 4)
(def grid-unit-y params/wrist-plinth-length)
(def node-size 2)
(def wall-z-offset -1)
(defn- wall-offsetter [coordinates corner] [0 wall-z-offset])

(def last-column (int (/ params/wrist-plinth-width grid-unit-x)))
(def last-row (int (/ params/wrist-plinth-length grid-unit-y)))
(def all-columns (range 0 (+ last-column 1)))
(def all-rows (range 0 (+ last-row 1)))

(defn- wrist? [[column row]]
  "True if specified node in wrist rest surface has been requested."
  (and (<= 0 column last-column) (<= 0 row last-row)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Threaded Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def threaded-position-keyboard
  (conj (vec (map + (case-south-wall-xy [params/wrist-threaded-column SSE])
                    params/wrist-threaded-offset-keyboard))
        params/wrist-threaded-height))

(def threaded-position-plinth
  (conj (vec (map + plinth-xy-nw params/wrist-threaded-offset-plinth))
        params/wrist-threaded-height))

(def threaded-midpoint
  "The X, Y and Z coordinates of the middle of the threaded rod."
  (vec (map #(/ % 2)
            (map + threaded-position-keyboard threaded-position-plinth))))

(def rod-angle
  "The angle (from the y axis) of the threaded rod."
  (let [d (map abs (map - threaded-position-keyboard threaded-position-plinth))]
    (Math/atan (/ (first d) (second d)))))

(def threaded-rod
  "An unthreaded model of a theaded cylindrical rod connecting the keyboard and wrist rest."
  (translate threaded-midpoint
    (rotate [(/ π 2) 0 rod-angle]
      (cylinder (/ params/wrist-threaded-fastener-diameter 2) params/wrist-threaded-fastener-length))))

(defn rod-offset
  "A rod-specific offset relative to the primary rod (index 0).
  The nullary form returns the offset of the last rod."
  ([] (rod-offset (dec params/wrist-threaded-fastener-amount)))
  ([index] (vec (map #(* index %) params/wrist-threaded-separation))))

(def connecting-rods-and-nuts
  "The full set of connecting threaded rods with nuts for nut bosses."
  (let [nut
          (->> (misc/iso-hex-nut-model params/wrist-threaded-fastener-diameter)
               (rotate [(/ π 2) 0 0])
               (translate [0 3 0])
               (rotate [0 0 rod-angle])
               (translate threaded-position-keyboard))]
   (apply union
    (for [i (range params/wrist-threaded-fastener-amount)]
      (translate (rod-offset i)
        (union
          threaded-rod
          nut))))))

(defn- plate-block [depth]
  (let [g0 params/wrist-threaded-anchor-girth
        g1 (dec g0)
        d0 depth
        d1 (dec d0)]
   (union
     (cube g0 d1 g0)
     (cube g1 d0 g1)
     (cube 1 1 (+ g1 8)))))

(def case-plate
  "A plate for attaching a threaded rod to the keyboard case.
  This is intended to have nuts on both sides, with a boss on the inward side."
  (misc/bottom-hull
    (translate threaded-position-keyboard
      (translate (rod-offset)
        (rotate [0 0 rod-angle]
          (plate-block params/wrist-threaded-anchor-depth-case))))))

(def plinth-plate
  "A plate on the plinth side."
  (misc/bottom-hull
    (translate (vec (map + threaded-position-plinth (rod-offset)))
      (rotate [0 0 rod-angle]
        (plate-block params/wrist-threaded-anchor-depth-plinth)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Solid Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def solid-connector
  (let [xy-west plinth-xy-nw
        xy-east (vec (map + (case-south-wall-xy [key/last-finger-column SSE]) params/wrist-placement-offset))
        bevel 10
        p0 (case-south-wall-xy [(- params/wrist-placement-column 1) SSE])]
   (extrude-linear
     {:height params/wrist-solid-connector-height}
     (polygon
       (concat
         [p0]
         (map case-south-wall-xy
           (for [column (filter (partial <= params/wrist-placement-column) key/all-finger-columns)
                 corner [SSW SSE]]
             [column corner]))
         [[(first xy-east) (second xy-west)]
          xy-west
          [(first xy-west) (- (second p0) bevel)]
          [(- (first xy-west) bevel) (second p0)]]
    )))))

(def case-hook
  "A model hook. In the solid style, this holds the wrest in place."
  (let [[column row] (key/first-in-column key/last-finger-column)
        [x4 y2 _] (key/finger-key-position [column row]
                                           (key/mount-corner-offset ESE))
        x3 (- x4 2)
        x2 (- x3 6)
        x1 (- x2 2)
        x0 (- x1 0.6)
        y1 (- y2 6)
        y0 (- y1 1)]
    (extrude-linear
      {:height params/wrist-solid-connector-height}
      ;; Draw the outline of the hook moving counterclockwise.
      (polygon [[x0 y1]  ; Left part of the point.
                [x1 y0]  ; Right part of the point.
                [x3 y0]  ; Rightmost contact with the connector.
                [x4 y2]  ; Rightmost contact with the case.
                [x2 y2]] ; Leftmost contact with the case.
                ))))

;;;;;;;;;;;;;;;;;;;;;;;
;; Main Model Basics ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def surface-node
  (let [h (+ (abs wall-z-offset) params/plate-thickness)
        dz (- (- (/ h 2) params/plate-thickness))]
    (translate [0 0 dz] (cube node-size node-size h))))

(defn- node-corner-offset [directions]
  "Produce a translator for getting to one corner of a wrist rest node."
  (matrix/general-corner
    node-size node-size params/web-thickness params/plate-thickness directions))

(defn- node-corner-post [directions]
  "A truncated post shape that comes offset for one corner of a wrist rest node."
  (translate (node-corner-offset directions)
    (cube params/corner-post-width params/corner-post-width 0.01)))

(defn node-place [[column row] shape]
  (let [μ 0
        M (- column (* 2 (/ last-column 3)))  ; Placement of curvature.
        σ params/wrist-rest-σ
        θ params/wrist-rest-θ
        z (* params/wrist-z-coefficient θ)
        ]
  (->> shape
       (rotate [0 (* θ (𝒩′ M μ σ)) 0])
       (translate [0 0 (- (* z (𝒩 M μ σ)))])
       (translate [(* column grid-unit-x) (* row grid-unit-y) 0])
       (translate [(first plinth-xy-nw)
                   (- (second plinth-xy-nw) params/wrist-plinth-length)
                   params/wrist-plinth-height])
       )))

(def nodes
  (apply union
    (map #(misc/bottom-hull (node-place % surface-node))
      (matrix/coordinate-pairs all-columns all-rows))))

(def surface-elements
  (body/walk-and-web all-columns all-rows wrist? node-place node-corner-post))

(def bevel-elements
  (body/walk-and-wall
    (fn [[column row]] (and (<= 0 column last-column) (<= 0 row last-row)))
    node-place
    wall-offsetter
    node-corner-post
    body/dropping-bevel
    [[0 0] :north]
    [[0 0] :north]))

(def bevel-3d-model (apply union bevel-elements))

(def bevel-2d-outline (hull (project bevel-3d-model)))

;;;;;;;;;;;;;
;; Outputs ;;
;;;;;;;;;;;;;

(def plinth-zone-rubber
  (union
    (translate [0 0 (+ 50 params/wrist-silicone-starting-height)] (cube 500 500 100))
    (translate [0 0 (- params/wrist-silicone-starting-height (/ params/wrist-silicone-trench-depth 2))]
      (extrude-linear {:height params/wrist-silicone-trench-depth}
        (offset -2 bevel-2d-outline)))))

(def plinth-shape
  "The overall shape of rubber and plastic as one object."
  (letfn [(shadow [shape]
            (translate [0 0 params/wrist-plinth-base-height]
              (extrude-linear {:height 0.01}
                (offset 1 (project shape)))))
          (hull-to-base [shape]
            (hull shape (shadow shape)))]
   (union
     (apply union (map hull-to-base surface-elements))
     (apply union (map hull-to-base bevel-elements))
     (misc/bottom-hull (shadow bevel-3d-model))
     (case params/wrist-rest-style
       :threaded plinth-plate
       :solid solid-connector))))

(def plinth-plastic
  "The lower portion of a wrist rest, to be printed in a rigid material."
  (let [nut (rotate [(/ π 2) 0 0]
               (misc/iso-hex-nut-model params/wrist-threaded-fastener-diameter))]
   (intersection
     (difference
       plinth-shape
       plinth-zone-rubber
       (case params/wrist-rest-style
         :solid
           (union
             case-hook
             body/finger-walls)
         :threaded
           (union
             connecting-rods-and-nuts
             ;; A hex nut pocket:
             (translate threaded-position-plinth
               (rotate [0 0 rod-angle]
                 (hull nut (translate [0 0 100] nut))))))
       ;; Two square holes for pouring silicone:
       (translate (vec (map + plinth-xy-ne [-10 -10]))
         (extrude-linear {:height 200} (square 10 10)))
       (translate (vec (map + plinth-xy-sw [10 10]))
         (extrude-linear {:height 200} (square 10 10))))
     (translate [0 0 500] (cube 1000 1000 1000)))))

(def rubber-insert
  "The upper portion of a wrist rest, to be cast or printed in a soft material."
  (color [0.5 0.5 1 1] (intersection plinth-zone-rubber plinth-shape)))

(def rubber-casting-mould
  "A thin shell that goes on top of a wrist plinth temporarily.
  This is for casting silicone into, “in place”. As long as the
  wrist rest has 180° rotational symmetry around the z axis, one mould should
  be enough for both halves’ wrist rests, with tape to prevent leaks."
  (let [dz (- params/wrist-plinth-height params/wrist-plinth-base-height)]
   (rotate [π 0 0]  ;; Print bottom-up.
     (difference
       (translate [0 0 (+ params/wrist-plinth-base-height (/ dz 2))]
         (extrude-linear {:height (+ dz 4)}
           (offset 2 bevel-2d-outline)))
       plinth-shape))))

(def unified-preview
  "A merged view of a wrist rest. This might be printed in hard plastic for a
  prototype but is not suitable for long-term use: It would typically be too
  hard for ergonomy and does not have a nut pocket for threaded rods."
  (intersection
    (difference
      plinth-shape
      (union
        (case params/wrist-rest-style
          :solid (union case-hook body/finger-walls)
          :threaded connecting-rods-and-nuts)))
    (translate [0 0 500] (cube 1000 1000 1000))))
