;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Placement Utilities                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module consolidates functions on the basis that some minor features,
;;; including foot plates and bottom-plate anchors, can be positioned in
;;; relation to multiple other types of features, creating the need for a
;;; a high-level, delegating placement utility that builds on the rest.

(ns dactyl-keyboard.cad.place
  (:require [clojure.spec.alpha :as spec]
            [scad-clj.model :as model]
            [scad-tarmi.core :refer [abs]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.reckon :as reckon]
            [dactyl-keyboard.generics :refer [directions-to-unordered-corner]]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.param.access :refer [most-specific
                                                  resolve-anchor]]
            [dactyl-keyboard.param.schema :as schema]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Basic Dimensional Facts ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Mounts for neighbouring 1U keys are about 0.75” apart.
(def mount-1u 19.05)

;; Typical 1U keycap width and depth, approximate.
(def key-width-1u 18.25)
(def key-margin (/ (- mount-1u key-width-1u) 2))

;; Mount plates are a bit wider than typical keycaps.
(def mount-width (+ key-width-1u 0.15))
(def mount-depth mount-width)


;;;;;;;;;;;;;;;
;; Functions ;;
;;;;;;;;;;;;;;;

;; Key mounts.

(defn key-length [units] (- (* units mount-1u) (* 2 key-margin)))

(defn mount-corner-offset
  "Produce a mm coordinate offset for a corner of a switch mount."
  [getopt directions]
  (let [subject-x mount-width
        subject-y mount-depth
        neighbour-z (getopt :case :key-mount-thickness)
        area-z (getopt :case :web-thickness)
        m (getopt :case :key-mount-corner-margin)]
    [(* (apply matrix/compass-dx directions) (- (/ subject-x 2) (/ m 2)))
     (* (apply matrix/compass-dy directions) (- (/ subject-y 2) (/ m 2)))
     (+ (/ area-z -2) neighbour-z)]))

(defn- curver
  "Given an angle for progressive curvature, apply it. Else lay keys out flat."
  [subject dimension-n rotate-type delta-fn orthographic
   translate-fn rot-ax-fn getopt cluster coord obj]
  (let [index (nth coord dimension-n)
        most #(most-specific getopt % cluster coord)
        angle-factor (most [:layout rotate-type :progressive])
        neutral (most [:layout :matrix :neutral subject])
        separation (most [:layout :matrix :separation subject])
        delta-f (delta-fn index neutral)
        delta-r (delta-fn neutral index)
        angle-product (* angle-factor delta-f)
        flat-distance (* mount-1u (- index neutral))
        cap-height
          (getopt :keycaps :derived :from-plate-bottom :resting-cap-bottom)
        radius (+ cap-height
                  (/ (/ (+ mount-1u separation) 2)
                     (Math/sin (/ angle-factor 2))))
        ortho-x (- (* delta-r (+ -1 (- (* radius (Math/sin angle-factor))))))
        ortho-z (* radius (- 1 (Math/cos angle-product)))]
   (if (zero? angle-factor)
     (translate-fn (assoc [0 0 0] dimension-n flat-distance) obj)
     (if orthographic
       (->> obj
            (rot-ax-fn angle-product)
            (translate-fn [ortho-x 0 ortho-z]))
       (misc/swing-callables translate-fn radius
                             (partial rot-ax-fn angle-product) obj)))))

(defn- put-in-column
  "Place a key in relation to its column."
  [translate-fn rot-ax-fn getopt cluster coord obj]
  (curver :row 1 :pitch #(- %1 %2) false
          translate-fn rot-ax-fn getopt cluster coord obj))

(defn- put-in-row
  "Place a key in relation to its row."
  [translate-fn rot-ax-fn getopt cluster coord obj]
  (let [style (getopt :key-clusters :derived :by-cluster cluster :style)]
   (curver :column 0 :roll #(- %2 %1) (= style :orthographic)
           translate-fn rot-ax-fn getopt cluster coord obj)))

(declare reckon-feature)

(defn- cluster-origin-finder
  "Compute 3D coordinates for the middle of a key cluster.
  Return a unary function: A partial translator."
  [translate-fn getopt subject-cluster]
  (let [settings (getopt :key-clusters subject-cluster)
        {:keys [anchor offset] :or {offset [0 0 0]}} (:position settings)
        feature (reckon-feature getopt (resolve-anchor getopt anchor))]
   (partial translate-fn (mapv + feature offset))))

(defn- cluster-base
  "Place and tilt passed ‘subject’ as if into a key cluster."
  [translate-fn rotate-fn getopt cluster coord subject]
  (let [[column row] coord
        most #(most-specific getopt (concat [:layout] %) cluster coord)
        center (most [:matrix :neutral :row])
        bridge (cluster-origin-finder translate-fn getopt cluster)]
    (->> subject
         (translate-fn (most [:translation :early]))
         (rotate-fn [(most [:pitch :intrinsic])
                     (most [:roll :intrinsic])
                     (most [:yaw :intrinsic])])
         (put-in-column translate-fn #(rotate-fn [%1 0 0] %2) getopt cluster coord)
         (put-in-row translate-fn #(rotate-fn [0 %1 0] %2) getopt cluster coord)
         (translate-fn (most [:translation :mid]))
         (rotate-fn [(most [:pitch :base])
                     (most [:roll :base])
                     (most [:yaw :base])])
         (translate-fn [0 (* mount-1u center) 0])
         (translate-fn (most [:translation :late]))
         (bridge))))

(def cluster-place
  "A function that puts a passed shape in a specified key matrix position."
  (partial cluster-base maybe/translate maybe/rotate))

(def cluster-position
  "Get coordinates for a key cluster position.
  Using this wrapper, the ‘subject’ argument to cluster-base should be a
  single point in 3-dimensional space, typically an offset in mm from the
  middle of the indicated key."
  (partial cluster-base reckon/translate reckon/rotate))


;; Case walls extending from key mounts.

(defn- wall-segment-offset
  "Compute a 3D offset from one corner of a switch mount to a part of its wall."
  [getopt cluster coord cardinal-direction segment]
  (let [most #(most-specific getopt (concat [:wall] %) cluster coord)
        thickness (most [:thickness])
        bevel-factor (most [:bevel])
        parallel (most [cardinal-direction :parallel])
        perpendicular (most [cardinal-direction :perpendicular])
        {dx :dx dy :dy} (cardinal-direction matrix/compass-to-grid)
        bevel
          (if (zero? perpendicular)
            bevel-factor
            (* bevel-factor
               (/ perpendicular (abs perpendicular))))]
   (case segment
     0 [0 0 0]
     1 [(* dx thickness) (* dy thickness) bevel]
     2 [(* dx parallel) (* dy parallel) perpendicular]
     3 [(* dx (+ parallel thickness))
        (* dy (+ parallel thickness))
        perpendicular]
     4 [(* dx (+ parallel))
        (* dy (+ parallel))
        (+ perpendicular bevel)])))

(defn- wall-vertex-offset
  "Compute a 3D offset from the center of a web post to a vertex on it."
  [getopt directions keyopts]
  (let [xy (/ (getopt :case :key-mount-corner-margin) 2)
        z (/ (getopt :case :key-mount-thickness) 2)]
    (matrix/cube-vertex-offset directions [xy xy z] keyopts)))

(defn wall-corner-offset
  "Combined [x y z] offset from the center of a switch mount.
  By default, this goes to one corner of the hem of the mount’s skirt of
  walling and therefore finds the base of full walls."
  [getopt cluster coordinates
   {:keys [directions segment vertex]
    :or {segment 3, vertex false} :as keyopts}]
  (mapv +
    (if directions
      (mount-corner-offset getopt directions)
      [0 0 0])
    (if directions
      (wall-segment-offset
        getopt cluster coordinates (first directions) segment)
      [0 0 0])
    (if (and directions vertex)
      (wall-vertex-offset getopt directions keyopts)
      [0 0 0])))

(defn wall-corner-position
  "Absolute position of the lower wall around a key mount."
  ([getopt cluster coordinates]
   (wall-corner-position getopt cluster coordinates {}))
  ([getopt cluster coordinates keyopts]
   {:post [(vector? %)
           (spec/valid? ::schema/point-3d %)]}
   (cluster-position getopt cluster coordinates
     (wall-corner-offset getopt cluster coordinates keyopts))))

(defn wall-slab-center-offset
  "Combined [x y z] offset to the center of a vertical wall.
  Computed as the arithmetic average of its two corners."
  [getopt cluster coordinates direction]
  (letfn [(c [turning-fn]
            (wall-corner-offset getopt cluster coordinates
              {:directions [direction (turning-fn direction)]}))]
    (vec (map / (vec (map + (c matrix/left) (c matrix/right))) [2 2 2]))))

(defn- wall-edge
  "Produce a sequence of corner posts for the upper or lower part of the edge
  of one wall slab."
  [post-fn getopt cluster upper [coord direction turning-fn]]
  (let [extent (most-specific getopt [:wall direction :extent]
                 cluster coord)
        last-upper-segment (case extent :full 4, :none 0, extent)
        place-post (post-fn getopt cluster coord
                     [direction (turning-fn direction)])]
   (if-not (zero? last-upper-segment)
     (if upper
       (map place-post (range (inc last-upper-segment)))
       (when (= extent :full)
         (map place-post [2 3 4]))))))

(defn cluster-segment-placer
  "Two-level closure for wall edge object placement."
  [shape-fn]
  (fn [getopt cluster coord directions]
    (fn [segment]
      (->>
        (shape-fn getopt)
        (maybe/translate
          (wall-corner-offset getopt cluster coord
            {:directions directions, :segment segment, :vertex false}))
        (cluster-place getopt cluster coord)))))

(defn wall-edge-placer
  [shape-fn]
  (partial wall-edge (cluster-segment-placer shape-fn)))

(defn- cluster-reckoner
  "A function for finding wall edge vertices."
  ([getopt cluster coord directions & {:as keyopts}]
   (fn [segment]
     (cluster-position getopt cluster coord
       (wall-corner-offset getopt cluster coord
         (merge {:directions directions, :segment segment, :vertex true}
                keyopts))))))

(defn cluster-segment-reckon
  [getopt cluster coord directions segment bottom]
  ((cluster-reckoner getopt cluster coord directions :bottom bottom) segment))

(def wall-edge-reckon (partial wall-edge cluster-reckoner))


;; Rear housing.

(defn- housing-segment-offset
  "Compute the [x y z] coordinate offset from a rear housing roof corner."
  [getopt cardinal-direction segment]
  (let [cluster (getopt :case :rear-housing :position :cluster)
        key (partial getopt :case :rear-housing :derived)
        wall (partial wall-segment-offset getopt cluster)]
   (case cardinal-direction
     :west (wall (key :west-end-coord) cardinal-direction segment)
     :east (wall (key :east-end-coord) cardinal-direction segment)
     :north (if (= segment 0) [0 0 0] [0 1 -1]))))

(defn- housing-vertex-offset [getopt directions]
  (let [t (/ (getopt :case :web-thickness) 2)]
    (matrix/cube-vertex-offset directions [t t t] {})))

(defn- housing-corner-coordinates
  "Convert an ordered corner tuple to a 3-tuple of coordinates."
  [getopt corner]
  (getopt :case :rear-housing :derived (directions-to-unordered-corner corner)))

(defn- housing-placement
  "Place passed shape in relation to a corner of the rear housing’s roof."
  [translate-fn getopt corner segment subject]
  (->> subject
       (translate-fn (housing-corner-coordinates getopt corner))
       (translate-fn (housing-segment-offset getopt (first corner) segment))))

(def housing-place
  "Akin to cluster-place but with a rear housing wall segment."
  (partial housing-placement maybe/translate))

(def housing-reckon
  "Akin to cluster-position but with a rear housing wall segment."
  (partial housing-placement reckon/translate))

(defn housing-vertex-reckon
  "Find the exact position of a vertex on a housing cube."
  [getopt corner segment]
  (housing-reckon getopt corner segment (housing-vertex-offset getopt corner)))

(defn housing-opposite-reckon
  "Like housing-vertex-reckon but for the other end of the indicated facing."
  ;; This is just a workaround for fitting the bottom plate.
  ;; It would not be needed if there were an edge walking function like that
  ;; of the cluster walls but for the rear housing: A function that returned
  ;; pairs of vertices on the outside wall.
  [getopt corner segment]
  (let [[dir0 dir1] corner
        other-corner [dir0 (matrix/left (matrix/left dir1))]]
    (housing-reckon getopt corner segment
      (housing-vertex-offset getopt other-corner))))


;; Wrist rests.

(defn- wrist-placement
  "Place passed object like the plinth of the wrist rest."
  [translate-fn rotate-fn getopt obj]
  (->>
    obj
    (rotate-fn [(getopt :wrist-rest :rotation :pitch)
                (getopt :wrist-rest :rotation :roll)
                0])
    (translate-fn (conj (getopt :wrist-rest :derived :center-2d)
                        (getopt :wrist-rest :plinth-height)))))

(def wrist-place (partial wrist-placement maybe/translate maybe/rotate))

(def wrist-reckon (partial wrist-placement reckon/translate reckon/rotate))

(defn- remap-outline
  [getopt base-xy outline-key]
  (let [index (.indexOf (getopt :wrist-rest :derived :outline :base) base-xy)]
    (nth (getopt :wrist-rest :derived :outline outline-key) index)))

(defn- wrist-lip-coord
  [getopt xy outline-key]
  {:post [(spec/valid? ::schema/point-3d %)]}
  (let [nxy (remap-outline getopt xy outline-key)]
    (wrist-reckon getopt (conj nxy (getopt :wrist-rest :derived :z1)))))

(defn wrist-segment-coord
  "Take an xy coordinate pair as in the 2D wrist-rest spline outline and a
  segment ID number as for a case wall.
  Return vertex coordinates for the corresponding point on the plastic plinth
  of a wrist rest, in its final position.
  Segments extend outward and downward. Specifically, segment 0 is at
  the top of the lip, segment 1 is at the base of the lip, segment 2 is at
  global floor level, and all other segments are well below floor level to
  ensure that they fall below segment 1 even on a low and tilted rest."
  [getopt xy segment]
  {:pre [(vector? xy), (integer? segment)]
   :post [(spec/valid? ::schema/point-3d %)]}
  (case segment
    0 (wrist-reckon getopt (conj xy (getopt :wrist-rest :derived :z2)))
    1 (wrist-lip-coord getopt xy :lip)
    (let [[x y z] (wrist-segment-coord getopt xy 1)]
      [x y (if (= segment 2) 0.0 -100.0)])))

(defn wrist-segment-naive
  "Use wrist-segment-coord with a layer of translation from the naïve/relative
  coordinates initially supplied by the user to the derived base.
  Also support outline keys as an alternative to segment IDs, for bottom-plate
  fasteners."
  [getopt naive-xy outline-key segment]
  (let [translator (getopt :wrist-rest :derived :relative-to-base-fn)
        aware-xy (translator naive-xy)]
    (if (some? outline-key)
      (wrist-lip-coord getopt aware-xy outline-key)
      (wrist-segment-coord getopt aware-xy segment))))

(defn- wrist-block-placement
  "Place a block for a wrist-rest mount."
  ;; TODO: Rework the block model to provide meaningful support for corner and
  ;; segment. Those parameters are currently ignored.
  [translate-fn rotate-fn getopt mount-index side-key corner segment obj]
  {:pre [(integer? mount-index) (keyword? side-key)]}
  (let [prop (partial getopt :wrist-rest :mounts mount-index :derived)]
    (->>
      obj
      (rotate-fn [0 0 (prop :angle)])
      (translate-fn (prop side-key)))))

(def wrist-block-place
  (partial wrist-block-placement maybe/translate maybe/rotate))

(def wrist-block-reckon
  (partial wrist-block-placement reckon/translate reckon/rotate))

;; Generalizations.

(defn- reckon-feature
  "A convenience for placing stuff in relation to other features.
  Differents parts of a feature can be targeted with keyword parameters.
  Return a vector of three numbers.
  Generally, the vector refers to what would be the middle of the outer wall
  of a feature. For keys, rear housing and wrist-rest mount blocks, this
  is the middle of a wall post. For the perimeter of the wrist rest, it’s a
  vertex on the surface and the corner argument is ignored.
  Any offset passed to this function will be interpreted in the native context
  of each feature placement function, with varying results."
  [getopt {:keys [type  ; Mandatory in all cases.
                  cluster  ; Keys only.
                  mount-index side-key  ; Wrist-rest mounts only.
                  coordinates  ; Keys and wrist-rest perimeter.
                  outline-key  ; Wrist-rest perimeter only.
                  corner segment offset]
           :or {segment 3, offset [0 0 0]}}]
  {:pre [(keyword? type)
         (integer? segment)
         (vector? offset)
         (spec/valid? ::schema/point-3d offset)]
   :post [(vector? %)
          (spec/valid? ::schema/point-3d %)]}
  (case type
    :origin offset
    :rear-housing (housing-reckon getopt corner segment offset)
    :wr-perimeter (wrist-segment-naive getopt coordinates outline-key segment)
    :wr-block (wrist-block-reckon getopt mount-index side-key corner segment offset)
    :key
      (if (some? corner)
        ;; Corner named. By default, the target feature is the outermost wall.
        (wall-corner-position getopt cluster coordinates
          {:directions corner :segment segment})
        ;; No corner named. The target feature is the middle of the key mounting plate.
        (cluster-position getopt cluster coordinates offset))))

(defn reckon-from-anchor
  "Find a position corresponding to a named point."
  [getopt anchor extras]
  {:pre [(keyword? anchor) (map? extras)]}
  (reckon-feature getopt (merge (resolve-anchor getopt anchor) extras)))

(defn reckon-with-anchor
  "Produce coordinates for a specific feature using a single map that names
  an anchor."
  [getopt {:keys [anchor] :as opts}]
  {:pre [(keyword? anchor)]}
  (reckon-from-anchor getopt anchor opts))

(defn offset-from-anchor
  "Apply an offset from a user configuration to the output of
  reckon-with-anchor, instead of passing it as an input.
  The results are typically more predictable than passing the offset to
  reckon-with-anchor, being simple addition at a late stage.
  This function also supports explicit 2-dimensional inputs and outputs."
  [getopt opts dimensions]
  (let [base-3d (reckon-with-anchor getopt (dissoc opts :offset))
        base-nd (subvec base-3d 0 dimensions)
        offset-nd (get opts :offset (take dimensions (repeat 0)))]
    (mapv + base-nd offset-nd)))

(defn into-nook
  "Produce coordinates for translation into requested corner.
  This has strict expectations but can place a feature in relation either
  to the rear housing or a key, as requested by the user."
  [getopt field lateral-shift]
  (let [corner (getopt field :position :corner)
        use-housing (and (getopt :case :rear-housing :include)
                         (getopt field :position :prefer-rear-housing))
        general
          (reckon-from-anchor getopt
            (if use-housing :rear-housing (getopt field :position :anchor))
            {:corner corner})
        to-nook
          (if use-housing
            (let [{dxs :dx dys :dy} (matrix/compass-to-grid (second corner))]
             [(* -1 dxs lateral-shift) (* -1 dys lateral-shift) 0])
            ;; Else don’t bother.
            [0 0 0])
        offset (getopt field :position :offset)]
   (mapv + (misc/z0 general) to-nook offset)))
