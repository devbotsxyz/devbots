(ns devbots.bots.lock-pull-request
  (:require [clojure.java.io :as io]
            [devbots.events :as events]
            [devbots.github :as github]
            [devbots.macros :as macros]))

(macros/defbot "lock-pull-request" :action-filter #{"closed"} :default-settings {:comment nil})

(defmethod events/handle-pull-request "lock-pull-request" [bot auth settings event]
  (let [repo (get-in event [:repository :full-name]) pull-number (get-in event [:pull-request :number])]
    (when-let [comment (:comment settings)]
      (github/create-issue-comment auth repo pull-number comment))
    (github/lock-issue auth repo pull-number "resolved")))
