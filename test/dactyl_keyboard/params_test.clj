(ns dactyl-keyboard.params-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
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

(deftest test-coordinates-validator
  (testing "empty"
    (is (= (spec/valid? ::params/key-coordinates [])
           false)))
  (testing "short"
    (is (= (spec/valid? ::params/key-coordinates [1])
           false)))
  (testing "literal key coordinates"
    (is (= (spec/valid? ::params/key-coordinates [1 1])
           true)))
  (testing "a mapping"
    (is (= (spec/valid? ::params/key-coordinates {1 1})
           false)))
  (testing "a valid keyword"
    (is (= (spec/valid? ::params/key-coordinates [1 :last])
           true)))
  (testing "an invalid keyword"
    (is (= (spec/valid? ::params/key-coordinates [1 :soup])
           false))))

(deftest test-parser-defaults
  (testing "validation of configuraton parser defaults"
    (params/validate-configuration {})))
