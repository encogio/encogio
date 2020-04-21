(defproject encogio "0.1.0-SNAPSHOT"
  :url "http://encog.io"
  :dependencies
  [[org.clojure/clojure "1.10.1"]
   ;; config
   [environ "1.1.0"]
   ;; client-side scripting
   [org.clojure/clojurescript "1.10.439"
    :exclusions [com.fasterxml.jackson.core/jackson-core]] ;; needed since `reitit-middleware` transitively depends on jackson-core and jackson-databind and their versions MUST match
   ;; html rendering
   [rum "0.12.0-SNAPSHOT"]
   ;; promises for async programming
   [funcool/promesa "5.1.0"]
   ;; clipboard management
   [cljsjs/clipboard "2.0.1-2"] ;; NOTE: 2.0.4 doesn't include `.destroy` in extern
   ;; http server abstraction
   [ring/ring-core "1.8.0"]
   ;; http server runtime
   [ring/ring-jetty-adapter "1.8.0"]
   ;; http router & middleware
   [metosin/reitit-ring "0.4.2"]
   [metosin/reitit-middleware "0.4.2"]
   ;; http api docs
   [metosin/reitit-swagger "0.4.2"]
   [metosin/reitit-swagger-ui "0.4.2"]
   [metosin/reitit-malli "0.4.2"]
   ;; redis client
   [com.taoensso/carmine "2.20.0-RC1"]
   [com.taoensso/encore "2.119.0"]
   ;; auth(z)
   [buddy/buddy-auth "2.2.0"]
   [buddy/buddy-hashers "1.4.0"]
   ;; i18n
   [com.taoensso/tempura "1.2.1"]
   ;; generative testing
   [org.clojure/test.check "1.0.0"]]
  
  :repl-options {:init-ns encogio.core}

  :uberjar-name "encogio.jar"
  :main encogio.server
  :min-lein-version "2.0.0"
  :hooks [leiningen.cljsbuild leiningen.scss]

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]]}
             :uberjar {:aot :all
                       :main encogio.server}}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-scss "0.3.0"]]

  :source-paths ["src"]
  :resource-paths ["resources" "target/cljsbuild"]

  :scss {:builds
         {:develop    {:source-dir "scss/"
                       :dest-dir   "resources/public/css/"
                       :executable "sassc"
                       :args       ["-m" "-I" "scss/" "-t" "nested"]}
          :production {:source-dir "scss/"
                       :dest-dir   "resources/public/css/"
                       :executable "sassc"
                       :args       ["-I" "scss/" "-t" "compressed"]
                       :jar        true}}}
  :cljsbuild
  {:builds
   [{:id "min"
     :jar true
     :source-paths ["src/encogio/client/"]
     :compiler {:main "encogio.client.core"
                :output-to "target/cljsbuild/public/js/app.js"
                :output-dir "target/cljsbuild/public/js"
                :optimizations :advanced
                :pretty-print false}}

    {:id "dev"
     :figwheel true
     :source-paths ["src/encogio/client/"]
     :watch-paths ["src"]
     :compiler {:main "encogio.client.core"
                :asset-path "/js/out"
                :output-to "target/cljsbuild/public/js/app.js"
                :output-dir "target/cljsbuild/public/js/out"
                :source-map true
                :optimizations :none
                :pretty-print true
                :preloads [devtools.preload]}}]}

  :figwheel {:http-server-root "public"
             :css-dirs ["resources/public/css"]
             :server-port 3449
             :nrepl-port 7002
             :ring-handler encogio.ring/app})
