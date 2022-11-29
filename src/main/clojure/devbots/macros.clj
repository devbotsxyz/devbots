(ns devbots.macros
  (:require [devbots.util :as util]))

(def ^{:private true} common-settings
  {:delay 0})

;; TODO Can we completely get rid of the id here and do (defbot
;; lock-issue ...) ? Then we do not have to deal with staging vs
;; production. We can simply rely on the bot-name parameter in the
;; webhook and make sure that matches.
;;
;; For the key, we should just include both staging and production
;; in the project and name them production-key & staging-key. And
;; pick the right one based on the incoming request. Probably using
;; the domain name or another request parameter like env=staging.
;;
;; That will simplify things a lot!

;; TODO The action filter works for simple bots that just deal with
;; one webhook but breaks down for bots that handle multiple
;; events. The filter could take a combination of event & action? Or
;; the filter should be on the event handler?

(defmacro defbot [name & {:keys [default-settings action-filter]}] ;; TODO Do we need this? :or {default-settings nil}}]
  (let [settings (merge common-settings default-settings {:enabled false})]
    `(intern *ns* (symbol "bot")
      {:name ~name
       :keys (devbots.util/load-keys ~name)
       :default-settings ~settings
       :action-filter ~action-filter})))
