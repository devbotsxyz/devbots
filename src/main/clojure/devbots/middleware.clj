(ns devbots.middleware
  (:require [devbots.util :as util]
            [camel-snake-kebab.core :as csk]
            [cheshire.core :as cheshire]
            [ring.util.request :as request]
            [ring.util.response :as response]
            [cidr.core :as cidr]
            [pandect.algo.sha1 :refer [sha1-hmac]]))

(def webhook-string-headers
  #{"x-github-delivery"
    "x-github-event"
    "x-github-hook-installation-target-type"})

(def webhook-number-headers
  #{"x-github-hook-id"
    "x-github-hook-installation-target-id"})

(def hub-headers
  #{"x-hub-signature"
    "x-hub-signature-256"})

(def webhook-required-headers
  (clojure.set/union webhook-string-headers webhook-number-headers hub-headers))

(defn parse-webhook-headers [headers]
  (merge
   (into {} (for [[k v] headers :let [k (clojure.string/lower-case k)] :when (webhook-string-headers k)]
              [(keyword (subs k 9)) (if (= "x-github-event" k) (clojure.string/replace v "_" "-") v)]))
   (into {} (for [[k v] headers :let [k (clojure.string/lower-case k)] :when (webhook-number-headers k)]
              [(keyword (subs k 9)) (Long/parseLong v)]))))

(defn wrap-webhook-headers [handler]
  (fn [request]
    (handler (assoc request :github (parse-webhook-headers (:headers request))))))

(defn wrap-webhook-request-checker [handler]
  (fn [request]
    (cond
      (not= :post (:request-method request))
        (-> (response/response "Method Not Allowed")
            (response/status 405))
      (not (every? #(contains? (:headers request) %) webhook-required-headers))
        (-> (response/response "Missing Required Headers")
            (response/status 400))
      :else
        (handler request))))

;; TODO Should really come from https://api.github.com/meta
(def github-webhook-addresses
  ["192.30.252.0/22",
   "185.199.108.0/22",
   "140.82.112.0/20"])

(defn wrap-webhook-remote-addr-checker [handler addresses]
  (fn [request]
    (if (some #(cidr/in-range? (:remote-addr request) %) addresses)
      (handler request)
      (-> (response/response (str "Forbidden (Invalid Remote IP: " (:remote-addr request) ")"))
          (response/status 403)))))

(defn wrap-webhook-signature-checker [handler secrets]
  (fn [request]
    (let [body-str (ring.util.request/body-string request)
          environment (get-in request [:devbots :environment])]
      (if (= (get-in request [:headers "x-hub-signature"]) (str "sha1=" (sha1-hmac body-str (get secrets environment))))
        (handler (assoc request :body body-str)) ;; (java.io.StringReader. body-str)))
        (-> (response/response "Forbidden (Incorrect Signature)")
            (response/status 403))))))

(defn wrap-devbots-environment [handler]
  (fn [request]
    (let [environment (if (= "webhook.devbots.xyz" (:server-name request)) :production :staging)
          app-name (get-in request [:query-params "app-name"])]
      (handler (assoc request :devbots {:environment environment :app-name app-name})))))

(defn wrap-webhook-body [handler]
  (fn [request]
    (let [body (ring.util.request/body-string request)
          json (cheshire/parse-string (:body request) csk/->kebab-case-keyword)]
      (handler (assoc request :body json)))))

