;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Auxiliary Features                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns dactyl-keyboard.cad.aux
  (:require [scad-clj.model :exclude [use import] :refer :all]
            [unicode-math.core :refer :all]
            [dactyl-keyboard.generics :refer :all]
            [dactyl-keyboard.params :refer :all]
            [dactyl-keyboard.cad.misc :refer :all]
            [dactyl-keyboard.cad.matrix :refer :all]
            [dactyl-keyboard.cad.key :refer :all]
            [dactyl-keyboard.cad.case :refer :all]))

;;;;;;;;;;;;;;;;
;; Wrist Rest ;;
;;;;;;;;;;;;;;;;

(defn case-south-wall-xy [[column corner]]
  "An [x y] coordinate pair at the south wall of the keyboard case."
  (take 2 (finger-wall-corner-position (first-in-column column) corner)))

(def wrist-placement-xy-keyboard (case-south-wall-xy [wrist-placement-column SSE]))

(def wrist-plinth-xy-nw (vec (map + wrist-placement-xy-keyboard wrist-placement-offset)))
(def wrist-plinth-xy-ne [(+ (first wrist-plinth-xy-nw) wrist-plinth-width)
                           (second wrist-plinth-xy-nw)])
(def wrist-plinth-xy-sw (vec (map - wrist-plinth-xy-nw [0 wrist-plinth-length])))
(def wrist-plinth-xy-se (vec (map - wrist-plinth-xy-ne [0 wrist-plinth-length])))

(def wrist-grid-unit-x 4)
(def wrist-grid-unit-y wrist-plinth-length)
(def wrist-node-size 2)
(def wrist-wall-z-offset -1)
(defn wrist-wall-offsetter [coordinates corner] [0 wrist-wall-z-offset])

(def last-wrist-column (int (/ wrist-plinth-width wrist-grid-unit-x)))
(def last-wrist-row (int (/ wrist-plinth-length wrist-grid-unit-y)))
(def all-wrist-columns (range 0 (+ last-wrist-column 1)))
(def all-wrist-rows (range 0 (+ last-wrist-row 1)))

(defn wrist? [[column row]]
  "True if specified node in wrist rest surface has been requested."
  (and (<= 0 column last-wrist-column) (<= 0 row last-wrist-row)))

(def wrist-node
  (let [h (+ (abs wrist-wall-z-offset) plate-thickness)
        dz (- (- (/ h 2) plate-thickness))]
    (translate [0 0 dz] (cube wrist-node-size wrist-node-size h))))

(defn node-corner-offset [directions]
  "Produce a translator for getting to one corner of a wrist rest node."
  (general-corner
    wrist-node-size wrist-node-size web-thickness plate-thickness directions))

(defn node-corner-post [directions]
  "A truncated post shape that comes offset for one corner of a wrist rest node."
  (translate (node-corner-offset directions)
    (cube corner-post-width corner-post-width 0.01)))

(def wrist-threaded-position-keyboard
  (conj (vec (map + (case-south-wall-xy [wrist-threaded-column SSE])
                    wrist-threaded-offset-keyboard))
        wrist-threaded-height))

(def wrist-threaded-position-plinth
  (conj (vec (map + wrist-plinth-xy-nw wrist-threaded-offset-plinth))
        wrist-threaded-height))

(def wrist-threaded-midpoint
  "The X, Y and Z coordinates of the middle of the threaded rod."
  (vec (map #(/ % 2)
            (map + wrist-threaded-position-keyboard wrist-threaded-position-plinth))))

(def wrist-threaded-angle
  "The angle (from the y axis) of the threaded rod."
  (let [d (map abs (map - wrist-threaded-position-keyboard wrist-threaded-position-plinth))]
    (Math/atan (/ (first d) (second d)))))

(def wrist-threaded-rod
  "An unthreaded model of a theaded cylindrical rod connecting the keyboard and wrist rest."
  (translate wrist-threaded-midpoint
    (rotate [(/ π 2) 0 wrist-threaded-angle]
      (cylinder (/ wrist-threaded-fastener-diameter 2) wrist-threaded-fastener-length))))

(def case-wrist-plate
  "A plate for attaching a threaded rod to the keyboard case.
  This is intended to have nuts on both sides and therefore has no bosses."
  (let [g0 wrist-threaded-anchor-girth
        g1 (dec g0)
        d0 wrist-threaded-anchor-depth
        d1 (dec d0)]
    (translate wrist-threaded-position-keyboard
      (rotate [0 0 wrist-threaded-angle]
        (bottom-hull
          (cube g0 d1 g0)
          (cube g1 d0 g1))))))

