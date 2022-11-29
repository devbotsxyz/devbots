(ns devbots.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.logger :as logger]
            [compojure.core :refer :all]
            [compojure.route :as route]
            ;;[devbots.webhook :as webhook]
            ;;[devbots.dispatcher :as dispatcher]
            [devbots.application :as application]
            [devbots.marketplace :as marketplace]
            [devbots.version :as version]
            [devbots.ping :as ping]
            [devbots.env :as env]
            [integrant.core :as ig]
            [taoensso.carmine.message-queue :as mq]
            [taoensso.timbre :as timbre :refer [info]])
  (:gen-class))

;;

(defn app-routes [{:keys [redis-connection] :as opts}]
  (println "OPTS" opts)
  (compojure.core/routes
    (GET "/ping" request
      (ping/ping request (:redis-connection opts)))
    (GET "/version" request
      (version/version request))
    (POST "/v1/application-webhook" request
      (application/webhook request (:redis-connection opts)))
    (POST "/v1/marketplace-webhook" request
      (marketplace/webhook request (:redis-connection opts)))
    (route/not-found
      "<h1>Route not found</h1>")))

(defmethod ig/init-key :handler/app-routes [_ {:keys [redis-connection] :as opts}]
  (println "STARTING :handler/app-routes")
  (app-routes opts))

;;

(defmethod ig/init-key :worker/application [_ {:keys [redis-connection] :as opts}]
  (println "STARTING :worker/application")
  (mq/worker redis-connection "application-request-queue" {:handler application/worker}))

;;

(defmethod ig/init-key :worker/marketplace [_ {:keys [redis-connection] :as opts}]
  (println "STARTING :worker/marketplace")
  (mq/worker redis-connection "marketplace-request-queue" {:handler marketplace/worker}))

;;

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler] :as opts}]
  (jetty/run-jetty handler (-> opts (dissoc :handler) (assoc :join? false))))

;;

(defn start-devbots [overrides]
  (let [jetty-port (or (env/number :jetty-port) (get overrides :jetty-port) 8080)
        redis-uri  (or (env/string :redis-uri) (get overrides :redis-uri) "redis://127.0.0.1:6379")]
    (let [config {:adapter/jetty        {:port jetty-port, :handler (ig/ref :handler/app-routes)}
                  :handler/app-routes   {:redis-connection {:pool {} :spec {:uri redis-uri}}}
                  :worker/application   {:redis-connection {:pool {} :spec {:uri redis-uri}}}
                  :worker/marketplace   {:redis-connection {:pool {} :spec {:uri redis-uri}}}}
          system (ig/init config)]
      (info "The system has been started"))))

(defn stop-devbots []
  nil)

;;

(defn -main []
  (start-devbots {}))
