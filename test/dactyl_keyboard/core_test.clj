(ns dactyl-keyboard.core-test
  (:require [clojure.test :refer :all]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.params :as params]
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

(deftest test-endpoint?
  (testing "empty"
    (is (= (params/endpoint? {}) true)))
  (testing "default only"
    (is (= (params/endpoint? {:default 1}) true)))
  (testing "non-reserved keyword"
    (is (= (params/endpoint? {:a 1}) false)))
  (testing "nested"
    (is (= (params/endpoint? {:k {:default 1}}) false))))

(deftest test-validate-leaf
  (testing "simple"
    (is (= (params/validate-leaf {:parse-fn int} 2) 2)))
  (testing "default where nil"
    (is (= (params/validate-leaf {:default 3} nil) 3)))
  (testing "parsing as keyword"
    (is (= (params/validate-leaf {:parse-fn keyword} "s") :s)))
  (testing "validation, negative for error"
    (is (= (params/validate-leaf {:validate [(partial = 1)]} 1) 1)))
  (testing "validation, positive for error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
          (params/validate-leaf {:validate [(partial = 1)]} 2)))))

(deftest test-validate-node
  (testing "simple"
    (is (= (params/validate-node
             {:k {:default 2}}
             {:k 1}
             :k)
           {:k 1})))
  (testing "default value in absence of candidate"
    (is (= (params/validate-node
             {:k {:default 2}}
             {}
             :k)
           {:k 2}))))

(deftest test-validate-branch
  (testing "nested asymmetric master with empty input"
    (is (= (params/validate-branch
             {:k0 {:k0a {:default 1}
                   :k0b {:default 2}}
              :k1 {:default 3}}
             {})
           {:k0 {:k0a 1
                 :k0b 2}
            :k1 3})))
  (testing "nested and ordered"
    (let [om ordered-map]
      (is (= (params/validate-branch
               (om :k0 (om :k0a (om :default 1)
                           :k0b (om :default 2)))
               (om :k0 (om :k0b 3)))
             (om :k0 (om :k0a 1
                         :k0b 3)))))))
