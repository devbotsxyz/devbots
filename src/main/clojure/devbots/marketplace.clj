(ns devbots.marketplace
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.proxy-headers :refer [wrap-forwarded-remote-addr]]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre :refer [info]]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.message-queue :as mq]
            [devbots.astronote :as astronote]
            [devbots.middleware :as middleware]))

(def astronote-api-key "")

;;

(defn execute-request [request]
  (let [bot-name (get-in request [:devbots :app-name]) event-name (get-in request [:github :event])]
    (info "executing marketplace request" bot-name event-name)
    (if (= "marketplace-purchase" event-name)
      (astronote/notify astronote-api-key "DevBots" (format "New install for <%s> bot." bot-name)))))

(defn process-request [request redis-connection]
  (info "processing marketplace request")
  (car/wcar redis-connection
    (mq/enqueue "marketplace-request-queue" request)))

(defn worker [{:keys [message attempt]}]
  (execute-request message)
  {:status :success})

;;

(defn handler [redis-connection]
  (fn [request]
    (process-request request redis-connection)
    "OK"))

(def marketplace-webhook-secrets ;; TODO Should move to a config file?
  {:staging    "" ;; TODO I don't think there is a staging?
   :production ""})

(defn webhook [request redis-connection]
  (-> (handler redis-connection)
      (middleware/wrap-webhook-body)
      (middleware/wrap-webhook-headers)
      (middleware/wrap-webhook-signature-checker marketplace-webhook-secrets)
      (middleware/wrap-webhook-remote-addr-checker middleware/github-webhook-addresses)
      (middleware/wrap-webhook-request-checker)
      (middleware/wrap-devbots-environment)
      (wrap-forwarded-remote-addr)
      (wrap-params)))
