(ns dactyl-keyboard.base-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.param.base :as base]))

(deftest test-validate-leaf
  (testing "simple"
    (is (= (base/validate-leaf {:parse-fn int} 2) 2)))
  (testing "default where nil"
    (is (= (base/validate-leaf {:default 3} nil) 3)))
  (testing "parsing as keyword"
    (is (= (base/validate-leaf {:parse-fn keyword} "s") :s)))
  (testing "validation, negative for error"
    (is (= (base/validate-leaf {:validate [(partial = 1)]} 1) 1)))
  (testing "validation, positive for error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
          (base/validate-leaf {:validate [(partial = 1)]} 2)))))

(deftest test-validate-node
  (testing "simple"
    (is (= (base/validate-node
             {:k {:default 2}}
             {:k 1}
             :k)
           {:k 1})))
  (testing "default value in absence of candidate"
    (is (= (base/validate-node
             {:k {:default 2}}
             {}
             :k)
           {:k 2}))))

(deftest test-validate-branch
  (testing "nested asymmetric master with empty input"
    (is (= (base/validate-branch
             {:k0 {:k0a {:default 1}
                   :k0b {:default 2}}
              :k1 {:default 3}}
             {})
           {:k0 {:k0a 1
                 :k0b 2}
            :k1 3})))
  (testing "nested and ordered"
    (let [om ordered-map]
      (is (= (base/validate-branch
               (om :k0 (om :k0a (om :default 1)
                           :k0b (om :default 2)))
               (om :k0 (om :k0b 3)))
             (om :k0 (om :k0a 1
                         :k0b 3)))))))