(def wrist-solid-connector
  (let [xy-west wrist-plinth-xy-nw
        xy-east (vec (map + (case-south-wall-xy [last-finger-column SSE]) wrist-placement-offset))
        bevel 10
        p0 (case-south-wall-xy [(- wrist-placement-column 1) SSE])]
   (extrude-linear
     {:height wrist-solid-connector-height}
     (polygon
       (concat
         [p0]
         (map case-south-wall-xy
           (for [column (filter (partial <= wrist-placement-column) all-finger-columns)
                 corner [SSW SSE]]
             [column corner]))
         [[(first xy-east) (second xy-west)]
          xy-west
          [(first xy-west) (- (second p0) bevel)]
          [(- (first xy-west) bevel) (second p0)]]
    )))))

(defn wrist-node-place [[column row] shape]
  (let [μ 0
        M (- column (* 2 (/ last-wrist-column 3)))  ; Placement of curvature.
        σ wrist-rest-σ
        θ wrist-rest-θ
        z (* wrist-z-coefficient θ)
        ]
  (->> shape
       ((rotator-vector [0 (* θ (𝒩′ M μ σ)) 0]))
       (translate [0 0 (- (* z (𝒩 M μ σ)))])
       (translate [(* column wrist-grid-unit-x) (* row wrist-grid-unit-y) 0])
       (translate [(first wrist-plinth-xy-nw)
                   (- (second wrist-plinth-xy-nw) wrist-plinth-length)
                   wrist-plinth-height])
       )))

(def wrist-nodes
  (apply union
    (map #(bottom-hull (wrist-node-place % wrist-node))
      (coordinate-pairs all-wrist-columns all-wrist-rows))))

(def wrist-surface-elements
  (walk-and-web all-wrist-columns all-wrist-rows wrist? wrist-node-place node-corner-post))

(def wrist-bevel-elements
  (walk-and-wall
    [[0 0] :north]
    [[0 0] :north]
    (fn [[column row]] (and (<= 0 column last-wrist-column) (<= 0 row last-wrist-row)))
    (partial bevel-only wrist-node-place wrist-wall-offsetter node-corner-post)))

(def wrist-bevel-3d-model (apply union wrist-bevel-elements))

(def wrist-bevel-2d-outline (hull (project wrist-bevel-3d-model)))

(def wrist-plinth-zone-rubber
  (union
    (translate [0 0 (+ 50 wrist-silicone-starting-height)] (cube 500 500 100))
    (translate [0 0 (- wrist-silicone-starting-height (/ wrist-silicone-trench-depth 2))]
      (extrude-linear {:height wrist-silicone-trench-depth}
        (offset -2 wrist-bevel-2d-outline)))))

(def wrist-plinth-shape
  "The overall shape of rubber and plastic as one object."
  (letfn [(shadow [shape]
            (translate [0 0 wrist-plinth-base-height]
              (extrude-linear {:height 0.01}
                (offset 1 (project shape)))))
          (hull-to-base [shape]
            (hull shape (shadow shape)))]
   (union
     (apply union (map hull-to-base wrist-surface-elements))
     (apply union (map hull-to-base wrist-bevel-elements))
     (bottom-hull (shadow wrist-bevel-3d-model)))))

(def wrist-plinth-plastic
  "The lower portion of a wrist rest, to be printed in a rigid material."
  (difference
    wrist-plinth-shape
    wrist-plinth-zone-rubber
    (if (= wrist-rest-style :threaded)
      ;; A hex nut pocket:
      (translate wrist-threaded-position-plinth
        (rotate [0 0 wrist-threaded-angle]
          (hull
            (rotate [(/ π 2) 0 0]
              (iso-hex-nut-model wrist-threaded-fastener-diameter))
            (translate [0 0 100]
              (rotate [(/ π 2) 0 0]
                (iso-hex-nut-model wrist-threaded-fastener-diameter)))))))
    ;; Two square holes for pouring silicone:
    (translate (vec (map + wrist-plinth-xy-ne [-10 -10]))
      (extrude-linear {:height 200} (square 10 10)))
    (translate (vec (map + wrist-plinth-xy-sw [10 10]))
      (extrude-linear {:height 200} (square 10 10)))))

(def wrist-plinth-rubber
  "The upper portion of a wrist rest, to be cast or printed in a soft material."
  (color [0.5 0.5 1 1] (intersection wrist-plinth-zone-rubber wrist-plinth-shape)))

