(ns tetris.core
  (:require [goog.events :as events]
            [goog.events.KeyCodes :as key-codes]
            [goog.events.EventType :as event-type]
            [goog.storage.mechanism.HTML5LocalStorage :as HTML5LocalStorage]
            [reagent.core :as r]))


(defonce initial-tick-frequency 500)
(defonce min-tick-frequency 50)

(defonce width 10)
(defonce height 20)

(defonce empty-grid
  (vec (take height (repeat (vec (take width (repeat \ )))))))

(defonce empty-world
  {:grid  empty-grid
   :speed 0
   :score 0})

(defonce blocks [["IIII"]
                 ["JJJ"
                  "  J"]
                 ["LLL"
                  "L  "]
                 ["OO"
                  "OO"]
                 [" SS"
                  "SS "]
                 ["TTT"
                  " T "]
                 ["ZZ "
                  " ZZ"]])

(defn new-world []
  (assoc empty-world
         :next-block (rand-nth blocks)))

(defonce local-storage (goog.storage.mechanism.HTML5LocalStorage.))

(defonce high-score-atom
  (r/atom (if-let [v (.get local-storage "high-score")]
            (js/parseInt v)
            500)))

(defonce high-score-watcher
  (add-watch high-score-atom :local-storage
             (fn [_ _ _ score]
               (.set local-storage "high-score" (str score)))))

(defn rotate [block rotations]
  (reduce (fn [block _]
            (apply map (fn [& args] (vec (reverse args))) block))
          block
          (range (mod rotations 4))))

(defn valid? [{:keys [grid block x y rotations]}]
  (let [block (rotate block rotations)]
    (and (>= x 0)
         (<= (+ x (count (first block))) width)
         (>= y 0)
         (<= (+ y (count block)) height)
         (every? (fn [[grid-line block-line]]
                   (every? (fn [[grid-cell block-cell]]
                             (or (= grid-cell \ ) (= block-cell \ )))
                           (map vector
                                (drop x grid-line)
                                block-line)))
                 (map vector
                      (drop y grid)
                      block)))))

(defn render [{:keys [grid block x y rotations] :as world :or {y 0}}]
  (let [block (rotate block rotations)]
    (concat (take y grid)
            (map (fn [grid-line block-line]
                   (concat (take x grid-line)
                           (map (fn [grid-cell block-cell]
                                  (if (= block-cell \ )
                                    grid-cell
                                    block-cell))
                                (drop x grid-line)
                                block-line)
                           (drop (+ x (count block-line)) grid-line)))
                 (drop y grid)
                 block)
            (drop (+ y (count block)) grid))))

(defn score [{:keys [grid] :as world}]
  (let [grid   (render world)
        marked (->> grid
                    (map vector (iterate inc 0))
                    (reduce (fn [marked [i grid-line]]
                              (if (every? (partial not= \ ) grid-line)
                                (conj marked i)
                                marked))
                            #{}))]
    (-> world
        (assoc :grid grid
               :marked marked)
        (update :speed + (if (empty? marked) 0 10)))))

(defn clear [{:keys [grid] :as world}]
  (-> world
      (assoc :grid (reduce (fn [grid grid-line]
                             (if (every? (partial not= \ ) grid-line)
                               (into [(take width (repeat \ ))] grid)
                               (conj grid grid-line)))
                           []
                           grid))
      (dissoc :marked)))

(defn new-block [{:keys [marked score next-block] :as world}]
  (let [block next-block
        x     (int (- (/ width 2) (/ (count (first block)) 2)))
        grid  (-> world render)
        score (+ score (* 100 (count marked) (/ (count marked) 2)))
        new   (assoc world
                     :grid grid
                     :x x, :y 0, :rotations 0 :block block
                     :next-block (rand-nth blocks), :score score)]
    (if (valid? new)
      new
      (assoc new :game-over? true))))

(defn move [{:keys [start? game-over? marked] :as world} direction]
  (let [new (case direction
              :up    (update world :rotations inc)
              :down  (update world :y inc)
              :left  (update world :x dec)
              :right (update world :x inc)
              world)]
    (cond
      (or start? game-over?) world
      marked                 (clear world)
      (valid? new)           new
      (= direction :down)    (-> world score new-block)
      :else                  world)))

(defn grid-component [{:keys [marked] :as world}]
  (let [grid (render world)]
    [:div.grid
     (for [[y line] (map vector (iterate inc 0) grid)]
       [:div.line
        {:key   (str "line-" y)
         :class (when (get marked y) "marked")}
        (for [[x cell] (map vector (iterate inc 0) line)]
          [:div.cell
           {:key   (str "cell-" x)
            :class (when-not (= cell \ )
                     (str "taken block-" cell))}])])]))

(defn next-block-component [{:keys [next-block]}]
  [:div.next-block
   (for [[y line] (map vector (iterate inc 0) next-block)]
     [:div.line
      {:key   (str "line-" y)}
      (for [[x cell] (map vector (iterate inc 0) line)]
        [:div.cell
         {:key   (str "cell-" x)
          :class (when-not (= cell \ )
                   (str "taken block-" cell))}])])])

(defn move! [world-atom direction]
  (swap! world-atom move direction)
  (let [high-score @high-score-atom
        score      (:score @world-atom)]
    (when (> score high-score)
      (reset! high-score-atom (max @high-score-atom
                                   (:score @world-atom))))))

(defn main-component [world-atom]
  (let [high-score    @high-score-atom
        {:keys [start? game-over? grid score]
         :as   world} @world-atom
        running?      (not (or start? game-over?))
        button        (fn [dir]
                        (let [f #(do (move! world-atom dir)
                                     (.preventDefault %))]
                          [:button {:class    (name dir)
                                    :on-click f}
                           (name dir)]))]
    [:div.main {:class (when-not running? "paused")}
     [:div.game {:class (when (>= score high-score) "have-high-score")}

      [:div.score.high-score
       [:span.label "High Score:"] " " [:span.amount high-score]]
      (grid-component world)
      (when running? (next-block-component world))
      [:div.score.current-score
       [:span.label "Score:"] " " [:span.amount score]]

      [:div.controls
       (button :left)
       (button :right)
       (button :up)
       (button :down)]]

     (when-not running?
       [:a.title
        {:href     "#"
         :on-click #(reset! world-atom (new-block (new-world)))}
        (if start?
          "START GAME"
          "GAME OVER")])]))

(defonce world-atom (r/atom (assoc (new-world) :start? true)))
(defonce paused-atom (atom false)) ;; note: normal atom, not reagent
(defn toggle-pause! []
  (swap! paused-atom not))

(defn tick! []
  (when-not @paused-atom
    (move! world-atom :down))
  (let [tick-frequency (max (- initial-tick-frequency
                               (-> world-atom deref :speed))
                            min-tick-frequency)]
    (js/setTimeout tick! tick-frequency)))

(defonce initialize
  (do
    (events/listen js/document event-type/KEYDOWN
                   (fn [e]
                     (when (= key-codes/P (.-keyCode e))
                       (toggle-pause!))

                     (when-let [direction ({key-codes/LEFT  :left
                                            key-codes/UP    :up
                                            key-codes/RIGHT :right
                                            key-codes/DOWN  :down} (.-keyCode e))]
                       (move! world-atom direction))))
    (tick!)
    true))

(defn ^:export run []
  (r/render-component [main-component world-atom]
                      (.getElementById js/document "container")))
