(ns devbots.ping
  (:require [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as response]
            [taoensso.carmine :as car :refer (wcar)]))

(defn- ping-handler [redis-connection]
  "Endpoint for API status checks. Pings Redis too."
  (fn [request]
    (response/response {:ok true
                        :redis-response (car/wcar redis-connection (car/ping))})))

(defn ping [request redis-connection]
  (-> (ping-handler redis-connection)
      (wrap-json-response)))
