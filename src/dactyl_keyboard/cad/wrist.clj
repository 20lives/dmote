;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Wrist Rest                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.wrist
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.params :as params]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.key :as key]))


;;;;;;;;;;;;;;
;; Generics ;;
;;;;;;;;;;;;;;

(defn- case-south-wall-xy [[column corner]]
  "An [x y] coordinate pair at the south wall of the keyboard case."
  (take 2 (body/finger-wall-corner-position (key/first-in-column column) corner)))

(def node-size 2)
(def wall-z-offset -1)

(defn square-matrix-checker [[max-x max-y]]
  "Construct a function that will return true if a specified node is part of
  a simple matrix extending from [0, 0] to [max-x max-y]."
  (fn [[x y]] (and (<= 0 x max-x) (<= 0 y max-y))))

(defn- derive-properties [getopt]
  (let [pivot
          (case-south-wall-xy [(getopt :wrist-rest :position :finger-key-column)
                               (keyword-to-directions
                                 (getopt :wrist-rest :position :key-corner))])
        offset (getopt :wrist-rest :position :offset)
        [base-x base-y base-z] (getopt :wrist-rest :plinth-base-size)
        lip (getopt :wrist-rest :lip-height)
        pad (getopt :wrist-rest :rubber :height)
        pad-above (:above-lip pad)
        pad-below (:below-lip pad)
        corner-nw (vec (map + pivot offset))
        corner-ne (vec (map + corner-nw [base-x 0]))
        corner-sw (vec (map - corner-nw [0 base-y]))
        corner-se (vec (map - corner-ne [0 base-y]))]
   {:offset offset
    :base-x base-x
    :base-y base-y
    :base-z base-z
    :mtz-z (+ base-z lip)  ; Plastic-silicone material transition zone.
    :total-z (+ base-z lip pad-above)
    :nw corner-nw
    :ne corner-ne
    :sw corner-sw
    :se corner-se}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Threaded Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn threaded-center-height [getopt]
  (+ (/ (getopt :wrist-rest :fasteners :diameter) 2)
     (getopt :wrist-rest :fasteners :height :first)))

(defn threaded-position-keyboard [getopt]
  (let [position (getopt :wrist-rest :fasteners :mounts :case-side)
        {column :finger-key-column corner :key-corner offset :offset} position
        base (case-south-wall-xy [column ((keyword corner) keyword-to-directions)])
        height (threaded-center-height getopt)]
   (conj (vec (map + base offset)) height)))

(defn threaded-position-plinth [getopt]
  (let [corner (:nw (derive-properties getopt))
        offset (getopt :wrist-rest :fasteners :mounts :plinth-side :offset)
        height (threaded-center-height getopt)]
   (conj (vec (map + corner offset)) height)))

(defn threaded-midpoint [getopt]
  "The X, Y and Z coordinates of the middle of the threaded rod."
  (vec (map #(/ % 2)
            (map + (threaded-position-keyboard getopt) (threaded-position-plinth getopt)))))

(defn rod-angle [getopt]
  "The angle (from the y axis) of the threaded rod."
  (let [p (threaded-position-plinth getopt)
        d (map abs (map - (threaded-position-keyboard getopt) p))]
   (Math/atan (/ (first d) (second d)))))

(defn threaded-rod [getopt]
  "An unthreaded model of a theaded cylindrical rod connecting the keyboard and wrist rest."
  (translate (threaded-midpoint getopt)
    (rotate [(/ π 2) 0 (rod-angle getopt)]
      (cylinder (/ (getopt :wrist-rest :fasteners :diameter) 2)
                (getopt :wrist-rest :fasteners :length)))))

(defn rod-offset
  "A rod-specific offset relative to the primary rod (index 0).
  The unary form returns the offset of the last rod."
  ([getopt] (rod-offset getopt (dec (getopt :wrist-rest :fasteners :amount))))
  ([getopt index] (vec (map #(* index %) [0 0 (getopt :wrist-rest :fasteners :height :increment)]))))

(defn connecting-rods-and-nuts [getopt]
  "The full set of connecting threaded rods with nuts for nut bosses."
  (let [nut
          (->> (misc/iso-hex-nut-model (getopt :wrist-rest :fasteners :diameter))
               (rotate [(/ π 2) 0 0])
               (translate [0 3 0])
               (rotate [0 0 (rod-angle getopt)])
               (translate (threaded-position-keyboard getopt)))]
   (apply union
    (for [i (range (getopt :wrist-rest :fasteners :amount))]
      (translate (rod-offset getopt i)
        (union
          (threaded-rod getopt)
          nut))))))

(defn- plate-block [getopt depth]
  (let [g0 (getopt :wrist-rest :fasteners :mounts :width)
        g1 (dec g0)
        d0 depth
        d1 (dec d0)]
   (union
     (cube g0 d1 g0)
     (cube g1 d0 g1)
     (cube 1 1 (+ g1 8)))))

(defn case-plate [getopt]
  "A plate for attaching a threaded rod to the keyboard case.
  This is intended to have nuts on both sides, with a boss on the inward side."
  (misc/bottom-hull
    (translate (threaded-position-keyboard getopt)
      (translate (rod-offset getopt)
        (rotate [0 0 (rod-angle getopt)]
          (plate-block getopt (getopt :wrist-rest :fasteners :mounts :case-side :depth)))))))

(defn plinth-plate [getopt]
  "A plate on the plinth side."
  (misc/bottom-hull
    (translate (vec (map + (threaded-position-plinth getopt) (rod-offset getopt)))
      (rotate [0 0 (rod-angle getopt)]
        (plate-block getopt (getopt :wrist-rest :fasteners :mounts :plinth-side :depth))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Solid Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn solid-connector [getopt]
  (let [basecol (getopt :wrist-rest :position :finger-key-column)
        lastcol key/last-finger-column
        prop (derive-properties getopt)
        xy-west (:nw prop)
        xy-east (vec (map + (case-south-wall-xy [lastcol SSE]) (:offset prop)))
        bevel 10
        p0 (case-south-wall-xy [(- basecol 1) SSE])]
   (extrude-linear
     {:height (getopt :wrist-rest :solid-bridge :height)}
     (polygon
       (concat
         [p0]
         (map case-south-wall-xy
           (for [column (filter (partial <= basecol) key/all-finger-columns)
                 corner [SSW SSE]]
             [column corner]))
         [[(first xy-east) (second xy-west)]
          xy-west
          [(first xy-west) (- (second p0) bevel)]
          [(- (first xy-west) bevel) (second p0)]]
    )))))

(defn case-hook [getopt]
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
      {:height (getopt :wrist-rest :solid-bridge :height)}
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

(defn- node-corner-offset [directions]
  "Produce a translator for getting to one corner of a wrist rest node."
  (matrix/general-corner
    node-size node-size params/web-thickness params/plate-thickness directions))

(defn- node-corner-post [directions]
  "A truncated post shape that comes offset for one corner of a wrist rest node."
  (translate (node-corner-offset directions)
    (cube params/corner-post-width params/corner-post-width 0.01)))

(defn pad-shape [getopt]
  "A single function to determine the shape of the rubber pad.
  This outputs a map of two separate objects that need similar inputs."
  (let [prop (derive-properties getopt)
        [grid-x grid-y] (getopt :wrist-rest :rubber :shape :grid-size)
        last-column (int (/ (:base-x prop) grid-x))
        last-row (int (/ (:base-y prop) grid-y))
        all-columns (range 0 (+ last-column 1))
        all-rows (range 0 (+ last-row 1))
        wrist? (square-matrix-checker [last-column last-row])
        origin [(first (:sw prop))
                (second (:sw prop))
                (:total-z prop)]
        μ 0
        σ params/wrist-rest-σ
        θ params/wrist-rest-θ
        z (* params/wrist-z-coefficient θ)
        node-place
          (fn [[column row] shape]
            (let [M (- column (* 2 (/ last-column 3)))]  ; Placement of curvature.
             (->> shape
                  (rotate [0 (* θ (𝒩′ M μ σ)) 0])
                  (translate [0 0 (- (* z (𝒩 M μ σ)))])
                  (translate [(* column grid-x) (* row grid-y) 0])
                  (translate origin))))]
  {:top  (body/walk-and-web
           all-columns
           all-rows
           wrist?
           node-place
           node-corner-post)
   :edge (body/walk-and-wall
           wrist?
           node-place
           (fn [_ _] [0 wall-z-offset])  ; Offsetter.
           node-corner-post
           body/dropping-bevel
           [[0 0] :north]
           [[0 0] :north])}))

(defn bevel-3d-model [getopt]
  (apply union (:edge (pad-shape getopt))))

(defn bevel-2d-outline [getopt]
  (hull (project (bevel-3d-model getopt))))

(defn plinth-zone-rubber [getopt]
  (let [depth (getopt :wrist-rest :rubber :height :below-lip)]
   (union
     (translate [0 0 (+ 50 (:mtz-z (derive-properties getopt)))]
       (cube 500 500 100))
     (translate [0 0 (- (:mtz-z (derive-properties getopt)) (/ depth 2))]
       (extrude-linear {:height depth}
         (offset -2 (bevel-2d-outline getopt)))))))

(defn plinth-shape [getopt]
  "The overall shape of rubber and plastic as one object."
  (let [{top :top edge :edge} (pad-shape getopt)]
   (letfn [(shadow [shape]
             (translate [0 0 (:base-z (derive-properties getopt))]
               (extrude-linear {:height 0.01}
                 (offset 1 (project shape)))))
           (hull-to-base [shape]
             (hull shape (shadow shape)))]
    (union
      (apply union (map hull-to-base top))
      (apply union (map hull-to-base edge))
      (misc/bottom-hull (shadow (bevel-3d-model getopt)))
      (case (getopt :wrist-rest :style)
        :threaded (plinth-plate getopt)
        :solid (solid-connector getopt))))))


;;;;;;;;;;;;;
;; Outputs ;;
;;;;;;;;;;;;;

(defn plinth-plastic [getopt]
  "The lower portion of a wrist rest, to be printed in a rigid material."
  (let [properties (derive-properties getopt)
        nut (rotate [(/ π 2) 0 0]
               (misc/iso-hex-nut-model (getopt :wrist-rest :fasteners :diameter)))]
   (intersection
     (difference
       (plinth-shape getopt)
       (plinth-zone-rubber getopt)
       (case (getopt :wrist-rest :style)
         :solid
           (union
             (case-hook getopt)
             body/finger-walls)
         :threaded
           (union
             (connecting-rods-and-nuts getopt)
             ;; A hex nut pocket:
             (translate (threaded-position-plinth getopt)
               (rotate [0 0 (rod-angle getopt)]
                 (hull nut (translate [0 0 100] nut))))))
       ;; Two square holes for pouring silicone:
       (translate (vec (map + (:ne properties) [-10 -10]))
         (extrude-linear {:height 200} (square 10 10)))
       (translate (vec (map + (:sw properties) [10 10]))
         (extrude-linear {:height 200} (square 10 10))))
     (translate [0 0 500] (cube 1000 1000 1000)))))

(defn rubber-insert [getopt]
  "The upper portion of a wrist rest, to be cast or printed in a soft material."
  (color [0.5 0.5 1 1]
    (intersection (plinth-zone-rubber getopt) (plinth-shape getopt))))

(defn rubber-casting-mould [getopt]
  "A thin shell that goes on top of a wrist plinth temporarily.
  This is for casting silicone into, “in place”. As long as the
  wrist rest has 180° rotational symmetry around the z axis, one mould should
  be enough for both halves’ wrist rests, with tape to prevent leaks."
  (let [dz (- (:total-z (derive-properties getopt)) (:base-z (derive-properties getopt)))]
   (rotate [π 0 0]  ;; Print bottom-up.
     (difference
       (translate [0 0 (+ (:base-z (derive-properties getopt)) (/ dz 2))]
         (extrude-linear {:height (+ dz 4)}
           (offset 2 (bevel-2d-outline getopt))))
       (plinth-shape getopt)))))

(defn unified-preview [getopt]
  "A merged view of a wrist rest. This might be printed in hard plastic for a
  prototype but is not suitable for long-term use: It would typically be too
  hard for ergonomy and does not have a nut pocket for threaded rods."
  (intersection
    (difference
      (plinth-shape getopt)
      (union
        (case (getopt :wrist-rest :style)
          :solid (union case-hook body/finger-walls)
          :threaded (connecting-rods-and-nuts getopt))))
    (translate [0 0 500] (cube 1000 1000 1000))))
