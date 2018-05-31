(ns dactyl-keyboard.core-test
  (:require [clojure.test :refer :all]
            [dactyl-keyboard.generics :as generics]))

(deftest test-soft-merge
  (testing "1 deep, leaf replacement."
    (is (= (generics/soft-merge {:a 1} {:a 2}) {:a 2})))
  (testing "2 deep, leaf replacement."
    (is (= (generics/soft-merge {:a {:b 1}} {:a {:b 2}}) {:a {:b 2}})))
  (testing "2 deep, addition."
    (is (= (generics/soft-merge {:a {:b 1}} {:a {:b 2 :c 3}}) {:a {:b 2 :c 3}})))
  (testing "2 deep, conservation."
    (is (= (generics/soft-merge {:a {:b 1 :c 3}} {:a {:b 2}}) {:a {:b 2 :c 3}}))))
