(ns tetris.core
  (:require [reagent.core :as r]))

(defonce ^:const initial-tick-frequency 500)
(defonce ^:const width 10)
(defonce ^:const height 20)

(defonce empty-grid
  (vec (take height (repeat (vec (take width (repeat \ )))))))

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

(defn score [grid]
  (reduce (fn [[n grid] grid-line]
            (if (every? (partial not= \ ) grid-line)
              [(inc n) (into [(take width (repeat \ ))] grid)]
              [n (conj grid grid-line)]))
          [0 []]
          grid))

(defn new-block [world]
  (let [block (rand-nth blocks)
        x (int (- (/ width 2) (/ (count (first block)) 2)))
        [score grid] (-> world render score)
        new (-> world
                (assoc :grid grid, :x x, :y 0, :rotations 0, :block block)
                (update :score (fnil + 0) score))]
    (if (valid? new)
      new
      (assoc new :game-over? true))))

(defn move [{:keys [game-over?] :as world} direction]
  (let [new (case direction
              :up (update world :rotations inc)
              :down (update world :y inc)
              :left (update world :x dec)
              :right (update world :x inc)
              world)]
    (cond game-over? world
          (valid? new) new
          (= direction :down) (new-block world)
          :else world)))

(defn grid-component [grid]
  [:div.grid
   (for [[y line] (map vector (iterate inc 0) grid)]
     [:div.line
      {:key (str "line-" y)}
      (for [[x cell] (map vector (iterate inc 0) line)]
        [:div.cell
         {:key (str "cell-" x)
          :class (when-not (= cell \ )
                   (str "taken block-" cell))}])])])

(defn move! [world-atom direction]
  (swap! world-atom move direction))

(defn main-component [world-atom]
  (let [{:keys [game-over? grid score] :as world} @world-atom
        button (fn [dir]
                 (let [f #(do (move! world-atom dir)
                              (.preventDefault %))]
                   [:button {:class (name dir), :on-click f, :on-touch-start f} (name dir)]))]
    [:div.main
     {:class (when game-over? "game-over")}

     (grid-component (render world))
     [:div.score score]
     [:div.controls
      (button :left)
      (button :right)
      (button :up)
      (button :down)]

     (when game-over?
       [:a.title
        {:href "#"
         :on-click #(reset! world-atom
                            (new-block {:grid empty-grid}))}
        "GAME OVER"])]))

(defonce world-atom (r/atom (new-block {:grid empty-grid})))

(defonce key-listener
  (.addEventListener (.-body js/document) "keydown"
                     (fn [e]
                       (when-let [direction ({37 :left, 38 :up, 39 :right, 40 :down}
                                             (.-keyCode e))]
                         (move! world-atom direction)))))

(defn tick! []
  (move! world-atom :down)
  (let [tick-frequency (- initial-tick-frequency
                          (-> world-atom deref :score (* 10)))]
    (js/setTimeout tick! tick-frequency)))

(defonce game-loop
  (tick!))

(defn ^:export run []
  (r/render-component [main-component world-atom]
                    (.getElementById js/document "container")))
