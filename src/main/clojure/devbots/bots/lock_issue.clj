(ns devbots.bots.lock-issue
  (:require [devbots.events :as events]
            [devbots.github :as github]
            [devbots.macros :as macros]))

(macros/defbot "lock-issue" :action-filter #{"closed"} :default-settings {:comment nil})

(defmethod events/handle-issues "lock-issue" [bot auth settings event]
  (let [repo (get-in event [:repository :full-name]) issue-number (get-in event [:issue :number])]
    (when-let [comment (:comment settings)]
      (github/create-issue-comment auth repo issue-number comment))
    (github/lock-issue auth repo issue-number "resolved")))
