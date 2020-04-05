(defproject encogio "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; client-side scripting
                 [org.clojure/clojurescript "1.10.439"]
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
  :main encogio.app
  :min-lein-version "2.0.0"

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]]}
             :uberjar {:aot :all :main encogio.app}}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]]

  :source-paths ["src"]
  :resource-paths ["resources" "target/cljsbuild"]

  :cljsbuild
  {:builds
   [{:id "app"
     :figwheel true
     :source-paths ["src"]
     :watch-paths ["src"]
     :compiler {:main "encogio.client.core"
                :asset-path "/js/out"
                :output-to "target/cljsbuild/public/js/app.js"
                :output-dir "target/cljsbuild/public/js/out"
                :source-map true
                :optimizations :none
                :pretty-print true
                :preloads [devtools.preload]                
                :aot-cache true}}
    {:id "min"
     :source-paths ["src"]
     :compiler {:output-to "target/cljsbuild/public/js/app.js"
                :output-dir "target/cljsbuild/public/js"
                :source-map "target/cljsbuild/public/js/app.js.map"
                :optimizations :advanced
                :pretty-print false
                :aot-cache true}}]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :nrepl-port 7002
             ;; Server index.html for all routes for HTML5 routing
             :ring-handler encogio.http/home})
