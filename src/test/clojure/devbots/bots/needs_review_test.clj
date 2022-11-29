(ns devbots.bots.needs_review-test
  (:require [clojure.test :refer :all]
            [devbots.bots.needs-review :refer [should-add-label]]))

(deftest should-add-label-test
  (testing "Do not add if status is not open"
    (is (false? (should-add-label {:enabled true :label "review"}
                                  {:state "closed"})))))

(deftest settings-enabled
  (testing "Do not add if disabled"
    (is (false? (should-add-label {:enabled false :label "review"}
                                  {:state "open"}))))
  (testing "Add if enabled"
    (is (true? (should-add-label {:enabled true :label "review"}
                                 {:state "open"})))))

(deftest test-skip-when-has-assignee
  (testing "Do not add if has assignee"
    (is (false? (should-add-label {:enabled true :label "review" :skip-when-has-assignee true}
                                  {:state "open" :assignees [{}]})))))

(deftest test-skip-when-has-reviewer
  (testing "Do not add if pr has requested reviewers"
    (is (false? (should-add-label {:enabled true :label "review" :skip-when-has-reviewer true}
                                  {:state "open" :requested-reviewers [{}]}))))
  (testing "Do not add if pr has requested teams"
    (is (false? (should-add-label {:enabled true :label "review" :skip-when-has-reviewer true}
                                  {:state "open" :requested-teams [{}]})))))
