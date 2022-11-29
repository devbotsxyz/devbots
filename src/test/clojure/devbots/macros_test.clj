(ns devbots.macros-test
  (:require [clojure.test :refer :all]
            [devbots.macros :as macros]))

(deftest test-defbot
  (testing "It generates the correct code"
    (is (= '(clojure.core/intern
             clojure.core/*ns*
             (clojure.core/symbol "bot")
             {:default-settings {:delay 1, :label "triage", :enabled false},
              :action-filter nil,
              :name "foo-bar",
              :keys (devbots.util/load-keys "foo-bar")})
           (macroexpand `(macros/defbot "foo-bar" :default-settings {:label "triage" :delay 1}))))))
