(defproject logan "0.1.0-SNAPSHOT"
  :description "Distributed analysis of IRC logs"
  :url "https://github.com/technomancy/logan"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories {"releases" "file:///tmp/repo"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [cheshire "4.0.0"]
                 [ringmon "0.1.2"]
                 [die-roboter "1.0.0"]])
