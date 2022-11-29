(ns devbots.github-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :as csk]
            [cheshire.core :as cheshire]
            [devbots.github :as github]))

(def example-events
  {"pull-request/opened/by-user" "pull_request-opened.json"
   "pull-request/closed/by-user" "pull_request-closed.json"
   "pull-request/reopened/by-user" "pull_request-reopened.json"
   "pull-request/merged/by-user" "pull_request-merged.json"
   "issues/opened/by-user" "issues-opened.json"
   "issues/closed/by-user" "issues-closed.json"
   "issues/locked/by-bot" "issues-locked.json"})

(defn- load-event [name]
  (if-let [name (get example-events name)]
    (with-open [r (io/reader (io/resource (str "devbots/test/events/" name)))]
      (cheshire/parse-stream r csk/->kebab-case-keyword))))

(deftest test-authentication
  (testing "Basic Authentication"
    true))

(deftest test-event-sender-is
  (testing "Check for bot-sent event"
    (is (= true (github/event-sender-is-bot? (load-event "issues/locked/by-bot"))))
    (is (= false (github/event-sender-is-bot? (load-event "issues/opened/by-user")))))
  (testing "Check for user-sent event"
    (is (= true (github/event-sender-is-user? (load-event "issues/opened/by-user"))))
    (is (= false (github/event-sender-is-user? (load-event "issues/locked/by-bot"))))))
