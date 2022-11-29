(ns devbots.application
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.proxy-headers :refer [wrap-forwarded-remote-addr]]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre :refer [info]]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.message-queue :as mq]
            [clojure.core.cache.wrapped :as cache]
            [devbots.bots.vacation :as vacation]
            [devbots.bots.lock-issue :as lock-issue]
            [devbots.bots.lock-pull-request :as lock-pull-request]
            [devbots.bots.needs-triage :as needs-triage]
            [devbots.bots.needs-review :as needs-review]
            [devbots.events :as events]
            [devbots.middleware :as middleware]
            [devbots.util :as util]))

;;

(def installation-access-token-cache
  (cache/ttl-cache-factory {} :ttl (* 55 60 1000)))

(defn cached-authenticate [bot app-id event environment]
  (let [cache-key [(:name bot) (get-in event [:installation :id])]]
    (cache/lookup-or-miss
     installation-access-token-cache cache-key
     (fn [_] (util/authenticate bot app-id event environment)))))

;;

(def bots (util/discover-bots)) ;; TODO Get rid of this global?

(defn resolve-handler [event-name]
  (resolve (symbol (str "devbots.events/handle-" event-name))))

(defn handles-action? [bot event]
  (let [filter (:action-filter bot) action (:action event)]
    (or (nil? action)
        (nil? filter)
        (filter action))))

;;

(defn execute-request [request]
  (when-let [bot (get bots (get-in request [:devbots :app-name]))]
    (let [event (:body request) repo (get-in event [:repository :full-name])]
      (info "executing application request" repo (:name bot) (get-in request [:github :event]) (get-in request [:body :action]))
      (when (handles-action? bot event)
        (let [app-id (get-in request [:github :hook-installation-target-id])
              auth (cached-authenticate bot app-id event (get-in request [:devbots :environment]))]
          (if-let [settings (util/fetch-settings bot auth repo)]
            (if (:enabled settings)
              (when-let [event-handler (resolve-handler (get-in request [:github :event]))]
                (info "executing application request" repo (:name bot) (get-in request [:github :event]) (get-in request [:body :action]))
                (let [start-time (System/currentTimeMillis)]
                  (event-handler bot auth settings (:body request))
                  (info "finished application request" repo (:name bot) (get-in request [:github :event]) (get-in request [:body :action]) (- (System/currentTimeMillis) start-time)))))))))))

(defn process-request [request redis-connection]
  (when-let [bot (get bots (get-in request [:devbots :app-name]))]
    (let [event (:body request) repo (get-in event [:repository :full-name])]
      (info "processing application request" repo (:name bot) (get-in request [:github :event]) (get-in request [:body :action]))
      (when (handles-action? bot event)
        (let [app-id (get-in request [:github :hook-installation-target-id])
              auth (cached-authenticate bot app-id event (get-in request [:devbots :environment]))]
          (if-let [settings (util/fetch-settings bot auth repo)]
            (if (:enabled settings)
              (when-let [event-handler (resolve-handler (get-in request [:github :event]))]
                (info "queueing application request" repo (:name bot) (get-in request [:github :event]) (get-in request [:body :action]))
                (car/wcar redis-connection
                  (mq/enqueue "application-request-queue" request {:initial-backoff-ms (* 60 1000 (:delay settings))}))))))))))

(defn worker [{:keys [message attempt]}]
  (execute-request message)
  {:status :success})

;;

(defn handler [redis-connection]
  (fn [request]
    (process-request request redis-connection)
    "OK"))

(def application-webhook-secrets
  {:staging    ""
   :production ""})

(defn webhook [request redis-connection]
  (-> (handler redis-connection)
      (middleware/wrap-webhook-body)
      (middleware/wrap-webhook-headers)
      (middleware/wrap-webhook-signature-checker application-webhook-secrets)
      (middleware/wrap-webhook-remote-addr-checker middleware/github-webhook-addresses)
      (middleware/wrap-webhook-request-checker)
      (middleware/wrap-devbots-environment)
      (wrap-forwarded-remote-addr)
      (wrap-params)))
