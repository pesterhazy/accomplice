(ns accomplice.core
  (:require [clojure.core.async :refer [chan >!! <!!
                                        go-loop <!
                                        pub sub unsub]])
  (:gen-class))

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
          (println "Cleaning up...")
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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