(def wrist-rubber-casting-mould
  "A thin shell that goes on top of a wrist plinth temporarily.
  This is for casting silicone into, “in place”, from below. As long as the
  wrist rest has 180° rotational symmetry around the z axis, one mould should
  be enough for both halves’ wrist rests, with tape to prevent leaks."
  (let [dz (- wrist-plinth-height wrist-plinth-base-height)]
    (difference
      (translate [0 0 (+ wrist-plinth-base-height (/ dz 2))]
        (extrude-linear {:height (+ dz 3)}
          (offset 2 wrist-bevel-2d-outline)))
      wrist-plinth-shape)))

(def case-wrist-hook
  "A model hook. In the solid style, this holds the wrest in place."
  (let [[column row] (first-in-column last-finger-column)
        [x4 y2 _] (finger-key-position [column row] (mount-corner-offset ESE))
        x3 (- x4 2)
        x2 (- x3 6)
        x1 (- x2 2)
        x0 (- x1 0.6)
        y1 (- y2 6)
        y0 (- y1 1)]
    (extrude-linear
      {:height wrist-solid-connector-height}
      ;; Draw the outline of the hook moving counterclockwise.
      (polygon [[x0 y1]  ; Left part of the point.
                [x1 y0]  ; Right part of the point.
                [x3 y0]  ; Rightmost contact with the connector.
                [x4 y2]  ; Rightmost contact with the case.
                [x2 y2]] ; Leftmost contact with the case.
                ))))

(defn wrist-rest-right [exploded]
  "Right-hand-side wrist support model(s)."
  (intersection
    (difference
      (union
        (if (= wrist-rest-style :solid) wrist-solid-connector)
        (if exploded
          (union
            (translate [0 0 70] wrist-rubber-casting-mould)
            (translate [0 0 30] wrist-plinth-rubber)
            wrist-plinth-plastic)
          wrist-plinth-shape))
      (union
        (case wrist-rest-style
          :solid (union case-wrist-hook case-walls-for-the-fingers)
          :threaded wrist-threaded-rod)))
    (translate [0 0 500] (cube 1000 1000 1000))))

;;;;;;;;;;;;;;;;;;;;;
;; Microcontroller ;;
;;;;;;;;;;;;;;;;;;;;;

;; MicroUSB female:
(def micro-usb-width 7.5)
(def micro-usb-length 5.3)
(def micro-usb-height 2.8)

(def micro-usb-receptacle
  "A USB female."
  (color [0.5 0.5 0.5 1]
    (cube micro-usb-width micro-usb-length micro-usb-height)))
(def micro-usb-channel (cube 7.8 10 2.8))

;; Teensy MCU: Not fully supported at the moment. Pro Micro is hardcoded below.
(def teensy-width 20)
(def teensy-height 12)
(def teensy-length 33)
(def teensy2-length 53)

;; Arduino Pro Micro MCU:
(def promicro-width 18)
(def promicro-length 33)
(def promicro-thickness 1.65)

(def mcu-thickness-tolerance 0.3)

(def mcu-microusb-offset
  "A millimetre offset between an MCU PCB and a micro-USB female."
  [0
   (+ (/ promicro-length 2) 1 (/ micro-usb-length -2))
   (+ (/ promicro-thickness 2) (/ micro-usb-height 2))])

(def promicro-pcb (cube promicro-width promicro-length promicro-thickness))
(def mcu-height-above-ground 2)
(def mcu-model
  (union
    (translate mcu-microusb-offset micro-usb-receptacle)
    (color [26/255, 90/255, 160/255 1] promicro-pcb)))

(def mcu-space-requirements
  "Negative space for an MCU in use, including USB connectors."
  (let [alcove 10]
    (union
      (translate mcu-microusb-offset
        (union
          ;; Female USB connector:
          micro-usb-channel
          ;; Male USB connector:
          (hull
            (translate [0 4 0] (cube 15 1 10))
            (translate [0 9 0] (cube 20 1 15)))))
      ;; An alcove in the inner wall, because a blind notch is hard to clean:
      (translate [0 (/ (- promicro-length alcove) 2) 0]
        (cube (+ promicro-width 5) alcove (+ promicro-thickness (* 2 micro-usb-height))))
      ;; The negative of the PCB, just to put a notch in the spine:
      (cube promicro-width promicro-length (+ promicro-thickness mcu-thickness-tolerance)))))

