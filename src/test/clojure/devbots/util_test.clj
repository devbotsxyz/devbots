(ns devbots.util-test
  (:require [clojure.test :refer :all]
            [devbots.util :as util]))

(deftest test-keywordize-keys
  (testing "Turn keys into keywords"
    (is (= {:foo 42 :bar true :baz "lol"} (util/keywordize-keys {"foo" 42, "bar" true, "baz" "lol"}))))
  (testing "Turn underscores into hyphens"
    (is (= {:created-at 1234} (util/keywordize-keys {"created_at" 1234})))))

(deftest test-parse-settings
  (testing "It uses keywords as keys"
    (is (= [:enabled :delay] (keys (util/parse-settings "enabled: true\ndelay: 3\n")))))
  (testing "It returns keys with snake case"
    (is (= [:foo-bar :one-two-three] (keys (util/parse-settings "foo_bar: true\none_two_three: 3\n")))))
  (testing "It converts booleans to native types"
    (is (= true (:enabled (util/parse-settings "enabled: true\ndelay: 3\n"))))
    (is (= false (:enabled (util/parse-settings "enabled: false\ndelay: 3\n")))))
  (testing "It converts a list of strings"
    (is (= ["One" "Two" "Three"] (:labels (util/parse-settings "enabled: true\nlabels: [One, Two, Three]\ndelay: 3\n"))))))
