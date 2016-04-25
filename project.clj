(defproject tetris "0.1.0-SNAPSHOT"
  :description "Yay!  Tetris!"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [reagent "0.5.1"]]

  :resource-paths ["resources"]
  :profiles {:dev {:plugins [[lein-cljsbuild "1.1.3"]
                             [lein-figwheel "0.5.3-2"]]
                   :dependencies [[figwheel-sidecar "0.5.3-2"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                                  :init (do (use 'figwheel-sidecar.repl-api) (start-figwheel!))}}}

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild {:builds {:dev {:source-paths ["src"]
                             :figwheel true
                             :compiler {:output-dir "resources/public/js-dev"
                                        :output-to "resources/public/js-dev/tetris.js"
                                        :optimizations :none}}
                       :prod {:source-paths ["src"]
                              :compiler {:output-dir "resources/public/js"
                                         :output-to "resources/public/js/tetris.js"
                                         :optimizations :advanced}}}})
