(ns devbots.env
  (:require [environ.core :refer [env]]))

(defn string [name]
  (env name))

(defn number [name]
  (if-let [e (env name)]
    (Long/parseLong e)))
