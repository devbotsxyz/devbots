(ns devbots.bots.needs-review
  (:require [clojure.java.io :as io]
            [devbots.events :as events]
            [devbots.github :as github]
            [devbots.macros :as macros]))

(macros/defbot "needs-review"
  :action-filter #{"opened" "ready_for_review"}
  :default-settings {:label "needs-triage"
                     :skip-when-has-assignee false
                     :skip-when-has-reviewer false})

(defn- check-enabled [settings issue]
  (not (:enabled settings)))

(defn- check-state [settings pr]
  (not= "open" (:state pr)))

(defn- check-assignee [settings pr]
  (and (:skip-when-has-assignee settings)
       (not (empty? (:assignees pr)))))

(defn- check-reviewer [settings pr]
  (and (:skip-when-has-reviewer settings)
       (or (not (empty? (:requested-reviewers pr)))
           (not (empty? (:requested-teams pr))))))

(defn should-add-label [settings pr]
  (not-any? #(% settings pr)
            [#'check-enabled #'check-state #'check-assignee #'check-reviewer]))

(defmethod events/handle-pull-request "needs-review" [bot auth settings event]
  (if-not (get-in event [:pull-request :draft] false)
    (let [repo-name (get-in event [:repository :full-name]) pull-number (get-in event [:pull-request :number])]
      (when-let [pull (github/get-pull-request auth repo-name pull-number)]
        (if (should-add-label settings pull)
          (github/add-labels-to-issue auth repo-name pull-number [(:label settings)]))))))
