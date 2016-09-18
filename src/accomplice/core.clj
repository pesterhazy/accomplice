(ns accomplice.core
  (:require [clojure.core.async :refer [chan >!! <!!
                                        go-loop <!
                                        pub sub unsub]]
            [ring.middleware.edn :as edn]
            [clojure.core.server]
            [org.httpkit.server :as server])
  (:gen-class))

(def web-port 8228)
(def telnet-port 8229)

(defn topic [v]
  :all)

(defprotocol ILog
  (append! [_ event])
  (follow [_]))

(defrecord Log [events c publication]
  ILog
  (append! [_ event]
    (swap! events conj event)
    (>!! c event))
  (follow [_]
    (let [subscriber (chan)]
      (sub publication :all subscriber)
      (try
        (loop []
          (let [v (<!! subscriber)]
            (prn v))
          (recur))
        (finally
          (unsub publication :all subscriber))))))

(defn make-log []
  (let [c (chan)
        publication (pub c topic)]
    (->Log (atom []) c publication)))

(def !log (make-log))

(defn annoy []
  (loop [n 0]
    (println "Appending..." n)
    (append! !log n)
    (println "Sleeping...")
    (Thread/sleep 1000)
    (recur (inc n))))

(defn event! [event]
  (append! !log event)
  {:status 200})

(defn handle [{:keys [uri request-method params] :as request}]
  (case [uri request-method]
    ["/event" :post] (event! params)))

(defn wrap [handler]
  (-> handler
      edn/wrap-edn-params))

(defonce !server (atom nil))

(defn start-webserver []
  (when @!server
    (@!server))
  (reset! !server (server/run-server (wrap #'handle) {:port web-port})))

(defonce _webserver (start-webserver))

(defn telnet []
  (loop []
    (when-let [line (read-line)]
      (append! !log {:line line})
      (recur))))

(defn start-telnetserver []
  (clojure.core.server/start-server {:name "telnet-json" :accept 'accomplice.core/telnet :port telnet-port })
  )

(defonce _telnetserver
  (start-telnetserver))

(defn serve []
  (start-webserver)
  (start-telnetserver))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
