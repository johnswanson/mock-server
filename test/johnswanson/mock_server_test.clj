(ns johnswanson.mock-server-test
  (:require [clojure.test :refer [deftest testing is]]
            [johnswanson.mock-server :as mock :refer [with-mock-server with-requests with-handler]]
            [org.httpkit.client :as http]))

(deftest mock-server-works-as-expected
  (with-mock-server server
    (with-requests requests
      (testing "returns 200 by default"
        (is (= 200 (:status @(http/post (str (mock/url server) "/foo"))))))
      (testing "we can count requests"
        (is (= 1 (count @requests))))
      (testing "or make more complex assertions"
        (is (= "/foo" (:uri (first @requests))))))
    (with-requests requests
      (is (= 200 (:status @(http/post (mock/url server)
                                      {:body "{\"foo\": \"bar\"}"}))))
      (testing "we can make assertions about the request body"
        (is (= "{\"foo\": \"bar\"}"
               (:body (first @requests))))))
    (testing "we can respond differently"
      (with-handler (constantly {:status 500})
        (is (= 500 (:status @(http/post (mock/url server)))))))
    (testing "server does not wait for request processing to complete"
      (with-handler (fn [_req]
                      (try
                        (Thread/sleep 60000)
                        (catch java.lang.InterruptedException _
                          nil))
                      {:status 200})
        (is (= "idle timeout: 10ms"
               (.getMessage (:error @(http/post (mock/url server)
                                                {:timeout 10})))))))))
