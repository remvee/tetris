(ns user.figwheel
  (:require [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(defn ^:export wrap [f]
  (figwheel/watch-and-reload
   :websocket-url "ws://localhost:3449/figwheel-ws"
   :jsload-callback f)
  (f))
