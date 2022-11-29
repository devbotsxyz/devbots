(ns devbots.github
  (:require [devbots.base64 :as base64]
            [clj-http.client :as client]
            [camel-snake-kebab.core :as csk]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :as keys]
            [clj-time.core :as time]))

;; Authentication tokens & headers

(def api-base "https://api.github.com")

(defn basic-authentication [username token]
  {:type :basic-authentication :username username :token token})

(defn personal-access-token [token]
  {:type :personal-access-token :token token})

(defn oauth-token [token]
  {:type :oauth-token :token token})

(defn application-authentication [token]
  {:type :application-token :token token})

(defn installation-authentication [token]
  {:type :installation-token :token token})

(defmulti authentication-headers :type)

(defmethod authentication-headers :basic-authentication [auth]
  {:Authorization (str "Basic " (base64/encode (str (:username auth) ":" (:token auth))))})

(defmethod authentication-headers :personal-access-token [auth]
  {:Authorization (str "token " (:token auth))})

(defmethod authentication-headers :oauth-token [auth]
  {:Authorization (str "token " (:token auth))})

(defmethod authentication-headers :application-token [auth]
  {"Authorization" (str "Bearer " (:token auth))})

(defmethod authentication-headers :installation-token [auth]
  {"Authorization" (str "Bearer " (:token auth))})

;; Repository full name parsing

(defmulti parse-repo-name class)

