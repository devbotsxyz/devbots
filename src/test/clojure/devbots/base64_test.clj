(ns devbots.base64-test
  (:require [clojure.test :refer :all]
            [devbots.base64 :as base64]))

(deftest test-encoding
  (testing "It encodes strings"
    (is (= "SGVsbG8sIHdvcmxkIQ==" (base64/encode "Hello, world!"))))
  (testing "It encodes strings with unicode"
    (is (= "VGhlIER1ZGUgYWJpZGVzIC4uLiDwn5iO" (base64/encode "The Dude abides ... 😎")))))

(deftest test-decoding
  (testing "It decodes base64 to a string"
    (is (= "Hello, world!" (base64/decode "SGVsbG8sIHdvcmxkIQ=="))))
  (testing "It decodes base64 strings with a trailing newline"
    (is (= "Hello, world!" (base64/decode "SGVsbG8sIHdvcmxkIQ==\n"))))
  (testing "It decodes base64 strings split up with newlines"
    (is (= "Hello, world!" (base64/decode "SGV\nsbG\n8sIH\ndvcm\nxkIQ==\n"))))
  (testing "It decodes base64 to a string with unicode"
    (is (= "The Dude abides ... 😎" (base64/decode "VGhlIER1ZGUgYWJpZGVzIC4uLiDwn5iO")))))
