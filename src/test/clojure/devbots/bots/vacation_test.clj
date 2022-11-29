(ns devbots.bots.vacation-test
  (:require [clojure.test :refer :all]
            [devbots.bots.vacation :refer [parse-period date-in-period? get-comment]]
            [java-time :as jt]))

(deftest test-parse-period
  (testing "Parses the period"
    (let [period {:start (jt/local-date "2020-11-21") :end (jt/local-date "2020-11-29")}]
      (is (= period (parse-period "2020-11-21, 2020-11-29")))
      (is (= period (parse-period "2020-11-21 / 2020-11-29")))
      (is (= period (parse-period "2020-11-21   2020-11-29")))
      (is (= period (parse-period "  2020-11-21 ,  2020-11-29")))
      (is (= period (parse-period "  2020-11-21 ,  2020-11-29  ")))
      (is (= period (parse-period "2020-11-21   2020-11-29"))))))

(deftest test-date-in-period
  (let [period {:start (jt/local-date "2020-11-21") :end (jt/local-date "2020-11-29")}]
    (testing "Matches date in period, inclusive start/end"
      (is (true? (date-in-period? (jt/local-date "2020-11-21") period)))
      (is (true? (date-in-period? (jt/local-date "2020-11-25") period)))
      (is (true? (date-in-period? (jt/local-date "2020-11-29") period))))
    (testing "Does not match date before period"
      (is (false? (date-in-period? (jt/local-date "2020-11-20") period))))
    (testing "Does not match date after period"
      (is (false? (date-in-period? (jt/local-date "2020-11-30") period))))))

(deftest test-get-comment
  (testing "Returns nil if no comments at all"
    (is (nil? (get-comment {} :issue)))
    (is (nil? (get-comment {} :pull-request))))
  (testing "Returns comment if no specific comment"
    (is (= "comment" (get-comment {:comment "comment"} :issue)))
    (is (= "comment" (get-comment {:comment "comment"} :pull-request))))
  (testing "Returns specific comment if only specific comment"
    (is (= "issue comment" (get-comment {:issue-comment "issue comment"} :issue)))
    (is (= "pull comment" (get-comment {:pull-comment "pull comment"} :pull-request))))
  (testing "Returns specific comment if both comment"
    (is (= "issue comment" (get-comment {:comment "comment" :issue-comment "issue comment"} :issue)))
    (is (= "pull comment" (get-comment {:comment "comment" :pull-comment "pull comment"} :pull-request)))))
