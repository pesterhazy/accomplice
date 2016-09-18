(ns accomplice.core
  (:require [clojure.core.async :refer [chan >!! <!!
                                        go-loop <!
                                        pub sub unsub]]
            [ring.middleware.edn :as edn]
            [clojure.core.server]
            [cuerdas.core :as str]
            [org.httpkit.server :as server])
  (:gen-class))

(def web-port 8228)
(def telnet-port 8229)

;; utility

(defn vec-take-last [v n]
  (assert (vector? v))
  (subvec v (max 0 (- (count v) n))))

(def width 100)

(defn prune [s] (str/prune s 100))

(defn redact [e]
  (cond-> e (:line e) (update :line prune)))

(defmulti prn-event :type)

(defmethod prn-event :default [e]
  (prn e))

(defmethod prn-event :syslog [{:keys [msg]}]
  (println (prune msg)))

(def rex #"^\S+ \S+ \S+ \S+ (\S+)\[\d*\]:? (?:<Notice>: )(.*)$")

(defn transform-syslog [{:keys [line] :as ev}]
  (if-let [[_ prog msg] (some->> line (re-matches rex))]
    (assoc ev
           :type :syslog
           :prog prog
           :msg msg)
    ev))

;; log

(defn topic [v]
  :all)

(defonce _
  (defprotocol ILog
    (append! [_ event])
    (follow [_])))

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
            (prn-event v))
          (recur))
        (finally
          (unsub publication :all subscriber))))))

(defn tail [log]
  (doseq [event (vec-take-last @(:events log) 10)]
    (prn-event event)))

(defn search [log term]
  (->> log
       :events
       deref
       (filter #(some-> % :line (clojure.string/includes? term)))
       (map prn-event)
       dorun))

(defn search-prog [log prog]
  (->> log
       :events
       deref
       (filter (comp #{prog} :prog))
       (map :msg)
       (map (comp println #(str/prune % width)))
       dorun))

(defn make-log []
  (let [c (chan)
        publication (pub c topic)]
    (->Log (atom []) c publication)))

(defn ingest! [log event]
  (->> event
       transform-syslog
       (append! log)))

(defonce !log (atom (make-log)))

(defn event! [event]
  (ingest! @!log event)
  {:status 200})

(defn handle [{:keys [uri request-method params] :as request}]
  (case [uri request-method]
    ["/event" :post] (event! params)))

(defn wrap [handler]
  (-> handler
      edn/wrap-edn-params))

(defonce !server (atom nil))

(defn restart-webserver []
  (when @!server
    (@!server))
  (reset! !server (server/run-server (wrap #'handle) {:port web-port})))

(defn telnet []
  (loop []
    (when-let [line (read-line)]
      (ingest! @!log {:line line})
      (recur))))

(def !telnetserver-running? (atom false))

(defn restart-telnetserver []
  (when @!telnetserver-running?
    (clojure.core.server/stop-server "telnet-json"))
  (clojure.core.server/start-server {:name "telnet-json" :accept 'accomplice.core/telnet :port telnet-port })
  (reset! !telnetserver-running? true))

(defn serve []
  (restart-webserver)
  (restart-telnetserver))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
