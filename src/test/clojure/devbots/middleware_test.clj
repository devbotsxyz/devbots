(ns devbots.middleware-test
  (:require [clojure.test :refer :all]
            [devbots.middleware :as middleware]))

(def good-webhook-headers
  {"accept" "*/*"
   "content-type" "application/json"
   "user-agent" "GitHub-Hookshot/ba59e05"
   "x-github-delivery" "783b3e80-282d-11eb-89de-b161099ccd2b"
   "x-github-event" "pull_request"
   "x-github-hook-id" "59570626"
   "x-github-hook-installation-target-id" "19770"
   "x-github-hook-installation-target-type" "integration"
   "x-hub-signature" "sha1=bf9aefb4a092fc77c55181d645e8a62f3395ddb8"
   "x-hub-signature-256" "sha256=ded6228cf6879cc626be6dbb9e98e5b4a53cb5f8f748fd0afe289f5672ffa2a0"})

(def parsed-good-webhook-headers
  {:delivery "783b3e80-282d-11eb-89de-b161099ccd2b"
   :event "pull-request"
   :hook-installation-target-type "integration"
   :hook-installation-target-id 19770
   :hook-id 59570626})

(deftest test-parse-webhook-headers
  (testing "It turns the event into kebab case"
    (is (= {:event "pull-request"} (middleware/parse-webhook-headers {"x-github-event" "pull_request"}))))
  (testing "It parses valid headers"
    (is (= parsed-good-webhook-headers
           (middleware/parse-webhook-headers good-webhook-headers)))))

(deftest test-wrap-webhook-headers
  (testing "It adds a github entry to the request"
    (is (= parsed-good-webhook-headers
           ((middleware/wrap-webhook-headers (fn [request] (:github request))) {:headers good-webhook-headers})))))

(deftest test-wrap-webhook-request-checker
  (let [handler (middleware/wrap-webhook-request-checker (fn [request] {:status 200}))]
    (testing "It returns a 405 if the method is not POST"
      (is (= 405 (:status (handler {:request-method :get})))))
    (testing "It returns a 400 if required headers are missing"
      (is (= 400 (:status (handler {:request-method :post :headers {}})))))
    (testing "It returns a 200 if everything is ok"
      (is (= 200 (:status (handler {:request-method :post :headers good-webhook-headers})))))))

(deftest test-wrap-webhook-remote-addr-checker
  (let [handler (middleware/wrap-webhook-remote-addr-checker (fn [request] {:status 200}) middleware/github-webhook-addresses)]
    (testing "It returns a 403 if the remote address is not in the allow list"
      (is (= 403 (:status (handler {:remote-addr "1.2.3.4"})))))
    (testing "It returns a 200 if the remote address is in the allow list"
      (is (= 200 (:status (handler {:remote-addr "140.82.112.123"})))))))

(deftest test-wrap-webhook-signature-checker
  (let [handler (middleware/wrap-webhook-signature-checker (fn [request] {:status 200}) {:production "s3cr3t" :staging "s3cr3t"})]
    (testing "It returns a 403 if the signature header does not match"
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" "sha1=43cceafd1d731605d990ea32c9ecc9a0a53d45c6"} :devbots {:environment :staging}})))))
    (testing "It returns a 403 if the signature header is missing"
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {} :devbots {:environment :staging}})))))
    (testing "It returns a 403 of the signature header is malformed"
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" "cheese=43cceafd1d731605d990ea32c9ecc9a0a53d45c6"} :devbots {:environment :staging}}))))
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" "sha1=cheese"} :devbots {:environment :staging}}))))
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" "fooblah"} :devbots {:environment :staging}}))))
      (is (= 403 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" ""} :devbots {:environment :staging}})))))
    (testing "It returns a 200 if the signature matches"
      (is (= 200 (:status (handler {:body "Hello, world!" :headers {"x-hub-signature" "sha1=c2529413944b5045021647d581d3dff8465b9cf7"} :devbots {:environment :staging}})))))))

(deftest test-wrap-devbots-environment
  (let [handler (middleware/wrap-devbots-environment (fn [request] (:devbots request)))]
    (testing "It sets the environment to staging"
      (is (= {:environment :staging, :app-name "hello-bot"} (handler {:server-name "devbots.sateh.com" :query-params {"app-name" "hello-bot"}}))))
    (testing "It sets the environment to production"
      (is (= {:environment :production, :app-name "hello-bot"} (handler {:server-name "webhook.devbots.xyz" :query-params {"app-name" "hello-bot"}}))))))

(deftest test-wrap-webhook-body
  (let [handler (middleware/wrap-webhook-body (fn [request] (:body request)))]
    (testing "It parses a JSON body into a map with kebab style keywords as keys"
      (is (= {:foo-bar 42 :baz true} (handler {:body "{\"foo_bar\": 42, \"baz\": true}"}))))))
