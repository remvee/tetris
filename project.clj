(defproject tetris "0.1.0-SNAPSHOT"
  :description "Yay!  Tetris!"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [reagent "0.8.1"]]

  :resource-paths ["resources"]
  :profiles {:dev {:plugins [[lein-cljsbuild "1.1.7"]
                             [lein-figwheel "0.5.19"]]
                   :dependencies [[figwheel-sidecar "0.5.19"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild {:builds {:dev {:source-paths ["src"]
                             :compiler {:output-dir "resources/public/js-dev"
                                        :output-to "resources/public/js-dev/tetris.js"
                                        :optimizations :none}}
                       :prod {:source-paths ["src"]
                              :compiler {:output-dir "resources/public/js"
                                         :output-to "resources/public/js/tetris.js"
                                         :optimizations :advanced}}}})
