(ns devbots.bots.vacation
  (:require [devbots.events :as events]
            [devbots.github :as github]
            [devbots.macros :as macros]
            [java-time :as jt]))

(defn date-in-period? [date period]
  (not (or (jt/before? date (:start period))
           (jt/after? date (:end period)))))

(defn parse-period [s]
  (if-let [matches (re-matches #"\s*(\d\d\d\d-\d\d-\d\d)[\s,/-]+(\d\d\d\d-\d\d-\d\d)\s*" s)]
    {:start (jt/local-date (nth matches 1)) :end (jt/local-date (nth matches 2))}))

(defn get-comment [settings issue-type]
  (condp = issue-type
    :issue        (or (:issue-comment settings) (:comment settings))
    :pull-request (or (:pull-comment settings) (:comment settings))))

(defn should-post-comment [bot auth settings event]
  (not (github/event-sender-is-bot? event)))

(defn post-vacation-comment [bot auth settings event issue-type]
  (when (should-post-comment bot auth settings event)
    (when-let [period (parse-period (:period settings))]
      (when (date-in-period? (jt/local-date) period)
        (let [repo (get-in event [:repository :full-name]) issue-number (get-in event [issue-type :number])]
          (when-let [comment (get-comment settings issue-type)]
            (github/create-issue-comment auth repo issue-number comment)))))))

(macros/defbot "vacation" :action-filter #{"opened"}
  :default-settings {:comment nil, :pull-comment nil, :issue-comment nil, :period nil})

(defmethod events/handle-issues "vacation" [bot auth settings event]
  (post-vacation-comment bot auth settings event :issue))

(defmethod events/handle-pull-request "vacation" [bot auth settings event]
  (post-vacation-comment bot auth settings event :pull-request))
