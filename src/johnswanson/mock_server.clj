(ns johnswanson.mock-server
  (:require [org.httpkit.server]))

(def requests (atom (atom [])))

(def handler-atom (atom nil))

(defn- handler [req]
  (let [req (update req :body #(some-> % slurp))]
    (swap! @requests conj req)
    (if-let [handler-fn @handler-atom]
      (handler-fn req)
      {:status 200})))

(defn start-server! []
  (org.httpkit.server/run-server handler {:port 0 :legacy-return-value? false}))

(defn stop-server! [s]
  @(org.httpkit.server/server-stop! s {:timeout 0}))

(defn url
  "Returns the URL for the mock server"
  [s]
  (format "http://localhost:%s" (org.httpkit.server/server-port s)))

(defn with-mock-server-fn [server f]
  (try
    (f)
    (finally
      (stop-server! server))))

(defmacro with-mock-server [server & forms]
  `(let [~server (start-server!)]
     (with-mock-server-fn ~server (fn [] ~@forms))))

(defmacro with-requests [requests & forms]
  `(let [~requests (atom [])]
     (reset! requests ~requests)
     (try
       (do ~@forms)
       (finally
         (reset! requests (atom []))))))

(defmacro with-handler [handler & forms]
  `(try
     (reset! handler-atom ~handler)
     (do ~@forms)
     (finally
       (reset! handler-atom nil))))
