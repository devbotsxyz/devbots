(ns devbots.bots.needs_triage-test
  (:require [clojure.test :refer :all]
            [devbots.bots.needs-triage :refer [should-add-label]]))

(deftest should-add-label-test
  (testing "Do not add if status is not open"
    (is (false? (should-add-label {:enabled true} {:state "closed"})))))

(deftest settings-enabled
  (testing "Do not add if disabled"
    (is (false? (should-add-label {:enabled false :label "triage"} {:state "open"}))))
  (testing "Add if enabled"
    (is (true? (should-add-label {:enabled true :label "triage"} {:state "open"})))))

(deftest settings-skip-when-has-assignee
  (testing "Add if has no assignees"
    (is (true? (should-add-label {:enabled true :skip-when-has-assignee true} {:state "open" :assignees []})))
    (is (true? (should-add-label {:enabled true :skip-when-has-assignee true} {:state "open" :assignees nil}))))
  (testing "Do not add if has assignees"
    (is (false? (should-add-label {:enabled true :skip-when-has-assignee true} {:state "open" :assignees [{}]})))))

(deftest settings-skip-when-has-milestone
  (testing "Add if issue has no milestone"
    (is (true? (should-add-label {:enabled true :skip-when-has-milestone true} {:state "open" :milestone nil}))))
  (testing "Do not add if issue has milestone"
    (is (false? (should-add-label {:enabled true :skip-when-has-milestone true} {:state "open" :milestone {}})))))

(deftest settings-skip-when-has-labels
  (testing "Add if issue has no matching labels"
    (is (true? (should-add-label {:enabled true :skip-when-has-labels ["p1"]}
                                 {:state "open" :labels [{:name "foo"} {:name "bar"}]}))))
  (testing "Add if issue has no labels at all"
    (is (true? (should-add-label {:enabled true :skip-when-has-labels ["p1"]}
                                 {:state "open" :labels []}))))
  (testing "Do not add if issue has wildcard"
    (is (false? (should-add-label {:enabled true :skip-when-has-labels "*"}
                                  {:state "open" :labels [{:name "something"}]}))))
  (testing "Do not add if issue has specific labels"
    (is (false? (should-add-label {:enabled true :skip-when-has-labels ["p1" "p2" "p3"]}
                                  {:state "open" :labels [{:name "bug"} {:name "p2"}]})))))

(deftest settings-issue-status
  (testing "Do not add if disabled"
    (is (true? (should-add-label {:enabled true} {:state "open"}))))
  (testing "Add if enabled"
    (is (false? (should-add-label {:enabled true} {:state "closed"})))))

;; (should-add-label {:skip-when-has-labels ["p1" "p2" "p3"]}
;;                   {:state "open" :labels [{:name "bug"} {:name "p2"} {:name "task"}]})