(def mcu-finger-coordinates (last-in-column mcu-finger-column))
(defn mcu-position [shape]
  "Transform passed shape into the reference frame for an MCU holder."
  (let [[x y] (take 2
                (finger-key-position
                  mcu-finger-coordinates
                  (finger-wall-offset mcu-finger-coordinates mcu-connector-direction)))]
   (->>
     shape
     ;; Put the USB end of the PCB at [0, 0].
     (translate [0 (/ promicro-length -2) 0])
     ;; Flip it to stand on the long edge for soldering access.
     ;; Have the components and silk face the interior of the housing.
     (rotate (/ π 2) [0 1 0])
     ;; Lift it to ground level.
     (translate [0 0 (/ promicro-width 2)])
     ;; Lift it a little further, to clear a support structure.
     (translate [0 0 mcu-height-above-ground])
     ;; Turn it around the z axis to point USB in the ordered direction.
     (rotate (- (compass-radians mcu-connector-direction)) [0 0 1])
     ;; Move it to the ordered case wall.
     (translate [x y 0])
     ;; Tweak as ordered.
     (translate mcu-offset))))

(def mcu-visualization (mcu-position mcu-model))
(def mcu-negative (mcu-position mcu-space-requirements))

;; Holder for MCU:
(def mcu-support
  (let [plinth-width (+ promicro-thickness mcu-thickness-tolerance 1.2)
        plinth-height mcu-height-above-ground
        grip-depth 0.6
        grip-to-base 5
        rev-dir (turning-left (turning-left mcu-connector-direction))
        cervix-coordinates (walk-matrix mcu-finger-coordinates rev-dir rev-dir)]
    (union
      (mcu-position
        (union
          ;; A little gripper to stabilize the PCB horizontally.
          ;; This is intended to be just shallow enough that the outer wall
          ;; will bend back far enough for the installation and is placed to
          ;; avoid covering any of the through-holes.
          (translate
            [0 (+ (/ promicro-length -2) (/ grip-depth 2)) 0]
            (cube (/ promicro-width 2) grip-depth plinth-width))
          ;; A block to support the gripper.
          (translate
            [0 (- (/ promicro-length -2) (/ grip-to-base 2)) 0]
            (cube (/ promicro-width 2) grip-to-base plinth-width))))
      ;; The spine connects a sacrum, which is the main body of the plinth
      ;; at ground level, with a cervix that helps support the finger web.
      (hull
        (mcu-position
          (translate
            [0 (- (/ promicro-length -2) grip-to-base) 0]
            (cube (/ promicro-width 2) grip-to-base plinth-width)))
        (finger-key-place cervix-coordinates
          (mount-corner-post [mcu-connector-direction (turning-left rev-dir)]))
        (finger-key-place cervix-coordinates
          (mount-corner-post [mcu-connector-direction (turning-right rev-dir)]))))))

;;;;;;;;;;;;;;;;
;; Back Plate ;;
;;;;;;;;;;;;;;;;

;; Plate for a connecting beam, rod etc.

(defn backplate-place [shape]
  (let [coordinates (last-in-column backplate-column)
        position (finger-key-position coordinates (finger-wall-offset coordinates :north))]
   (->>
     shape
     (rotate installation-angle [0 0 1])
     (translate position)
     (translate [0 0 (/ backplate-beam-height -2)])
     (translate backplate-offset))))

(def backplate-shape
  "A mounting plate for a connecting beam."
  (let [height backplate-beam-height
        width (+ backplate-fastener-distance height)
        depth 4
        interior-protrusion 8
        exterior-bevel 1
        interior-bevel 7]
   (hull
     (translate [0 (- interior-protrusion) 0]
       (cube (- width interior-bevel) depth (- height interior-bevel)))
     (cube width depth height)
     (translate [0 exterior-bevel 0]
       (cube (dec width) depth (dec height))))))

(def backplate-fastener-holes
  "Two holes for screws through the back plate."
  (letfn [(hole [x-offset]
            (->>
              (union
                (cylinder (/ backplate-fastener-diameter 2) 25)
                (if include-backplate-boss
                  (translate [0 0 10]
                    (iso-hex-nut-model backplate-fastener-diameter 10))))
              (rotate [(/ π 2) 0 0])
              (translate [x-offset 0 0])
              backplate-place))]
   (union
     (hole (/ backplate-fastener-distance 2))
     (hole (/ backplate-fastener-distance -2)))))

(def backplate-block
  (bottom-hull (backplate-place backplate-shape)))

;;;;;;;;;;;;;;;
;; LED Strip ;;
;;;;;;;;;;;;;;;

