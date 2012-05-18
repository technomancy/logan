(ns logan.logs
  (:require [clojure.java.io :as io]
            [die.roboter :as bot]))

(defn get-log-stream [channel day]
  (-> (format "http://raynes.me/logs/irc.freenode.net/%s/%s.txt" channel day)
      (java.net.URL.)
      (.openStream)))

(defn parse-line [line]
  (re-find #"\[([:\d]+?)\] \*?(\S+?):? (.*)" line))

(defn count-day [f channel day]
  (with-open [r (io/reader (get-log-stream channel day))]
    (reduce f {} (map parse-line (line-seq r)))))

(defn days-for [channel]
  (->> (slurp (str "http://raynes.me/logs/irc.freenode.net/" channel))
       (re-seq #"href=\"([-\d]+)\.txt\"")
       (map second)))

(defn count-channel [f channel]
  (apply merge-with +
         (for [day (take 100 (days-for channel))]
           (count-day f channel day))))

(defn nick-freqs [freqs [line time nick message]]
  (update-in freqs [nick] (fnil inc 0)))

(def nick-freqs-for-channel (partial count-channel nick-freqs))

(defn -main [channel]
  (println (format "Participants in #%s by lines sent:" channel))
  (doseq [[nick count] (sort-by val (nick-freqs-for-channel channel))]
    (println nick "-" count)))
