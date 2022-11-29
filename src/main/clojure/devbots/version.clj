(ns devbots.version
  (:require [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as response]))

(defn- runtime-details []
  (let [runtime (Runtime/getRuntime)]
    {:total-memory (.totalMemory runtime)
     :free-memory (.freeMemory runtime)
     :max-memory (.maxMemory runtime)}))

(defn- version-handler []
  (fn [request]
    (response/response
     {:project-version "0.1.0-SNAPSHOT"
      :git {:sha nil :tag nil}
      :clojure-version (clojure-version)
      :runtime (runtime-details)})))

(defn version [request]
  (-> (version-handler)
      (wrap-json-response)))
