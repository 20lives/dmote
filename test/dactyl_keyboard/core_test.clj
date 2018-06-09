(ns dactyl-keyboard.core-test
  (:require [clojure.test :refer :all]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.generics :as generics]))

(deftest test-soft-merge-unordered-maps
  (testing "1 deep, leaf replacement."
    (is (= (generics/soft-merge {:a 1} {:a 2}) {:a 2})))
  (testing "2 deep, leaf replacement."
    (is (= (generics/soft-merge {:a {:b 1}} {:a {:b 2}}) {:a {:b 2}})))
  (testing "2 deep, addition."
    (is (= (generics/soft-merge {:a {:b 1}} {:a {:b 2 :c 3}}) {:a {:b 2 :c 3}})))
  (testing "2 deep, conservation."
    (is (= (generics/soft-merge {:a {:b 1 :c 3}} {:a {:b 2}}) {:a {:b 2 :c 3}}))))

(deftest test-soft-merge-ordered-maps
  (let [om ordered-map]
    (testing "1 deep, leaf replacement."
      (is (= (generics/soft-merge (om :a 1) (om :a 2)) (om :a 2))))
    (testing "2 deep, leaf replacement."
      (is (= (generics/soft-merge (om :a (om :b 1)) (om :a (om :b 2)))
             (om :a (om :b 2)))))
    (testing "2 deep, addition."
      (is (= (generics/soft-merge (om :a (om :b 1)) (om :a (om :b 2 :c 3)))
             (om :a (om :b 2 :c 3)))))
    (testing "2 deep, conservation."
      (is (= (generics/soft-merge (om :a (om :b 1 :c 3)) (om :a (om :b 2)))
             (om :a (om :b 2 :c 3)))))))
