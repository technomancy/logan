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
  ;; workaround for https://github.com/mefesto/wabbitmq/pull/7
  (bot/with-robots {:virtual-host (if-let [uri (System/getenv "RABBITMQ_URL")]
                                    (->> (.getPath (java.net.URI. uri))
                                         (re-find #"/?(.*)") (second)))}
    (->> (days-for channel)
         (map #(bot/send-back `(reduce-day nick-freqs ~channel ~%)))
         (doall)
         (map deref)
         (apply merge-with +))))

(defonce responses (atom {}))

(defn queue-count [channel]
  (future (swap! responses conj channel (count-channel channel))))

(defn app [req]
  (let [channel (subs (:uri req) 1)]
    (if-let [response (@responses channel)]
      {:status 200 :headers {"Content-Type" "application/json"}
       :body (json/encode response)}
      (do (queue-count channel)
          {:status 202 {:headers {"Content-Type" "text/plain"}}
           :body "Please wait..."}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT")))]
    (jetty/run-jetty app {:port port})))
