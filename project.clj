(defproject devbots "0.3.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.cache "1.0.207"]
                 ;; For the github client code
                 [clj-http "3.10.3"]
                 [cheshire "5.10.0"]
                 [camel-snake-kebab "0.4.2"]
                 ;; For the bots
                 [ring "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [compojure "1.6.2"]
                 [com.ninjakoala/cidr "1.0.6"]
                 [pandect "0.6.1"]
                 [buddy/buddy-sign "3.2.0"]
                 [clj-time "0.15.2"] ;; TODO Deprecated - replace.
                 [ring-logger "1.0.1"]
                 ;; Move devbots.yaml to its own package
                 [org.snakeyaml/snakeyaml-engine "2.2"]
                 [clojure.java-time "0.3.2"]
                 [com.taoensso/timbre "5.1.0"]
                 [com.taoensso/carmine "3.1.0"]
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.2"]
                 [environ "1.2.0"]]
  :uberjar-name "devbots.jar"
  :repl-options {:init-ns devbots.main}
  :main devbots.main
  :resource-paths ["src/main/resources"]
  :source-paths ["src/main/clojure"]
  :profiles {:dev {:resource-paths ["src/test/resources"]
                   :test-paths ["src/test/clojure"]}}
  :aot [devbots.main])
