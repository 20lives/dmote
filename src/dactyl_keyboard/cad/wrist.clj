;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Wrist Rest                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.wrist
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [π]]
            [dactyl-keyboard.params :as params]
            [dactyl-keyboard.generics :refer [abs ESE SSE SSW]]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.key :as key]))


;;;;;;;;;;;;;;
;; Generics ;;
;;;;;;;;;;;;;;

(defn derive-properties [getopt]
  "Derive certain properties from the base configuration."
  (let [key-alias (getopt :wrist-rest :position :key-alias)
        cluster (getopt :key-clusters :derived :aliases key-alias :cluster)
        coord (getopt :key-clusters :derived :aliases key-alias :coordinates)
        pivot (body/wall-corner-position getopt cluster coord nil)
        offset (getopt :wrist-rest :position :offset)
        [base-x base-y z1] (getopt :wrist-rest :shape :plinth-base-size)
        lip (getopt :wrist-rest :shape :lip-height)
        z2 (+ z1 lip)
        getpad (partial getopt :wrist-rest :shape :pad :height)
        pad-middle (getpad :lip-to-surface)
        z3 (+ z2 pad-middle)
        pad-above (getpad :surface-range)
        z4 (+ z3 pad-above)
        pad-below (getpad :below-lip)
        corner-nw (vec (map + (take 2 pivot) offset))
        corner-ne (vec (map + corner-nw [base-x 0]))
        corner-sw (vec (map - corner-nw [0 base-y]))
        corner-se (vec (map - corner-ne [0 base-y]))
        center (conj (vec (map - corner-ne (map #(/ % 2) [base-x base-y]))))]
   {:offset offset
    :base-x base-x
    :base-y base-y
    :z1 z1  ; Top of base, bottom of lip.
    :z2 z2  ; Top of lip. Plastic-silicone material transition zone.
    :z3 z3  ; Silicone-to-silicone transition at base of heightmap.
    :z4 z4  ; Absolute peak of the entire plinth. Top of silicone pad.
    :key-cluster cluster
    :key-coord coord
    :nw corner-nw
    :ne corner-ne
    :sw corner-sw
    :se corner-se
    :center center}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Threaded Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn threaded-center-height [getopt]
  (+ (/ (getopt :wrist-rest :fasteners :diameter) 2)
     (getopt :wrist-rest :fasteners :height :first)))

(defn threaded-position-keyboard [getopt]
  (let [position (getopt :wrist-rest :fasteners :mounts :case-side)
        {alias :key-alias offset :offset} position
        key (getopt :key-clusters :derived :aliases alias)
        {cluster :cluster coordinates :coordinates} key
        base (body/wall-corner-position getopt cluster coordinates nil)
        height (threaded-center-height getopt)]
   (conj (vec (map + (take 2 base) offset)) height)))

(defn threaded-position-plinth [getopt]
  (let [corner (getopt :wrist-rest :derived :nw)
        offset (getopt :wrist-rest :fasteners :mounts :plinth-side :offset)
        height (threaded-center-height getopt)]
   (conj (vec (map + corner offset)) height)))

(defn threaded-midpoint [getopt]
  "The X, Y and Z coordinates of the middle of the threaded rod."
  (vec (map #(/ % 2)
            (map + (threaded-position-keyboard getopt)
                   (threaded-position-plinth getopt)))))

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

(defn threaded-fasteners [getopt]
  "The full set of connecting threaded rods with nuts for case-side nut bosses."
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
          (if (getopt :wrist-rest :fasteners :mounts :case-side :nuts :bosses :include)
            nut)))))))

(defn- plinth-nut-pockets [getopt]
  "Nut(s) in the plinth-side plate, with pocket(s)."
  (let [d (getopt :wrist-rest :fasteners :diameter)
        ps (getopt :wrist-rest :fasteners :mounts :plinth-side :pocket-scale)
        ph (getopt :wrist-rest :fasteners :mounts :plinth-side :pocket-height)
        nut (->> (misc/iso-hex-nut-model d)
                 (scale [ps ps ps])
                 (rotate [(/ π 2) 0 0]))]
   (translate (threaded-position-plinth getopt)
     (rotate [0 0 (rod-angle getopt)]
       (apply union
         (for [i (range (getopt :wrist-rest :fasteners :amount))]
           (translate (rod-offset getopt i)
             (hull nut (translate [0 0 ph] nut)))))))))

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
  (let [cluster (getopt :wrist-rest :derived :key-cluster)
        case-east-coord (getopt :wrist-rest :derived :key-coord)
        width (getopt :wrist-rest :solid-bridge :width)
        ne (take 2 (key/cluster-position getopt cluster case-east-coord
                            (key/mount-corner-offset getopt SSE)))
        x-west (- (first ne) width)
        plinth-west (getopt :wrist-rest :derived :nw)
        plinth-east (getopt :wrist-rest :derived :ne)
        by-col (getopt :key-clusters :derived :by-cluster cluster :coordinates-by-column)
        south-wall
          (fn [[coord corner]]
            (take 2 (body/wall-corner-position getopt cluster coord corner)))
        case-points
          (filter #(>= (first %) x-west)
            (map south-wall
              (for [column (range (first case-east-coord))  ; Exclusive!
                    corner [SSW SSE]]
                [(first (by-col column)) corner])))
        constrain-x (fn [limit p0 p1]
                      [(limit (first p0) (first p1)) (second p0)])
        nw [x-west (second (first case-points))]
        sw (constrain-x max plinth-west nw)
        se (constrain-x min plinth-east ne)
        bevel 10]
   (extrude-linear
     {:height (getopt :wrist-rest :solid-bridge :height)}
     (polygon
       (concat
         [nw]
         case-points
         [(vec (map - ne [3 0]))
          (vec (map - se [3 0]))
          (vec (map + sw [3 0]))]
         (if (< (first nw) (first sw))
           ; Case-side connection extends beyond the wrist rest.
           ; Add an intermediate point for aesthetics.
           [[(first sw) (+ (second sw) bevel)]]))))))