(def led-height (+ (/ led-housing-size 2) 5))

(def west-wall-west-points
  (for [row (finger-row-indices 0)
        corner [WSW WNW]]
   (let [[x y _] (finger-wall-corner-position [0 row] corner)]
    [(+ x wall-thickness) y])))

(def west-wall-east-points
  (map (fn [[x y]] [(+ x 10) y]) west-wall-west-points))

(def west-wall-led-channel
  (extrude-linear {:height 50}
    (polygon (concat west-wall-west-points (reverse west-wall-east-points)))))

(defn led-hole-position [ordinal]
  (let [row (first (finger-row-indices 0))
        [x0 y0 _] (finger-wall-corner-position [0 row] WNW)]
   [x0 (+ y0 (* led-pitch ordinal)) led-height]))

(defn led-emitter-channel [ordinal]
  (->> (cylinder (/ led-emitter-diameter 2) 50)
       (rotatev (/ π 2) [0 1 0])
       (translate (led-hole-position ordinal))))

(defn led-housing-channel [ordinal]
  (->> (cube 50 led-housing-size led-housing-size)
       (translate (led-hole-position ordinal))))

(def led-holes
  (let [holes (range led-amount)]
   (union
     (intersection
       west-wall-led-channel
       (apply union (map led-housing-channel holes)))
     (apply union (map led-emitter-channel holes)))))

;;;;;;;;;;;;;;;;
;; Signalling ;;
;;;;;;;;;;;;;;;;

;; 4P4C connector holder:
(def rj9-origin
  (let [c0 [0 (last (finger-row-indices 0))]
        c1 [1 (last (finger-row-indices 1))]
        corner (fn [c] (finger-wall-corner-position c NNW))
        [x0 y0] (take 2 (map + (corner c0) (corner c1)))]
   (map + [0 0 0] [(/ x0 2) (/ y0 2) 0])))

(defn rj9-position [shape]
  (->> shape
       (translate rj9-translation)
       (rotate (deg->rad 36) [0 0 1])
       (translate [(first rj9-origin) (second rj9-origin) 10.5])))

(def rj9-metasocket
  (hull
    (translate [0 1 18] (cube 6 4 1))
    (cube 13.8 12 21)))

(def rj9-socket-tshort (union (translate [0 2 0] (cube 10.78  9 18.38))
                              (translate [0 0 5] (cube 10.78 13  5))))

(def rj9-socket-616e
  "The shape of a 4P4C female connector for use as a negative.
  An actual 616E socket is not symmetric along the x axis. This model of it,
  being intended for mirroring, is deliberately imprecise. It includes a
  channel for the 4 wires entering the case and excludes the vertical bar."
  (translate [0 1 0]
    (union
     (cube 10 11 17.7)
     (translate [0 0 -5] (cube 8 20 7.7)))))

(def rj9-space  (rj9-position rj9-metasocket))

(def rj9-holder (rj9-position (difference rj9-metasocket
                                          rj9-socket-616e)))

;;;;;;;;;;;;;;;;;;;;
;; Minor Features ;;
;;;;;;;;;;;;;;;;;;;;

(def foot-plates
  "Model plates from polygons specified in ‘foot-plate-posts’.
  Each vector specifying a point in a polygon must have finger key coordinates
  and a mount corner identified by a direction tuple. These can be followed by
  a two-dimensional offset for tweaking."
  (letfn [(xy
            ([coordinates directions]
              (xy coordinates directions [0 0 0]))
            ([coordinates directions offset]
              (vec (map +
                (take 2 (finger-wall-corner-position coordinates directions))
                offset))))
          (plate [positions]
            (extrude-linear {:height foot-height :center false}
              (polygon (map (fn [spec] (apply xy spec)) positions))))]
   (apply union (map plate foot-plate-posts))))

;; USB female holder:
;; This is not needed if the MCU has an integrated USB connector and that
;; connector is directly exposed through the case.
(def usb-holder-position
  (let [coordinates [0 0]]
   (finger-key-position coordinates
     (map +
       (wall-segment-offset 2 :north (finger-key-wall-offsets coordinates WNW))
       [0 (/ mount-depth 2) 0]))))
(def usb-holder-size [6.5 10.0 13.6])
(def usb-holder-thickness 4)
(def usb-holder
    (->> (cube (+ (first usb-holder-size) usb-holder-thickness) (second usb-holder-size) (+ (last usb-holder-size) usb-holder-thickness))
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))
(def usb-holder-hole
    (->> (apply cube usb-holder-size)
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))
