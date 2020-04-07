(defproject encogio "0.1.0-SNAPSHOT"
  :url "http://encog.io"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;; config
                 [environ "1.1.0"]
                 ;; client-side scripting
                 [org.clojure/clojurescript "1.10.439"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]] ;; needed since `reitit-middleware` transitively depends on jackson-core and jackson-databind and their versions MUST match
                 ;; client-side rendering
                 [rum "0.11.4"]
                 ;; promises for async programming
                 [funcool/promesa "5.1.0"]
                 ;; clipboard management
                 [cljsjs/clipboard "2.0.4-0"]
                 ;; http server abstraction
                 [ring/ring-core "1.8.0"]
                 ;; http server runtime
                 [ring/ring-jetty-adapter "1.8.0"]
                 ;; http router & middleware
                 [metosin/reitit-ring "0.4.2"]
                 [metosin/reitit-middleware "0.4.2"]
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
             :ring-handler encogio.http/app})
