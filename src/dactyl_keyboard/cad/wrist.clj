;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Wrist Rest                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.wrist
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [scad-tarmi.core :refer [abs sin cos π]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.threaded :as threaded]
            [thi.ng.geom.bezier :refer [auto-spline2]]
            [thi.ng.geom.core :refer [tessellate vertices bounds]]
            [thi.ng.geom.polygon :refer [polygon2 inset-polygon]]
            [thi.ng.geom.vector :refer [vec2]]
            [dactyl-keyboard.generics :refer [ESE SSE SSW colours]]
            [dactyl-keyboard.cad.body :as body]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.cad.place :as place]
            [dactyl-keyboard.param.access :as access]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plinth and Pad Polyhedrons ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def first-elevation 0.0)

(defn- from-outline
  "Draw a thi.ng polygon based on the user-requested spline."
  [getopt inset]
  (let [outline (getopt :wrist-rest :derived :outline :base)]
    (polygon2 (inset-polygon outline inset))))

(defn- get-resolution
  "Get the effective spline resolution. Default to 1, i.e. no interpolation."
  [getopt & keys]
  (if (getopt :resolution :include)
    (apply (partial getopt :wrist-rest :shape) keys)
    1))

(defn- edge-angles
  "Find all angles needed to compute pad surface edge characteristics.
  Note that, due to rounding errors and floating-point shenanigans, the last of
  these angles will be approximately but not exactly π/2."
  [getopt]
  (let [res (get-resolution getopt :pad :surface :edge :resolution)]
    (mapv #(* (/ % res) (/ π 2)) (range (inc res)))))

(defn- edge-inset
  "Compute a pad surface edge inset in mm."
  [getopt θ]
  (let [maximum (getopt :wrist-rest :shape :pad :surface :edge :inset)]
    (* maximum (- 1 (cos θ)))))

(defn- edge-elevation
  "Compute pad surface elevation."
  [getopt θ]
  (let [maximum (getopt :wrist-rest :shape :pad :height :surface-range)]
    (* maximum (sin θ))))

(defn- polygon-step
  "Draw a thi.ng polygon for one level of the pad surface edge."
  [getopt θ]
  (from-outline getopt (edge-inset getopt θ)))

(defn- polygon-faces
  "A list of faces by index number, for one level of a polyhedron.
  Each face has four vertices, numbered in clockwise order."
  ;; From the OpenSCAD manual:
  ;; “All faces must have points ordered in the same direction.
  ;; OpenSCAD prefers clockwise when looking at each face from outside
  ;; inwards.”
  ;; A diagram of how this is implemented here:
  ;;      |    up    |
  ;; -----3----------0-----
  ;;   CW |          | CCW along the polyhedron wall
  ;; -----2----------1-----
  ;;      |   down   |
  [n-prior n-outline vertical-index]
  (for [i (map #(+ n-prior %) (range n-outline))]
    [i
     (- i n-outline)  ; The vertex below i.
     (+ (* (dec vertical-index) n-outline)
        (mod (- (inc i) n-outline) n-outline))  ; Vertex below subsequent.
     (+ (* vertical-index n-outline) (mod (inc i) n-outline))]))  ; Subsequent.

(defn- to-3d [triangles z] (mapv (fn [p] (map #(conj % z) p)) triangles))

(defn- find-face
  "Take point coordinates on a polyhedron level. Translate them to indices."
  [ref triangles]
  (letfn [(to-index [coord] (.indexOf ref coord))
          (to-face [triangle] (map to-index triangle))]
    (mapv to-face triangles)))

(defn- pad-walls
  "Points in a stepped polyhedron and faces that constitute its walls."
  [getopt]
  (let [outline (getopt :wrist-rest :derived :outline :base)]
    (reduce
      (fn [[points faces] index]
        (let [θ (nth (edge-angles getopt) index)
              elevation (edge-elevation getopt θ)
              poly (polygon-step getopt θ)]
          [(concat points (map #(conj % elevation) (:points poly)))
           (concat faces (polygon-faces (count points) (count outline) index))]))
      [(map #(conj % first-elevation) outline) []]
      (map inc (range (get-resolution getopt :pad :surface :edge :resolution))))))

(defn- transition-triangles
  "thi.ng triangles at the plinth-to-pad transition."
  [getopt]
  (let [base-polygon (getopt :wrist-rest :derived :base-polygon)
        triangles (tessellate base-polygon)]
    (if (nil? triangles)
      (throw (ex-info
               "Unable to tessellate wrist rest base. Check main points."
               {:base-polygon base-polygon})))
    triangles))

(defn- pad-surface-polyhedron
  "A scad-clj polyhedron for the basic surface shape of a rubber pad."
  [getopt]
  (let [outline (getopt :wrist-rest :derived :outline :base)
        base-polygon (getopt :wrist-rest :derived :base-polygon)
        last-elevation (getopt :wrist-rest :derived :pad-surface-height)
        floor-triangles (transition-triangles getopt)
        ceiling-triangles
          (tessellate (polygon-step getopt (last (edge-angles getopt))))
        [points wall-faces] (pad-walls getopt)]
    (if (nil? ceiling-triangles)
      (throw (ex-info
               (str "Unable to tessellate the top of the wrist rest. "
                    "The outline of the wrist rest is probably too narrow for "
                    "the maximum inset and/or the spline resolution is too "
                    "high.")
               {:base-polygon base-polygon,
                :inset (getopt :wrist-rest :shape :pad :surface :edge :inset)})))
    (polyhedron
      points
      (concat wall-faces
              ;; Additional faces for the floor.
              (find-face points (to-3d floor-triangles first-elevation))
              ;; Additional faces for the ceiling (reversed to go clockwise).
              (find-face points
                (mapv reverse (to-3d ceiling-triangles last-elevation))))
      :convexity 2)))

(defn- rubber-surface-heightmap
  "A heightmap horizontally centered on the world origin."
  [getopt]
  (resize (conj
            (getopt :wrist-rest :derived :bound-size)
            (getopt :wrist-rest :derived :pad-surface-height))
    (surface (getopt :wrist-rest :shape :pad :surface :heightmap :filepath)
             :convexity 3)))

(defn- rubber-bottom
  "Parts of the rubber pad below its upper surface."
  [getopt]
  (let [prop (partial getopt :wrist-rest :derived)
        intermediate (getopt :wrist-rest :shape :pad :height :lip-to-surface)
        depth (getopt :wrist-rest :shape :pad :height :below-lip)
        width (getopt :wrist-rest :shape :lip :width)]
    (union
      (translate [0 0 (prop :z2)]
        (extrude-linear {:height intermediate, :center false}
          (polygon (prop :outline :base))))
      (translate [0 0 (- (prop :z2) depth)]
        (extrude-linear {:height depth, :center false}
          (polygon (inset-polygon (prop :outline :base) width)))))))

(defn- rubber-body
  [getopt]
  (union
    (rubber-bottom getopt)
    (translate [0 0 (getopt :wrist-rest :derived :z3)]
      (maybe/intersection
        (pad-surface-polyhedron getopt)
        (when (getopt :wrist-rest :shape :pad :surface :heightmap :include)
          (rubber-surface-heightmap getopt))))))

(defn- temporary-polywalls
  "Points and faces as if for an OpenSCAD polygon, but the points are
  meant to be moved around later."
  [getopt indices]
  (let [outline (getopt :wrist-rest :derived :outline :base)]
    (reduce
      (fn [[points-3d faces] index]
        [(concat points-3d (map #(conj % index) outline))
         (if (zero? index)  ; Don’t build faces from below the first index.
           faces
           (concat faces (polygon-faces (count points-3d) (count outline) index)))])
      [[] []]
      indices)))

(defn- temporary-polygon
  "Temporary points and permanent faces for an OpenSCAD polygon."
  [getopt indices]
  (let [[tmp-points wall-faces] (temporary-polywalls getopt indices)
        triangles (transition-triangles getopt)]
    [tmp-points
     (concat
       wall-faces
       ;; Additional faces for the floor.
       (find-face tmp-points (to-3d triangles (first indices)))
       ;; Additional faces for the ceiling (reversed to go clockwise).
       (find-face tmp-points (mapv reverse (to-3d triangles (last indices)))))]))

(defn- move-points [base mapping] (mapv #(get mapping % %) base))

(defn- plinth-mapping
  "Take a list of 3D points. Map their z coordinate to a segment ID. Return a
  hash map translating the list into the shape of a wrist-rest plinth body."
  [getopt tmp-points segments]
  (let [index-to-seg (apply merge (map-indexed hash-map segments))]
    (reduce
      (fn [hmap p]
        (let [xy (vec (take 2 p))
              segment (get index-to-seg (int (last p)))]
          (when (nil? segment)
            (throw (ex-info "Error indexing plinth segments"
                            {:map index-to-seg, :point p})))
          (assoc hmap p (place/wrist-segment-coord getopt xy segment))))
      {}
      tmp-points)))

(defn- plinth-polyhedron
  [getopt]
  (let [segments [3 1 0]
        [points faces] (temporary-polygon getopt (vec (range (count segments))))]
    (polyhedron
      (move-points points (plinth-mapping getopt points segments))
      faces
      :convexity 2)))

(defn- mould-mapping
  "Like plinth-mapping but for the exterior of the mould."
  ;; This is not 100% safe. The larger the mould thickness, the larger the
  ;; probability that remapping the standard transition triangles will not be
  ;; legal.
  [getopt tmp-points]
  (let [prop (partial getopt :wrist-rest :derived)
        base (prop :outline :base)
        mould (prop :outline :mould)
        z1 (prop :outline :mould)]
    (reduce
      (fn [hmap [ox oy oz]]
        (let [[nx ny] (nth mould (.indexOf base [ox oy]))
              nz (prop (if (zero? oz) :z1 :z5))]
          (assoc hmap [ox oy oz] (place/wrist-place getopt [nx ny nz]))))
      {}
      tmp-points)))

(defn- splined
  "The 2D coordinates along a closed spline through passed points."
  [getopt points]
  (let [res (get-resolution getopt :spline :resolution)]
    (butlast (vertices (auto-spline2 (mapv vec2 points) true) res))))


;;;;;;;;;;;;;;;;;;;
;; Miscellaneous ;;
;;;;;;;;;;;;;;;;;;;


(defn sprues
  "Place the sprue module according to user configuration."
  [getopt]
  (apply maybe/union
    (map (place/wrist-module-placer getopt :sprue "sprue_negative")
         (getopt :wrist-rest :sprues :positions))))

(defn sprue-negative
  "A model of a sprue. This assumes that wrist-rest rotation is modest."
  [getopt]
  (let [height (getopt :wrist-rest :derived :z5)]
    (translate [0 0 (/ height 2)]
      (cylinder (/ (getopt :wrist-rest :sprues :diameter) 2)
                height))))

(defn- get-block-alias
  [getopt mount-index]
  (reduce
    (fn [coll [alias side-key]]
      (merge coll
        {alias {:type :wr-block
                :mount-index mount-index
                :side-key side-key}}))
    {}
    (getopt :wrist-rest :mounts mount-index :blocks :aliases)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration Interface ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn collect-point-aliases
  "Collect naïve coordinates of named main points. These coordinates will
  require mapping onto the splined outline later. This is an awkward
  consequence of aliases being collected before wrist-rest properties are
  derived, and of indices changing with the actual spline operation."
  [getopt]
  (reduce
    (fn [coll point-properties]
      (if-let [alias (:alias point-properties)]
        (merge coll
          {alias {:type :wr-perimeter
                  :coordinates (:position point-properties)}})
        coll))
    {}
    (getopt :wrist-rest :shape :spline :main-points)))

(defn collect-block-aliases
  [getopt]
  (reduce
    (fn [coll mount-index]
      (merge coll (get-block-alias getopt mount-index)))
    {}
    (range (count (getopt :wrist-rest :mounts)))))

(defn derive-properties
  "Derive certain properties from the base configuration.
  z-level properties herein do not take rotation of the rest into account."
  [getopt]
  ;; In order to compute a bounding rectangle adequate for a height map,
  ;; find a temporary spline using the original coordinates and the
  ;; final resolution.
  (let [raw (mapv :position (getopt :wrist-rest :shape :spline :main-points))
        {bound-sw :p bound-size :size} (bounds (polygon2 (splined getopt raw)))
        bound-center (mapv #(/ % 2) bound-size)
        around-origin (fn [p] (mapv - p bound-sw bound-center))
        ;; Draw the outline anew, now centered on the origin.
        ;; This obviates moving the pad before rotating it.
        raw-outline (splined getopt (mapv around-origin raw))
        inset (fn [n] (:points (polygon2 (inset-polygon (vec raw-outline) n))))
        lip-inset (getopt :wrist-rest :shape :lip :inset)
        z2 (getopt :wrist-rest :plinth-height)
        z1 (- z2 (getopt :wrist-rest :shape :lip :height))
        getpad (partial getopt :wrist-rest :shape :pad :height)
        pad-middle (getpad :lip-to-surface)
        z3 (+ z2 pad-middle)
        ;; The height of the pad surface is not the nominal :surface-range,
        ;; but that number adjusted by an imprecise multiplication with sin π/2.
        ;; This is because the functions that builds polyhedron faces must be
        ;; able to precisely identify elevation figures like this one.
        pad-above (edge-elevation getopt (last (edge-angles getopt)))
        z4 (+ z3 pad-above)
        z5 (+ z4 (getopt :wrist-rest :mould-thickness))
        absolute-ne
          (place/offset-from-anchor getopt (getopt :wrist-rest :position) 2)
        absolute-center (mapv - absolute-ne bound-center)]
   {:base-polygon (polygon2 raw-outline)
    :relative-to-base-fn around-origin
    :outline
      {:base (inset 0)
       :lip (inset lip-inset)
       :mould (inset (+ lip-inset (- (getopt :wrist-rest :mould-thickness))))
       :sprue (inset (getopt :wrist-rest :sprues :inset))
       :bottom (inset (getopt :wrist-rest :bottom-plate :inset))}
    :bound-size bound-size
    :center-2d absolute-center
    :pad-surface-height pad-above
    :z5 z5     ; Base of the mould (as positioned for printing, not use).
    :z4 z4     ; Peak of the entire rest. Top of silicone pad.
    :z3 z3     ; Silicone-to-silicone transition at base of heightmap.
    :z2 z2     ; Top of lip. Plastic-to-silicone material transition.
    :z1 z1}))  ; Bottom of lip. All plastic.

(defn derive-mount-properties
  "Derive properties for one connection between the case and wrist rest."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)
        anchoring (prop :anchoring)
        threaded-center-height
          ;; The mid-point height of the first threaded fastener.
          (+ (/ (prop :fasteners :diameter) 2)
             (prop :fasteners :height :first))
        to-3d #(misc/pad-to-3d % threaded-center-height)
        ofa #(place/offset-from-anchor getopt (prop :blocks % :position) 2)
        case-side (to-3d (ofa :case-side))
        plinth-side
          ;; Find the position of the plinth-side block.
          (to-3d
            (case anchoring
              :mutual (ofa :plinth-side)
              :case-side
                (let [θ (prop :angle)
                      d0 (/ (prop :blocks :case-side :depth) 2)
                      d1 (prop :blocks :distance)
                      d2 (/ (prop :blocks :plinth-side :depth) 2)
                      d (+ d0 d1 d2)]
                  (mapv + case-side [(* d (cos θ)), (* d (sin θ))]))))
        angle
          (case anchoring
            :case-side (prop :angle)  ; The fixed angle supplied by the user.
            :mutual  ; Compute the angle from the position of the blocks.
              (Math/atan (apply / (reverse (map - (take 2 plinth-side)
                                                  (take 2 case-side))))))]
    {:angle angle
     :threaded-center-height threaded-center-height
     :case-side case-side
     :plinth-side plinth-side
     ;;  X, Y and Z coordinates of the middle of the first threaded rod:
     :midpoint (mapv #(/ % 2) (map + case-side plinth-side))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Threaded Connector Variant ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn threaded-rod
  "An unthreaded model of a threaded cylindrical rod connecting the keyboard
  and wrist rest."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (->>
      (cylinder (/ (prop :fasteners :diameter) 2) (prop :fasteners :length))
      (rotate [0 (/ π 2) (prop :derived :angle)])
      (translate (prop :derived :midpoint)))))

(defn- rod-offset
  "A rod-specific offset relative to the primary rod (index 0).
  The binary form returns the offset of the last rod."
  ([getopt mount-index]
   (rod-offset getopt mount-index
     (dec (getopt :wrist-rest :mounts mount-index :fasteners :amount))))
  ([getopt mount-index rod-index]
   (let [z (getopt :wrist-rest :mounts mount-index :fasteners :height :increment)]
     (mapv #(* rod-index %) [0 0 z]))))

(defn- boss-nut
  "One model of a nut for a case-side nut boss."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (->>
      (threaded/nut :iso-size (prop :fasteners :diameter), :negative true)
      (rotate [(/ π 2) 0 0])
      (translate [0 3 0])
      (rotate [0 0 (prop :derived :angle)])
      (translate (prop :derived :case-side)))))

(defn- mount-fasteners
  "One mount’s set of connecting threaded rods with nuts."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (apply maybe/union
      (for [i (range (prop :fasteners :amount))]
        (translate (rod-offset getopt mount-index i)
          (maybe/union
            (threaded-rod getopt mount-index)
            (if (prop :blocks :case-side :nuts :bosses :include)
              (boss-nut getopt mount-index))))))))

(defn- all-mounts
  [getopt model-fn]
  (apply maybe/union
    (for [i (range (count (getopt :wrist-rest :mounts)))]
      (model-fn getopt i))))

(defn- plinth-nut-pockets
  "Nut(s) in the plinth-side plate, with pocket(s)."
  [getopt mount-index]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)
        d (prop :fasteners :diameter)
        height (prop :blocks :plinth-side :pocket-height)
        compensator (getopt :dfm :derived :compensator)
        nut (->> (threaded/nut :iso-size d, :negative true)
                 (rotate [(/ π 2) 0 (/ π 2)])
                 (compensator d {}))]
    (->>
      (apply union
        (for [i (range (prop :fasteners :amount))]
          (translate (rod-offset getopt mount-index i)
            (hull nut (translate [0 0 height] nut)))))
      (rotate [0 0 (prop :derived :angle)])
      (translate (prop :derived :plinth-side)))))

(defn- block-model
  [getopt mount-index side-key]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)
        g0 (prop :blocks :width)
        g1 (dec g0)
        d0 (prop :blocks side-key :depth)
        d1 (dec d0)]
    (union
      (cube d1 g0 g0)
      (cube d0 g1 g1)
      (cube 1 1 (+ g1 3)))))

(defn- block-in-place
  [getopt mount-index side-key]
  (let [prop (partial getopt :wrist-rest :mounts mount-index)]
    (->>
      (block-model getopt mount-index side-key)
      (rotate [0 0 (prop :derived :angle)])
      (translate
        (mapv + (prop :derived side-key) (rod-offset getopt mount-index)))
      (misc/bottom-hull))))

(defn case-block
  "A plate on the case side for a threaded rod to the keyboard case."
  [getopt mount-index]
  (block-in-place getopt mount-index :case-side))

(defn plinth-block
  "A plate on the plinth side for a threaded rod to the keyboard case."
  [getopt mount-index]
  (block-in-place getopt mount-index :plinth-side))


;;;;;;;;;;;;;
;; Outputs ;;
;;;;;;;;;;;;;


(defn all-case-blocks
  [getopt]
  (all-mounts getopt case-block))

(defn block-pairs
  "Pairs of mount blocks. For use in generating a unifying bottom plate."
  [getopt]
  (reduce
    (fn [coll mount-index]
      (conj coll [(case-block getopt mount-index)
                  (plinth-block getopt mount-index)]))
    []
    (range (count (getopt :wrist-rest :mounts)))))

(defn all-fasteners
  [getopt]
  (all-mounts getopt mount-fasteners))

(defn plinth-positive
  "A maquette of the plastic, rigid portion of wrist rest."
  [getopt]
  (maybe/union
    (difference
      (plinth-polyhedron getopt)
      (place/wrist-place getopt (rubber-bottom getopt)))
    (when (= (getopt :wrist-rest :style) :threaded)
      (all-mounts getopt plinth-block))))

(defn plinth-plastic
  "The lower portion of a wrist rest, to be printed in a rigid material.
  This is complete except for masking and holes for a bottom plate."
  [getopt]
  (maybe/difference
    (plinth-positive getopt)
    (when (= (getopt :wrist-rest :style) :threaded)
      (union
        (all-fasteners getopt)
        (all-mounts getopt plinth-nut-pockets)))
    (when (getopt :wrist-rest :sprues :include)
      (sprues getopt))))

(defn rubber-insert-positive
  "The upper portion of a wrist rest, to be cast or printed in a soft material.
  This model is not fully featured."
  [getopt]
  (maybe/difference
    (color (:rubber colours)
      (place/wrist-place getopt
        (rubber-body getopt)))
    (all-mounts getopt plinth-block)))

(defn unified-preview
  "A merged view of a wrist rest. This might be printed in hard plastic for a
  prototype but is not suitable for long-term use: It would typically be too
  hard for ergonomy and does not have all the details."
  [getopt]
  (union
    (rubber-insert-positive getopt)
    (plinth-positive getopt)))

(defn mould-polyhedron
  "An OpenSCAD polyhedron describing the exterior of the casting mould.
  This could be simplified to an OpenSCAD extrusion of an OpenSCAD polygon,
  since it is presently just one level with straight edges."
  [getopt]
  (let [[points faces] (temporary-polygon getopt [0 1])]
    (polyhedron
      (move-points points (mould-mapping getopt points))
      faces
      :convexity 2)))