(defmethod parse-repo-name String [name]
  (let [[owner repo] (str/split name #"/")]
    {:owner owner :repo repo}))

(defmethod parse-repo-name clojure.lang.PersistentVector [name]
  (let [[owner repo] name]
    {:owner owner :repo repo}))

(defmethod parse-repo-name clojure.lang.PersistentList [name]
  (let [[owner repo] name]
    {:owner owner :repo repo}))

(defmethod parse-repo-name clojure.lang.PersistentArrayMap [name]
  (select-keys name [:owner :repo]))

;; API Requests

;; application/vnd.github.groot-preview+json
;; application/vnd.github.v3+json

(defn headers [auth preview]
  (let [version (if preview (str preview "-preview") "v3")
        accept  (str "application/vnd.github." version "+json")]
    (merge {:Accept accept} (authentication-headers auth))))

(defn parse-path [path]
  (map #(if (str/starts-with? % ":") (keyword (subs % 1)) %)
       (drop 1 (str/split path #"/"))))

(defn build-path [path-components parameters]
  (str/join "/" (map #(if (keyword? %) (% parameters) %) path-components)))

(defn build-url [api-base path-components path-parameters]
  (str api-base "/" (build-path path-components path-parameters)))

(defn list-request [auth path-components path-parameters & {:keys [preview]}]
  (let [url (build-url api-base path-components path-parameters)]
    (cheshire/parse-string (:body (client/get url {:headers (headers auth preview)}))
                           csk/->kebab-case-keyword)))

(def unexceptional-statuses
  #{200 201 202 203 204 205 206 207 300 301 302 303 304 307 404})

(defn get-request [auth path-components path-parameters & {:keys [preview]}]
  (let [url (build-url api-base path-components path-parameters)]
    (let [r (client/get url {:headers (headers auth preview) :debug false
                             :unexceptional-status unexceptional-statuses})]
      (if (not= 404 (:status r))
        (cheshire/parse-string (:body r) csk/->kebab-case-keyword)))))

(defn post-request [auth path-components path-parameters body & {:keys [preview]}]
  (let [url (build-url api-base path-components path-parameters)]
    (if (nil? body)
      (cheshire/parse-string (:body (client/post url {:headers (headers auth preview)
                                                      :debug false}))
                             csk/->kebab-case-keyword)
      (cheshire/parse-string (:body (client/post url {:headers (headers auth preview)
                                                      :body (cheshire/encode body) :debug false}))
                             csk/->kebab-case-keyword))))

(defn put-request [auth path-components path-parameters body & {:keys [preview]}]
  (let [url (build-url api-base path-components path-parameters)]
    (if (nil? body)
      (cheshire/parse-string (:body (client/put url {:headers (headers auth preview)
                                                     :debug false}))
                             csk/->kebab-case-keyword)
      (cheshire/parse-string (:body (client/put url {:headers (headers auth preview)
                                                     :body (cheshire/encode body)
                                                     :debug false}))
                             csk/->kebab-case-keyword))))

(defn delete-request [auth path-components path-parameters & {:keys [preview]}]
  (let [url (build-url api-base path-components path-parameters)]
    (cheshire/parse-string (:body (client/delete url {:headers (headers auth preview)
                                                      :debug false}))
                           csk/->kebab-case-keyword)))

;; Pull Requests

(defn get-pull-request [auth repo pull-number]
  (get-request auth (parse-path "/repos/:owner/:repo/pulls/:pull-number")
               (merge {:pull-number pull-number} (parse-repo-name repo))))

;; Issues

(defn get-issue [auth repo issue-number]
  (get-request auth (parse-path "/repos/:owner/:repo/issues/:issue-number")
               (merge {:issue-number issue-number} (parse-repo-name repo))))

(defn add-labels-to-issue [auth repo issue-number labels]
  (post-request auth (parse-path "/repos/:owner/:repo/issues/:issue-number/labels")
                (merge {:issue-number issue-number} (parse-repo-name repo))
                {:labels labels}))

(defn lock-issue [auth repo issue-number lock-reason]
  (put-request auth (parse-path "/repos/:owner/:repo/issues/:issue-number/lock")
               (merge {:issue-number issue-number} (parse-repo-name repo))
               {:lock_reason lock-reason}))

(defn create-issue-comment [auth repo issue-number body]
  (post-request auth (parse-path "/repos/:owner/:repo/issues/:issue-number/comments")
                (merge {:issue-number issue-number} (parse-repo-name repo))
                {:body body}))

;; Applications

(defn get-app [auth app-slug]
  (get-request auth (parse-path "/app/:app-slug")
               {:app-slug app-slug}))

(defn get-authenticated-app [auth]
  (get-request auth (parse-path "/app") {}))

(defn list-app-installations [auth]
  (list-request auth (parse-path "/app/installations") {}))

(defn get-app-installation [auth installation-id]
  (get-request auth (parse-path "/app/installations/:installation-id")
               {:installation-id installation-id}))

(defn suspend-app-installation [auth installation-id]
  (put-request auth (parse-path "/app/installations/:installation-id/suspended")
               {:installation-id installation-id}))

(defn unsuspend-app-installation [auth installation-id]
  (delete-request auth (parse-path "/app/installations/:installation-id/suspended")
               {:installation-id installation-id}))

(defn delete-installation [auth installation-id]
  (delete-request auth (parse-path "/app/installations/:installation-id")
               {:installation-id installation-id}))

(defn create-installation-access-token [auth installation-id]
  (post-request auth (parse-path "/app/installations/:installation-id/access_tokens")
               {:installation-id installation-id} nil))

;; Organizations

(defn get-organization-membership [auth org username]
  (get-request auth (parse-path "/orgs/:org/members/:username")
               (merge {:org org :username username})))

;; Repositories

(defn get-repository-readme [auth repo path]
  (get-request auth (parse-path "/repos/:owner/:repo/readme")
               (parse-repo-name repo)))

(defn get-repository-content
  "Get repository content. Returns nil if not found."
  [auth repo path]
  (get-request auth (parse-path "/repos/:owner/:repo/contents/:path")
               (merge {:path path} (parse-repo-name repo))))

(defn list-releases [auth repo]
  (list-request auth (parse-path "/repos/:owner/:repo/releases")
                (parse-repo-name repo)))

(defn list-commit-statuses [auth repo ref]
  (list-request auth (parse-path "/repos/:owner/:repo/commits/:ref/statuses")
                (merge {:ref ref} (parse-repo-name repo))))

(defn get-branch [auth repo branch]
  (get-request auth (parse-path "/repos/:owner/:repo/branches/:branch")
               (merge {:branch branch} (parse-repo-name repo))))

(defn list-repository-contributors [auth repo]
  (list-request auth (parse-path "/repos/:owner/:repo/contributors")
                (parse-repo-name repo)))

;; Authentication

(defn application-token-claims [bot app-id]
  (let [now (time/now)]
    {:iss app-id
     :iat now
     :exp (time/plus now (time/seconds 300))
     :id  (str (java.util.UUID/randomUUID))}))

(defn generate-application-token [bot app-id environment]
  (let [private-key (keys/str->private-key (get-in bot [:keys environment]))]
    (jwt/sign (application-token-claims bot app-id) private-key {:alg :rs256})))

;; Git Commits

(defn get-commit [auth repo commit-sha]
  (get-request auth (parse-path "/repos/:owner/:repo/git/commits/:commit-sha")
               (merge {:commit-sha commit-sha} (parse-repo-name repo))))

(defn list-pulls-associated-with-commit [auth repo commit-sha]
  (get-request auth (parse-path "/repos/:owner/:repo/commits/:commit-sha/pulls")
               (merge {:commit-sha commit-sha} (parse-repo-name repo)) :preview "groot"))

;; Users

(defn get-authenticated-user [auth]
  (get-request auth (parse-path "/user")))

(defn get-user [auth username]
  (get-request auth (parse-path "/users/:username")))

(defn get-user-hovercard [auth username]
  (get-request auth (parse-path "/users/:username/hovercard")))

;;

(defn event-sender-is-bot? [event]
  (= "Bot" (get-in event [:sender :type])))

(defn event-sender-is-user? [event]
  (= "User" (get-in event [:sender :type])))
