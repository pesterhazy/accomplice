(ns accomplice.core
  (:require [clojure.core.async :refer [chan >!! <!! go-loop <!]])
  (:gen-class))

(defprotocol ILog
  (append! [_ event])
  (follow [_]))

(defrecord Log [events c]
  ILog
  (append! [_ event]
    (swap! events conj event)
    (>!! c event))
  (follow [_]
    (loop []
      (let [x (<!! c)]
        (prn x))
      (recur))))

(defn make-log []
  (->Log (atom []) (chan 10)))

(def !log (make-log))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
