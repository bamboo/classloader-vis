(defproject classloader-vis.main "0.1.0-SNAPSHOT"
  :description "The Gradle ClassLoader hierarchy visualization tool."
  :url "https://github.com/bamboo/classloader-vis"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.6.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.385"]]

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target"]

  :figwheel {:server-port 5309}

  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.4-7"]
                                  [com.cemerick/piggieback "0.2.1"]]}}

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel true
                :compiler {:main main.core
                           :output-to "target/compiled/main.js"
                           :output-dir "target/compiled/out"
                           :target :nodejs
                           :source-map-timestamp true}}
               {:id "min"
                :source-paths ["src"]
                :compiler     {:main            main.core
                               :output-to       "target/compiled/main.js"
                               :optimizations   :simple
                               :target          :nodejs
                               :closure-defines {goog.DEBUG false}
                               :pretty-print    false}}]})
