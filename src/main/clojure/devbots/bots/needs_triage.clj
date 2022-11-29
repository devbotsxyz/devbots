(ns devbots.bots.needs-triage
  (:require [clojure.java.io :as io]
            [devbots.events :as events]
            [devbots.github :as github]
            [devbots.macros :as macros]))

(macros/defbot "needs-triage"
  :action-filter #{"opened"}
  :default-settings {:label "needs-triage"
                     :skip-when-has-labels nil
                     :skip-when-has-any-labels false
                     :skip-when-has-assignee false
                     :skip-when-has-milestone false})

(defn- check-enabled [settings issue]
  (not (:enabled settings)))

(defn- check-state [settings issue]
  (not= "open" (:state issue)))

(defn- check-labels [settings issue]
  (when-let [labels (:skip-when-has-labels settings)]
    (if (or (= "*" labels) (= ["*"] labels))
      (not (empty? (:labels issue)))
      (some #(contains? (set labels) (:name %)) (:labels issue)))))

(defn- check-assignees [settings issue]
  (and (:skip-when-has-assignee settings)
       (not (empty? (:assignees issue)))))

(defn- check-milestone [settings issue]
  (and (:skip-when-has-milestone settings)
       (some? (:milestone issue))))

(defn should-add-label [settings issue]
  (not-any? #(% settings issue)
    [#'check-enabled #'check-state #'check-assignees #'check-milestone #'check-labels]))

(defmethod events/handle-issues "needs-triage" [bot auth settings event]
  (let [repo-name (get-in event [:repository :full-name]) issue-number (get-in event [:issue :number])]
    (when-let [issue (github/get-issue auth repo-name issue-number)]
      (if (should-add-label settings issue)
        (github/add-labels-to-issue auth repo-name issue-number [(:label settings)])))))