(defn case-hook [getopt]
  "A model hook. In the solid style, this holds the rest in place."
  (let [cluster (getopt :wrist-rest :derived :key-cluster)
        coord (getopt :wrist-rest :derived :key-coord)
        [x4 y2 _] (key/cluster-position getopt cluster coord
                    (key/mount-corner-offset getopt ESE))
        x3 (- x4 2)
        x2 (- x3 6)
        x1 (- x2 2)
        x0 (- x1 0.6)
        y1 (- y2 6)
        y0 (- y1 1)]
    (extrude-linear
      {:height (getopt :wrist-rest :solid-bridge :height)}
      ;; Draw the outline of the hook moving counterclockwise.
      (polygon [[x0 y1]       ; Left part of the point.
                [x1 y0]       ; Right part of the point.
                [x3 y0]       ; Rightmost contact with the connector.
                [x4 y2]       ; Rightmost contact with the case.
                [x2 y2]]))))  ; Leftmost contact with the case.


;;;;;;;;;;;;;;;;;;;;;;;
;; Main Model Basics ;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn plinth-outline [getopt]
  "A 2D outline, centered."
  (let [prop (partial getopt :wrist-rest :derived)
        chamfer (getopt :wrist-rest :shape :chamfer)]
   (->> (square (prop :base-x) (prop :base-y))
        (offset {:delta (- chamfer)})
        (offset {:delta chamfer :chamfer true}))))

(defn soft-zone [getopt]
  "A 3D mask capturing what would be rubber material, with wide margins."
  (let [height 100
        depth (getopt :wrist-rest :shape :pad :height :below-lip)
        z2 (getopt :wrist-rest :derived :z2)]
   (translate (getopt :wrist-rest :derived :center)
     (union
       (translate (assoc (getopt :mask :center) 2 (+ z2 (/ height 2)))
         (apply cube (assoc (getopt :mask :size) 2 height)))
       (translate [0 0 (- z2 depth)]
         (extrude-linear {:height depth :center false}
           (offset -2
             (plinth-outline getopt))))))))

