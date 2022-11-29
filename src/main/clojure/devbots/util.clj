(ns devbots.util
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.edn :as edn]
            [devbots.base64 :as base64]
            [devbots.github :as github]
            [devbots.yaml :as yaml]))

(defn discover-bots
  "Return a map of bot-name to bot for all bots loaded in the devbots.bots.* namespace."
  []
  (->> (all-ns)
       (filter #(clojure.string/starts-with? (ns-name %) "devbots.bots."))
       (map #(ns-resolve % 'bot))
       (filter some?)
       (map #(vector (:name (var-get %)) (var-get %)))
       (into {})))

;; (let [x (first (filter #(clojure.string/starts-with? (ns-name %) "devbots.bots.") (all-ns)))]
;;   (var-get (ns-resolve x 'bot)))

(defn keywordize-keys
  "Recursively transforms all map keys from strings to lowercase kebab keywords."
  [m]
  (let [f (fn [[k v]]
            (if (string? k)
              [(-> k clojure.string/lower-case (clojure.string/replace "_" "-") keyword) v]
              [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn parse-settings [yml]
  (keywordize-keys (yaml/parse-map yml)))

(defn fetch-settings [bot auth repo]
  (when-let [response (github/get-repository-content auth repo (str ".devbots/" (:name bot) ".yml"))]
    (when-let [yml (base64/decode (:content response))]
      (merge (:default-settings bot) (parse-settings yml)))))

(defn authenticate [bot app-id event environment]
  (let [installation-id (get-in event [:installation :id])
        application-auth (github/application-authentication (github/generate-application-token bot app-id environment))
        installation-access-token (:token (github/create-installation-access-token application-auth installation-id))] ;; This is weird with both things called installation-access-token
    (github/installation-authentication installation-access-token)))

(defn load-keys [name]
  (with-open [r (io/reader (io/resource (str "devbots/bots/" name ".keys")))]
    (edn/read (java.io.PushbackReader. r))))
