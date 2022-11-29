(ns devbots.yaml
  (:import [org.snakeyaml.engine.v2.api LoadSettings Load]))

(def ^:private load-settings (.build (LoadSettings/builder)))

(def ^:private yaml-load (Load. load-settings))

(defn parse-map [s]
  (into {} (.loadFromString yaml-load s)))

(parse-map "z: 1\na: 2\nh: [1,2,3]")
