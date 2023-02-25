(ns johnswanson.mock-server-test
  (:require [clojure.test :refer :all]
            [johnswanson.mock-server :as mock :refer [with-expect with-mock-server]]
            [clj-http.client :as http]))

(deftest mock-server-works-as-expected
  (with-mock-server
    (testing "we can run a mock server"
      (with-expect {:request {:url "/foo"
                              :method "POST"}
                    :response {:status 200}}
        (is (= 200 (:status (http/post (mock/url "/foo")))))))
    (testing "i can expect a certain number of requests"
      (with-expect {:request {:url "/foo"
                              :method "POST"}
                    :response {:status 200
                               :count 2}}
        (is (= 200 (:status (http/post (mock/url "/foo")))))
        (is (= 200 (:status (http/post (mock/url "/foo")))))))
    (testing "if i don't send all the expected requests, i get an error"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Response at index '1' not triggered \(\{:status 200\}\)"
        (with-expect {:request {:url "/foo"
                                :method "POST"}
                      :response {:status 200
                                 :count 2}}
          (is (= 200 (:status (http/post (mock/url "/foo")))))))))
    (testing "if i send too many requests, I get an error"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"More requests than responses"
        (with-expect {:request {:url "/foo"
                                :method "POST"}
                      :response {:status 200}}
          (is (= 200 (:status (http/post (mock/url "/foo")))))
          (is (= 404 (:status (http/post (mock/url "/foo")
                                         {:throw-exceptions false}))))))))
  (testing "I can set more complicated requests"
    (let [good-request
          {:method "POST"
           :url (mock/url "/foo")
           :throw-exceptions false
           :headers {"secret-token" "foobar"
                     "content-type" "application/json"}
           :body "{\"someValues\": \"go\", \"here_they_are\": \"here\"}"}
          expectation {:request {:url "/foo"
                                 :headers {"secret-token" {:matches "foobar"}
                                           "content-type" {:matches "application/json"}}
                                 :body-patterns [{:equal-to-json
                                                  {"someValues" "go"
                                                   "here_they_are" "here"}}]}
                       :response {:status 200}}]
      (testing "succeeds if it matches!"
        (with-expect expectation
          (is (= 200
                 (:status (http/request good-request))))))
      (testing "fails if it doesn't match"
        (testing "wrong header"
          (is (thrown-with-msg?
               clojure.lang.ExceptionInfo
               #"Response at index '0' not triggered \(\{:status 200\}\)"
               (with-expect expectation
                 (is (= 404 (-> good-request
                                (assoc-in [:headers "secret-token"] "WRONG")
                                http/request
                                :status)))))))
        (testing "wrong body"
          (is (thrown-with-msg?
               clojure.lang.ExceptionInfo
               #"Response at index '0' not triggered \(\{:status 200\}\)"
               (with-expect expectation
                 (is (= 404 (-> good-request
                                (assoc :body "oops")
                                http/request
                                :status))))))))))
    (testing "raw body equality"
      (with-expect {:request {:url "/foo"
                              :body-patterns [{:equal-to "foo"}]}
                    :response {:status 200}}
        (is (= 200 (:status (http/post (mock/url "/foo")
                                       {:body "foo"}))))))
    (testing "contains-based matching"
      (with-expect {:request {:url "/foo"
                              :body-patterns [{:contains "foobar"}]}
                    :response {:status 200}}
        (is (= 200 (:status (http/post (mock/url "/foo")
                                       {:body "hello world nice to foobar meet you!"}))))))
    (testing "headers can contain values as well"
      (with-expect {:request {:url "/foo"
                              :headers {"foo-header" {:contains "foo-value"}}}
                    :response {:status 200}}
        (is (= 200 (:status (http/post (mock/url "/foo")
                                       {:headers {"foo-header" "some-foo-value-goes-here"}}))))))))