(defn plinth-maquette [getopt]
  "The overall shape of rubber and plastic as one object in place.
  This does not have all the details."
  (let [prop (partial getopt :wrist-rest :derived)
        z3 (prop :z3)
        margin 0.4
        filepath (getopt :wrist-rest :shape :pad :surface-heightmap)
        surface-size [(prop :base-x)
                      (prop :base-y)
                      (getopt :wrist-rest :shape :pad :height :surface-range)]]
    (union
      (translate (getopt :wrist-rest :derived :center)
        (intersection
          (union  ; A squarish block of marble.
            (translate [0 0 z3]
              (resize surface-size
                (surface filepath :convexity 3)))
            (translate [0 0 (/ z3 2)]
              (cube (prop :base-x) (prop :base-y) z3)))
          (misc/pairwise-hulls  ; A chamfered, gently tapering envelope.
            (translate [0 0 (prop :z4)]
              (extrude-linear {:height 1}
                (offset (- margin)
                  (plinth-outline getopt))))
            (translate [0 0 (prop :z2)]
              (extrude-linear {:height 0.01}
                (offset (- margin)
                  (plinth-outline getopt))))
            (extrude-linear {:height (prop :z1) :center false}
              (plinth-outline getopt)))))
      (case (getopt :wrist-rest :style)
        :threaded (plinth-plate getopt)
        :solid (solid-connector getopt)))))

(defn solid-negative
  "A model of negative space for a solid hook."
  [getopt]
  (union
    (case-hook getopt)
    (body/cluster-wall getopt (getopt :wrist-rest :derived :key-cluster))))


;;;;;;;;;;;;;
;; Outputs ;;
;;;;;;;;;;;;;

(defn plinth-plastic [getopt]
  "The lower portion of a wrist rest, to be printed in a rigid material."
  (body/mask getopt
    (difference
      (plinth-maquette getopt)
      (soft-zone getopt)
      (case (getopt :wrist-rest :style)
        :solid (solid-negative getopt)
        :threaded
          (union
            (threaded-fasteners getopt)
            (plinth-nut-pockets getopt)))
      ;; Two square holes for pouring silicone:
      (translate (vec (map + (getopt :wrist-rest :derived :ne) [-20 -20]))
        (cube 12 12 200))
      (translate (vec (map + (getopt :wrist-rest :derived :sw) [20 20]))
        (cube 12 12 200)))))

(defn rubber-insert [getopt]
  "The upper portion of a wrist rest, to be cast or printed in a soft material."
  (color [0.5 0.5 1 1]
    (intersection
      (soft-zone getopt)
      (plinth-maquette getopt))))

(defn rubber-casting-mould [getopt]
  "A thin shell that goes on top of a wrist plinth temporarily.
  This is for casting silicone into, “in place”. If the wrist rest has
  180° rotational symmetry around the z axis, one mould should
  be enough for both halves’ wrist rests, with tape to prevent leaks."
  (let [prop (partial getopt :wrist-rest :derived)
        thickness 2
        height (+ (- (prop :z4) (prop :z1)) thickness)]
   (rotate [π 0 0]  ; Print bottom-up.
     (difference
       (translate (getopt :wrist-rest :derived :center)
         (translate [0 0 (prop :z1)]
           (extrude-linear {:height height :center false}
             (offset thickness
               (plinth-outline getopt)))))
       (plinth-maquette getopt)))))

(defn unified-preview [getopt]
  "A merged view of a wrist rest. This might be printed in hard plastic for a
  prototype but is not suitable for long-term use: It would typically be too
  hard for ergonomy and does not have a nut pocket for threaded rods."
  (body/mask getopt
    (difference
      (plinth-maquette getopt)
      (union
        (case (getopt :wrist-rest :style)
          :solid (solid-negative getopt)
          :threaded (threaded-fasteners getopt))))))
