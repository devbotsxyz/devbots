(ns devbots.astronote
  (:require [clj-http.client :as client]
             [cheshire.core :as cheshire]))

(def notify-endpoint "https://api.astronote.app/1/notify")

(defn notify [api-key title body]
  (let [headers {"Authorization" (str "token " api-key) "Content-Type" "application/json"}
        body    {:title title :body body}]
    (client/post notify-endpoint {:headers headers :body (cheshire/encode body)})))
