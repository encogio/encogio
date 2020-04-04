(defproject encogio "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; http server
                 [ring/ring-core "1.8.0"]
                 [ring/ring-jetty-adapter "1.8.0"]
                 ;; http router
                 [metosin/reitit-ring "0.4.2"]                 
                 ;; redis client
                 [com.taoensso/carmine "2.20.0-RC1"]
                 ;; generative testing
                 [org.clojure/test.check "1.0.0"]]
  :repl-options {:init-ns encogio.core}
  :uberjar-name "encogio.jar"
  :profiles {:uberjar {:aot :all :main encogio.app}})
