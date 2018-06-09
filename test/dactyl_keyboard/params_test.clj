(ns dactyl-keyboard.params-test
  (:require [clojure.test :refer :all]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.params :as params]))

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

(deftest test-coordinate-map-parser
  (testing "map of literal key coordinates"
    (is (= (params/map-of-key-coordinates {[1 1] 1})
           {[1 1] 1})))
  (testing "map containing relative key coordinates"
    (is (= (params/map-of-key-coordinates {["first" 1] 1})
           {[:first 1] 1})))
  (testing "map of nothing but relative key coordinates"
    (is (= (params/map-of-key-coordinates {["first" "last"] 1})
           {[:first :last] 1}))))

(deftest test-coordinate-map-validator
  (testing "map of literal key coordinates"
    (is (= (params/map-of-key-coordinate-pairs? {[1 1] 1})
           true)))
  (testing "map keyed with a valid keyword"
    (is (= (params/map-of-key-coordinate-pairs? {[1 :last] 1})
           true)))
  (testing "map keyed with an invalid keyword"
    (is (= (params/map-of-key-coordinate-pairs? {[1 :soup] 1})
           false)))
  (testing "short key"
    (is (= (params/map-of-key-coordinate-pairs? {[1] 1})
           false)))
  (testing "empty key"
    (is (= (params/map-of-key-coordinate-pairs? {[] 1})
           false))))

(deftest test-parser-defaults
  (testing "validation of configuraton parser defaults"
    (params/validate-configuration {})))
