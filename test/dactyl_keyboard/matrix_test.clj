(ns dactyl-keyboard.matrix-test
  (:require [clojure.test :refer :all]
            [dactyl-keyboard.cad.matrix :as matrix]))

(deftest test-walk
  (testing "Walking in place."
    (is (= (matrix/walk [0 0]) [0 0])))
  (testing "Walking one step north."
    (is (= (matrix/walk [0 0] :north) [0 1])))
  (testing "Walking two steps north."
    (is (= (matrix/walk [0 0] :north :north) [0 2]))))

(deftest test-trace-edge
  (testing "Walking the edge of a single tile."
    (is (= (first (matrix/trace-edge
                    (fn [coord] (= coord [0 0]))
                    {:coordinates [0 0], :direction :north}))
           {:coordinates [0 0], :direction :north, :corner :outer}))
    (is (= (take 5 (matrix/trace-edge
                     (fn [coord] (= coord [0 0]))
                     {:coordinates [0 0], :direction :north}))
           [{:coordinates [0 0], :direction :north, :corner :outer}
            {:coordinates [0 0], :direction :east, :corner :outer}
            {:coordinates [0 0], :direction :south, :corner :outer}
            {:coordinates [0 0], :direction :west, :corner :outer}
            {:coordinates [0 0], :direction :north, :corner :outer}])))
  (testing "Walking the edge of a mirror L shape."
    (is (= (take 6 (matrix/trace-edge
                     (fn [coord] (contains? #{[0 0] [1 0] [1 1]} coord))
                     {:coordinates [0 0], :direction :north}))
           [{:coordinates [0 0], :direction :north, :corner :outer}
            {:coordinates [0 0], :direction :east, :corner :inner}
            {:coordinates [1 1], :direction :north, :corner :outer}
            {:coordinates [1 1], :direction :east, :corner :outer}
            {:coordinates [1 1], :direction :south, :corner nil}
            {:coordinates [1 0], :direction :south, :corner :outer}]))))

(deftest test-trace-between
  (testing "Walking a straight edge with an explicit stopping position."
    (is (= (matrix/trace-between
             (fn [coord] (not-any? neg? coord))
             {:coordinates [0 0], :direction :west}
             {:coordinates [0 4], :direction :north})
           [{:coordinates [0 0], :direction :west, :corner :outer}
            {:coordinates [0 0], :direction :north, :corner nil}
            {:coordinates [0 1], :direction :north, :corner nil}
            {:coordinates [0 2], :direction :north, :corner nil}
            {:coordinates [0 3], :direction :north, :corner nil}])))
  (testing "Walking a lap around a single tile, with explicit stop."
    (is (= (take 4 (matrix/trace-between
                     (fn [coord] (= coord [0 0]))
                     {:coordinates [0 0], :direction :north}
                     {:coordinates [0 0], :direction :north}))
           [{:coordinates [0 0], :direction :north, :corner :outer}
            {:coordinates [0 0], :direction :east, :corner :outer}
            {:coordinates [0 0], :direction :south, :corner :outer}
            {:coordinates [0 0], :direction :west, :corner :outer}])))
  (testing "Walking a lap around a single tile, with implicit stop."
    (is (= (matrix/trace-between
             (fn [coord] (= coord [0 0])))
           [{:coordinates [0 0], :direction :north, :corner :outer}
            {:coordinates [0 0], :direction :east, :corner :outer}
            {:coordinates [0 0], :direction :south, :corner :outer}
            {:coordinates [0 0], :direction :west, :corner :outer}])))
  (testing "Walking a lap around a single tile, shifted 90º."
    (is (= (matrix/trace-between
             (fn [coord] (= coord [0 0]))
             {:coordinates [0 0], :direction :east})
           [{:coordinates [0 0], :direction :east, :corner :outer}
            {:coordinates [0 0], :direction :south, :corner :outer}
            {:coordinates [0 0], :direction :west, :corner :outer}
            {:coordinates [0 0], :direction :north, :corner :outer}]))))
