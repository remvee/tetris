(defproject tetris "0.1.0-SNAPSHOT"
  :description "Yay!  Tetris!"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [reagent "0.5.0"]
                 [figwheel "0.3.7"]]

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.7"]]

  :figwheel {:nrepl-port 7888
             :css-dirs ["resources/public/css"]}

  :cljsbuild {:builds {:dev {:source-paths ["dev" "src"]
                             :compiler {:preamble ["reagent/react.js"]
                                        :output-dir "resources/public/js-dev"
                                        :output-to "resources/public/js-dev/tetris.js"
                                        :source-map "resources/public/js-dev/tetris.js.map"
                                        :optimizations :none}}
                       :prod {:source-paths ["src"]
                              :compiler {:output-dir "resources/public/js"
                                         :output-to "resources/public/js/tetris.js"
                                         :optimizations :advanced}}}})
