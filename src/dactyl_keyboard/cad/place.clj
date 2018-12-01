;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Placement Utilities                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; This module consolidates functions on the basis that some minor features,
;;; including foot plates and bottom-plate anchors, can be positioned in
;;; relation to multiple other types of features, creating the need for a
;;; a high-level, delegating placement utility that builds on the rest.

(ns dactyl-keyboard.cad.place
  (:require [scad-clj.model :as model]
            [scad-tarmi.core :refer [abs]]
            [scad-tarmi.maybe :as maybe]
            [scad-tarmi.reckon :as reckon]
            [dactyl-keyboard.generics :refer [directions-to-unordered-corner]]
            [dactyl-keyboard.cad.matrix :as matrix]
            [dactyl-keyboard.cad.misc :as misc]
            [dactyl-keyboard.param.access :refer [most-specific]]))


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

(declare cluster-position)

(defn- cluster-origin-finder
  "Compute 3D coordinates for the middle of a key cluster.
  Return a unary function: A partial translator."
  [translate-fn getopt subject-cluster]
  (let [settings (getopt :key-clusters subject-cluster)
        {:keys [key-alias offset], :or {offset [0 0 0]}} (:position settings {})
        from-alias
          (if key-alias
            (let [properties (getopt :key-clusters :derived :aliases key-alias)
                  {:keys [cluster coordinates]} properties]
             (cluster-position getopt cluster coordinates [0 0 0]))
            [0 0 0])
        origin (vec (map + from-alias offset))]
   (partial translate-fn origin)))

(defn- cluster-base
  "Place and tilt passed ‘subject’ as if into a key cluster."
  [translate-fn rot-fn getopt cluster coord subject]
  (let [[column row] coord
        most #(most-specific getopt (concat [:layout] %) cluster coord)
        center (most [:matrix :neutral :row])
        bridge (cluster-origin-finder translate-fn getopt cluster)]
    (->> subject
         (translate-fn (most [:translation :early]))
         (rot-fn [(most [:pitch :intrinsic])
                  (most [:roll :intrinsic])
                  (most [:yaw :intrinsic])])
         (put-in-column translate-fn #(rot-fn [%1 0 0] %2) getopt cluster coord)
         (put-in-row translate-fn #(rot-fn [0 %1 0] %2) getopt cluster coord)
         (translate-fn (most [:translation :mid]))
         (rot-fn [(most [:pitch :base])
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
  (vec
    (map +
      (if directions
        (mount-corner-offset getopt directions)
        [0 0 0])
      (if directions
        (wall-segment-offset
          getopt cluster coordinates (first directions) segment)
        [0 0 0])
      (if (and directions vertex)
        (wall-vertex-offset getopt directions keyopts)
        [0 0 0]))))

(defn wall-corner-position
  "Absolute position of the lower wall around a key mount."
  ([getopt cluster coordinates]
   (wall-corner-position getopt cluster coordinates {}))
  ([getopt cluster coordinates keyopts]
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
  (partial housing-placement (partial map +)))

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

(defn wrist-reckon
  "The position of a corner of a wrist rest’s plinth.
  Translate the segment range used for keys into a very rough equivalent
  for the plinth, pretending that, as with the rear housing and key clusters,
  segments extend outward and downward."
  [getopt corner segment start]
  (let [prop (partial getopt :wrist-rest :derived)
        base (prop (directions-to-unordered-corner corner))
        {:keys [dx dy]} (matrix/compass-to-grid corner)
        chamfer (getopt :wrist-rest :shape :chamfer)]
    (vec (map + start
                (conj base (case segment, 0 (prop :z2), 1 (prop :z1), 0))
                (if (= segment 0)
                  [(* -1 dx chamfer) (* -1 dy chamfer) 0]
                  [0 0 0])))))


;; Generalizations.

(defn- key-anchor
  [getopt {:keys [key-alias corner]}]
  (let [key (getopt :key-clusters :derived :aliases key-alias)
        {:keys [cluster coordinates]} key]
    (wall-corner-position getopt cluster coordinates {:directions corner})))

(defn- reckon-2d-anchor
  "A convenience for placing stuff in relation to other features.
  Return a vec, for predictably conj’ing a zero onto.
  The position refers to what would be the middle of the outer wall post of the
  anchor."
  [getopt {:keys [anchor corner] :or {anchor :key, segment 0} :as opts}]
  (vec (take 2
         (case anchor
           :key (key-anchor getopt opts)
           :rear-housing (housing-reckon getopt corner 1 [0 0 0])
           :wrist-rest (wrist-reckon getopt corner 1 [0 0 0])))))

(defn reckon-2d-offset
  "Determine xy coordinates of some other feature with an offset."
  [getopt {:keys [offset] :or {offset [0 0]} :as opts}]
  (vec (map + (reckon-2d-anchor getopt opts) offset)))

(defn into-nook
  "Produce coordinates for translation into requested corner.
  This has strict expectations but can place a feature in relation either
  to the rear housing or a key, as requested by the user."
  [getopt field lateral-shift]
  (let [corner (getopt field :position :corner)
        use-housing (and (getopt :case :rear-housing :include)
                         (getopt field :position :prefer-rear-housing))
        general
          (reckon-2d-anchor getopt
             {:anchor (if use-housing :rear-housing :key)
              :key-alias (getopt field :position :key-alias)
              :corner corner})
        to-nook
          (if use-housing
            (let [{dxs :dx dys :dy} (matrix/compass-to-grid (second corner))]
             [(* -1 dxs lateral-shift) (* -1 dys lateral-shift) 0])
            ;; Else don’t bother.
            [0 0 0])
        offset (getopt field :position :offset)]
   (vec (map + (conj general 0) to-nook offset))))
