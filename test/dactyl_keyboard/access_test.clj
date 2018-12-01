(ns dactyl-keyboard.base-test
  (:require [clojure.test :refer :all]
            [dactyl-keyboard.param.access :as access]))

(deftest test-parser-defaults
  (testing "validation of configuraton parser defaults"
    (access/validate-configuration {})))
