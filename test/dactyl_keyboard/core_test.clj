(ns dactyl-keyboard.core-test
  (:require [clojure.test :refer :all]
            [dactyl-keyboard.generics :as generics]))

(deftest test-chainget
  (testing "1 deep."
    (is (= (generics/chain-get {:a 1} :a) 1)))
  (testing "2 deep, fetching a map."
    (is (= (generics/chain-get {:a {:b 3}} :a) {:b 3})))
  (testing "2 deep, fetching a value."
    (is (= (generics/chain-get {:a {:b 3}} :a :b) 3))))

(deftest test-soft-merge-maps
  (testing "1 deep, leaf replacement."
    (is (= (generics/soft-merge-maps {:a 1} {:a 2}) {:a 2})))
  (testing "2 deep, leaf replacement."
    (is (= (generics/soft-merge-maps {:a {:b 1}} {:a {:b 2}}) {:a {:b 2}})))
  (testing "2 deep, addition."
    (is (= (generics/soft-merge-maps {:a {:b 1}} {:a {:b 2 :c 3}}) {:a {:b 2 :c 3}})))
  (testing "2 deep, conservation."
    (is (= (generics/soft-merge-maps {:a {:b 1 :c 3}} {:a {:b 2}}) {:a {:b 2 :c 3}}))))
