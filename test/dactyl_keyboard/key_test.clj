(ns dactyl-keyboard.key-test
  (:require [clojure.test :refer :all]
            [dactyl-keyboard.cad.key :as key]))

(deftest test-derivation
  (testing "single-key cluster"
    (let [prop (key/cluster-properties
                 :finger
                 (fn [& keys]
                   (get-in
                     {:key-clusters
                       {:finger
                         {:matrix-columns
                           [{}]}}}
                     keys)))]
      (is (= (:last-column prop) 0))
      (is (= (:column-range prop) [0]))
      (is (= ((:key-requested? prop) [-1 0]) false))
      (is (= ((:key-requested? prop) [0 -1]) false))
      (is (= ((:key-requested? prop) [0 0]) true))
      (is (= ((:key-requested? prop) [0 1]) false))
      (is (= ((:key-requested? prop) [1 0]) false))
      (is (= (:key-coordinates prop) '([0 0])))
      (is (= (:row-indices-by-column prop) {0 '(0)}))
      (is (= (:column-indices-by-row prop) {0 '(0)}))))
  (testing "┗┓-shaped cluster"
    (let [prop (key/cluster-properties
                 :finger
                 (fn [& keys]
                   (get-in
                     {:key-clusters
                       {:finger
                         {:matrix-columns
                           [{:rows-above-home 2}
                            {}
                            {:rows-below-home 2}]}}}
                     keys)))]
      (is (= (:last-column prop) 2))
      (is (= (:column-range prop) [0 1 2]))
      (is (= ((:key-requested? prop) [-1 0]) false))
      (is (= ((:key-requested? prop) [0 -1]) false))
      (is (= ((:key-requested? prop) [0 0]) true))
      (is (= ((:key-requested? prop) [0 1]) true))
      (is (= ((:key-requested? prop) [0 2]) true))
      (is (= ((:key-requested? prop) [0 3]) false))
      (is (= ((:key-requested? prop) [1 -1]) false))
      (is (= ((:key-requested? prop) [1 0]) true))
      (is (= ((:key-requested? prop) [1 1]) false))
      (is (= ((:key-requested? prop) [2 -3]) false))
      (is (= ((:key-requested? prop) [2 -2]) true))
      (is (= ((:key-requested? prop) [2 -1]) true))
      (is (= ((:key-requested? prop) [2 0]) true))
      (is (= ((:key-requested? prop) [2 1]) false))
      (is (= (:key-coordinates prop)
             '([0 0] [0 1] [0 2] [1 0] [2 -2] [2 -1] [2 0])))
      (is (= (:row-indices-by-column prop)
             {0 '(0 1 2), 1 '(0), 2 '(-2 -1 0)}))
      (is (= (:column-indices-by-row prop)
             {-2 '(2), -1 '(2), 0 '(0 1 2), 1 '(0), 2 '(0)})))))
