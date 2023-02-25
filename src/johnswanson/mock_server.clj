(ns johnswanson.mock-server
  (:require [jsonista.core :as json]
            [clj-http.client :as http]
            [camel-snake-kebab.core :refer [->kebab-case ->camelCase]])
  (:import (java.util UUID)
           (com.github.tomakehurst.wiremock WireMockServer)
           (com.github.tomakehurst.wiremock.core WireMockConfiguration)))

(def mapper
  (json/object-mapper
   {:encode-key-fn #(if (keyword? %)
                      (name (->camelCase %))
                      %)
    :decode-key-fn (comp ->kebab-case keyword)}))


(def ^:dynamic *server* nil)

(defn start-server! []
  (doto (WireMockServer. (.port (WireMockConfiguration/options) 0))
    (.start)))

(defn with-mock-server-fn [f]
  (let [server (start-server!)]
    (binding [*server* server]
      (try
        (f)
        (finally
          (.stop ^WireMockServer server))))))

(defmacro with-mock-server [& forms]
  `(with-mock-server-fn
     (fn []
       (do ~@forms))))

(defn stub!
  [body]
  (http/post (.url ^WireMockServer *server* "/__admin/mappings/new")
             {:body (json/write-value-as-string body mapper)}))

(defn url
  ([] (url ""))
  ([u] (.url ^WireMockServer *server* u)))

(defn requests
  []
  (:requests
   (json/read-value
    (:body (http/get (.url ^WireMockServer *server* "/__admin/requests")))
    mapper)))

(defn- reset-server!
  []
  (http/post (.url ^WireMockServer *server* "/__admin/reset") {}))

(defn ->mappings [{:keys [request response responses]}]
  (let [responses (or responses [response])
        responses (reduce
                   (fn [accu {:keys [count] :or {count 1} :as response}]
                     (apply conj accu (repeat count (dissoc response :count))))
                   []
                   responses)
        scenario (UUID/randomUUID)
        states (conj (repeatedly (count responses) #(str (UUID/randomUUID)))
                     "Started")
        ids (map-indexed (fn [i _] i) responses)
        id->response (zipmap ids responses)
        state->id (zipmap states ids)
        mappings
        (->> (partition 2 1 states)
             (map (fn [response [current-state next-state]]
                    {:scenario-name scenario
                     :new-scenario-state next-state
                     :required-scenario-state current-state
                     :request request
                     :response response})
                  responses))]
    {:scenario scenario
     :expected-state (last states)
     :id->response id->response
     :state->id state->id
     :mappings (conj mappings {:response {:status 404}
                               :request request
                               :scenario-name scenario
                               :required-scenario-state (last states)
                               :new-scenario-state "UNHANDLED"})}))

(defn error-message-for-unexpected-state
  "The state doesn't match what we expected for multiple responses. We
  expect the state to be the last possible state, but instead it's "
  [{:keys [id->response state->id] :as context} actual-state]
  (if (= actual-state "UNHANDLED")
    "More requests than responses"
    (let [id (state->id actual-state)
          response (id->response id)]
      (format "Response at index '%s' not triggered (%s)"
              id
              (pr-str response)))))

(defn expect-fn
  [m f]
  (reset-server!)
  (let [{:keys [mappings expected-state scenario] :as context}
        (->mappings m)]
    (doseq [mapping mappings]
      (stub! mapping))
    (let [result (f)
          actual-state (some->> (http/get (.url *server* "/__admin/scenarios"))
                                :body
                                ((fn [v] (json/read-value v mapper)))
                                :scenarios
                                (filter (fn [s]
                                          (= (:id s) (str scenario))))
                                first
                                :state)]
      (when-not (= (str expected-state) actual-state)
        (throw (ex-info (error-message-for-unexpected-state context actual-state)
                        (assoc context :actual-state actual-state))))
      result)))

(defmacro with-expect [m & forms]
  `(expect-fn ~m (fn [] (do ~@forms))))
