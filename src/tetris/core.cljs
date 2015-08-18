(ns tetris.core
  (:require [reagent.core :as r]))

(defonce ^:const width 10)
(defonce ^:const height 20)

(def empty-grid (vec (take height (repeat (vec (take width (repeat \ )))))))

(def blocks {:I ["IIII"]
             :J ["JJJ"
                 "  J"]
             :L ["LLL"
                 "L  "]
             :O ["OO"
                 "OO"]
             :S [" SS"
                 "SS "]
             :T ["TTT"
                 " T "]
             :Z ["ZZ "
                 " ZZ"]})

(defn rotate [block]
  (apply map (fn [& args] (vec (reverse args))) block))

(defn render [{:keys [grid block x y rotations]}]
  (let [block (reduce (fn [block _] (rotate block)) block (range rotations))]
    (when (and (>= x 0)
               (<= (+ x (count (first block))) width))
      (reduce (fn [grid [v block-line]]
                (when grid
                  (when-let [grid-line (get grid (+ y v))]
                    (when (every? (fn [[grid-cell block-cell]]
                                    (or (= block-cell \ )
                                        (= grid-cell \ )))
                                  (map vector
                                       (drop x grid-line)
                                       block-line))
                      (let [line (concat (take x grid-line)
                                         (map (fn [i block-cell]
                                                (if (and (= block-cell \ )
                                                         (< (+ i x) (count grid-line)))
                                                  (nth grid-line (+ i x))
                                                  block-cell))
                                              (iterate inc 0)
                                              block-line)
                                         (drop (+ x (count block-line)) grid-line))]
                        (assoc grid
                               (+ y v)
                               line))))))
              grid
              (map vector (iterate inc 0) block)))))

(defn clear-full [grid]
  (when grid
    (vec
     (reduce (fn [grid i]
               (if (empty? (filter (partial = \ ) (nth grid i)))
                 (vec (concat [(take width (repeat \ ))]
                              (take i grid)
                              (drop (inc i) grid)))
                 grid))
             grid
             (range (count grid))))))

(defn collect-points [grid]
  (count (filter #(every? (partial not= \ ) %) grid)))

(defn new-block [{:keys [grid] :as world}]
  (let [block (rand-nth (vals blocks))
        x (int (- (/ width 2) (/ (count (first block)) 2)))
        new-grid (render world)
        points (collect-points new-grid)
        new-grid (clear-full new-grid)]
    (if (nil? new-grid)
      (assoc world :game-over? true)
      (-> world
          (assoc :grid new-grid
                 :x x, :y 0, :rotations 0, :block block)
          (update :points (fnil + 0) points)))))

(defn move [{:keys [game-over?] :as world} direction]
  (let [new (case direction
              :up (update world :rotations inc)
              :down (update world :y inc)
              :left (update world :x dec)
              :right (update world :x inc)
              world)]
    (cond
      game-over?
      world

      (render new)
      new

      (= direction :down)
      (new-block world)

      :else
      world)))

(defn grid-component [grid]
  [:div.grid
   (for [[y line] (map vector (iterate inc 0) grid)]
     [:div.line {:key (str "line-" y)}
      (for [[x cell] (map vector (iterate inc 0) line)]
        [:div.cell {:key (str "cell-" x)
                    :class (when-not (= cell \ )
                             (str "taken block-" cell))}])])])

(defonce world-atom (r/atom (new-block {:grid empty-grid})))

(defn main-component []
  (let [{:keys [game-over? grid points] :as world} @world-atom
        grid (if game-over? grid (or (render world) grid))]
    [:div.main {:class (when game-over? "game-over")}
     (grid-component grid)
     [:div.points points]
     (when game-over?
       [:a.title {:href "#"
                  :on-click #(reset! world-atom (new-block {:grid empty-grid}))}
        "GAME OVER"])]))

(defn ^:export main [el]
  (r/render-component [main-component] el))

(defonce key-listeren
  (.addEventListener (.-body js/document) "keydown"
                     (fn [e]
                       (when-let [direction ({37 :left, 38 :up, 39 :right, 40 :down}
                                             (.-keyCode e))]
                         (swap! world-atom move direction)))))

(defonce gravity
  (js/setInterval #(swap! world-atom move :down)
                  500))
