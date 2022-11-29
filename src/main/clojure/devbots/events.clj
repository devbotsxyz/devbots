(ns devbots.events)

(defmulti handle-pull-request (fn [bot auth settings event] (:name bot)))
(defmethod handle-pull-request :default [bot auth settings event])

(defmulti handle-issues (fn [bot auth settings event] (:name bot)))
(defmethod handle-issues :default [bot auth settings event])
