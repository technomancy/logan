(ns logan.core
  (:require [clojure.java.io :as io]
            [die.roboter :as bot]
            [cheshire.core :as json]
            [ring.adapter.jetty :as jetty]))

(defn get-log-stream [channel day]
  (-> (format "http://raynes.me/logs/irc.freenode.net/%s/%s.txt" channel day)
      (java.net.URL.)
      (.openStream)))

(defn parse-line [line]
  (re-find #"\[([:\d]+?)\] \*?(\S+?):? (.*)" line))

(defn reduce-day [f channel day]
  (with-open [r (io/reader (get-log-stream channel day))]
    (reduce f {} (map parse-line (line-seq r)))))

(defn days-for [channel]
  (->> (slurp (str "http://raynes.me/logs/irc.freenode.net/" channel))
       (re-seq #"href=\"([-\d]+)\.txt\"")
       (map second)))

(defn nick-freqs [freqs [line time nick message]]
  (update-in freqs [nick] (fnil inc 0)))

(defn count-channel [channel]
  (->> (days-for channel)
       (map #(bot/send-back `(reduce-day nick-freqs ~channel ~%)))
       (doall)
       (map deref)
       (apply merge-with +)))

(defn app [req]
  (json/encode (count-channel (subs (:uri req) 1))))

(defn -main [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT")))]
    (jetty/run-jetty app {:port port})))
