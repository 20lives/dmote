(ns dactyl-keyboard.base-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [flatland.ordered.map :refer [ordered-map]]
            [dactyl-keyboard.param.base :as base]
            [dactyl-keyboard.param.schema :as schema]))

(deftest test-parse-leaf
  (testing "simple"
    (is (= (base/parse-leaf {:parse-fn int} 2) 2)))
  (testing "default where nil"
    (is (= (base/parse-leaf {:default 3} nil) 3)))
  (testing "parsing as keyword"
    (is (= (base/parse-leaf {:parse-fn keyword} "s") :s))))

(deftest test-validate-leaf
  (testing "validation, negative for error"
    (is (= (base/validate-leaf {:validate [(partial = 1)]} 1) [nil])))
  (testing "validation, positive for error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
          (base/validate-leaf {:validate [(partial = 1)]} 2)))))

(deftest test-parse-branch
  (testing "type error"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Non-mapping section"
          (base/parse-branch base/inclusive-or {:k {:default 2}} [:k 1])))))

(deftest test-parse-node
  (testing "simple"
    (is (= (base/parse-node base/inclusive-or
             {:k {:default 2}}
             {:k 1}
             :k)
           {:k 1})))
  (testing "default value in absence of candidate"
    (is (= (base/parse-node base/inclusive-or
             {:k {:default 2}}
             {}
             :k)
           {:k 2}))))

(deftest test-consume-branch
  (testing "nested asymmetric master with empty input"
    (is (= (base/consume-branch
             {:k0 {:k0a {:default 1}
                   :k0b {:default 2}}
              :k1 {:default 3}}
             {})
           {:k0 {:k0a 1
                 :k0b 2}
            :k1 3})))
  (testing "nested and ordered"
    (let [om ordered-map]
      (is (= (base/consume-branch
               (om :k0 (om :k0a (om :default 1)
                           :k0b (om :default 2)))
               (om :k0 (om :k0b 3)))
             (om :k0 (om :k0a 1
                         :k0b 3)))))))

(deftest test-closures
  (let [om ordered-map
        raws
          [["Configuration."]
           [:section [:a]
            "A."]
           [:parameter [:a :b]
            {:default 1}
            "B."]]
        inflated (base/inflate raws)]
    (testing "inflation"
      (is (= inflated
             (om :a (om :metadata {:help "A."}
                        :b {:default 1, :help "B."})))))
    (testing "parsing with defaults and no input"
      (is (= ((base/parser-with-defaults raws)
              {})
             {:a {:b 1}})))
    (testing "parsing without defaults and no input"
      (is (= ((base/parser-wo-defaults raws)
              {})
             {})))))

(deftest test-direct-delegation
  (let [om ordered-map
        sub-raws
          [["Subordinate configuration."]
           [:section [:b]
            "Subordinate A."]
           [:parameter [:b :p]
            {:default 0
             :validate [number?]}
            "Subordinate P."]]
        sub-defaults (base/extract-defaults sub-raws)
        sub-parser (base/parser-with-defaults sub-raws)
        sub-validator (base/delegated-validation sub-raws)
        sup-raws
          [["Superordinate configuration."]
           [:parameter [:x]
            {:default sub-defaults
             :parse-fn sub-parser
             :validate [sub-validator]}
            "Superordinate P."]]
        inflated (base/inflate sup-raws)]
    (testing "inflation of nested structure"
      (is (= inflated
             (om :x {:default {:b {:p 0}},
                     :parse-fn sub-parser,
                     :validate [sub-validator]
                     :help "Superordinate P."}))))
    (testing "parsing nested structure with defaults and no input"
      (is (= ((base/parser-with-defaults sup-raws)
              {})
             {:x {:b {:p 0}}})))
    (testing "parsing nested structure without defaults and no input"
      (is (= ((base/parser-wo-defaults sup-raws) {})
             {})))
    (testing "parsing nested structure with valid input"
      (is (= ((base/parser-wo-defaults sup-raws) {:x {:b {:p 1}}})
             {:x {:b {:p 1}}})))
    (testing "parsing subordinate structure with invalid input"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
            (base/consume-branch (base/inflate sub-raws) {:b {:p "s"}}))))
    (testing "parsing nested structure with invalid input"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
            (base/consume-branch inflated {:x {:b {:p "s"}}}))))))

(deftest test-collection-delegation
  (let [om ordered-map
        sub-raws
          [["Subordinate configuration."]
           [:section [:s]
            "Section."]
           [:parameter [:s :p0]
            {:default -1
             :validate [number?]}
            "Parameter."]
           [:parameter [:s :p1]
            {:default [:v]
             :validate [(spec/coll-of keyword?)]}
            "Parameter."]]
        sub-parser (schema/tuple-of (base/parser-with-defaults sub-raws))
        alt-parser (schema/tuple-of (base/parser-wo-defaults sub-raws))
        sub-validator (spec/coll-of (base/delegated-validation sub-raws))
        sup-raws
          [["Superordinate configuration."]
           [:parameter [:x]
            {:default []
             :parse-fn sub-parser
             :validate [sub-validator]}
            "X."]
           [:parameter [:y]
            {:default []
             :parse-fn alt-parser
             :validate [sub-validator]}
            "Y."]]
        inflated (base/inflate sup-raws)
        consume (partial base/consume-branch inflated)]
    (testing "inflation of nested list structure"
      (is (= inflated
             (om :x {:default [],
                     :parse-fn sub-parser
                     :validate [sub-validator]
                     :help "X."}
                 :y {:default [],
                     :parse-fn alt-parser
                     :validate [sub-validator]
                     :help "Y."}))))
    (testing "parsing nested list item with simple parameter"
      (is (= (consume {:x [{:s {:p0 1}}]})
             {:x [{:s {:p0 1, :p1 [:v]}}]
              :y []}))
      (is (= (consume {:x [{:s {:p0 1}}]
                       :y [{:s {:p0 1}}]})
             {:x [{:s {:p0 1, :p1 [:v]}}]
              :y [{:s {:p0 1}}]})))
    (testing "parsing nested list item with complex parameter"
      (is (= (consume {:x [{:s {:p1 [:a :b]}}]})
             {:x [{:s {:p0 -1, :p1 [:a :b]}}]
              :y []})))
    (testing "parsing nested list item with empty section"
      (is (= (consume {:x [{:s {}}]})
             {:x [{:s {:p0 -1, :p1 [:v]}}]
              :y []})))
    (testing "parsing nested list item with empty list item as input"
      (is (= (consume {:x [{}]})
             {:x [{:s {:p0 -1, :p1 [:v]}}]
              :y []})))
    (testing "parsing nested list item with empty list as input"
      (is (= (consume {:y []})
             {:x []
              :y []})))
    (testing "parsing nested list item without input"
      (is (= (consume {})
             {:x []
              :y []})))
    (testing "parsing nested list item with invalid input"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
            (consume {:x [{:s {:p0 "s"}}]})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Value out of range"
            (consume {:x [{:s {:p1 1}}]}))))))
