(ns brockton-bay.gui
  (require [seesaw.core :refer :all]
           [brockton-bay.game :as game]
           [brockton-bay.library :as lib]
           [brockton-bay.worlds :as worlds]
           [brockton-bay.generation :as generation]))

;;;; TODO: validate all inputs, and get rid of the "Cancel" button.

;;; UI constants. TODO: put all visible strings in here.

(def game-title "Brockton Bay")
(def question-nb-humans "How many human players?")
(def question-nb-ais "How many AIs?")

;;; Flexible GUI functions.

(defn frame? [x] (instance? javax.swing.JFrame x))

(defn display [frame content]
  {:pre [(frame? frame)]}
  (config! frame :content content)
  (pack! frame)
  content)

(defn ask
  ([frame question]
   {:pre [(frame? frame)]}
   (input frame question))
  ([frame question options]
   {:pre [(frame? frame)
          (seq options)]}
   (input frame question :choices options))
  )

(defn ask-with-id [frame question options-map]
  {:pre [(frame? frame)
         (map? options-map)
         (seq options-map)]}
  (input frame question :choices options-map)
  ;; TODO continue work from here
  )

(defn ask-number [frame question]
  {:pre [(frame? frame)]}
  (-> (ask frame question)
      (Integer/parseInt)))

;;; GUI elements

(defn area-location [world location-id]
  {:pre [(worlds/world? world)]}
  (let [location-text
        (str
          (get-in world [:locations location-id :name])
          ": $"
          (get-in world [:locations location-id :payoff]))]
    (text :text location-text)))

(defn area-locations [world]
  {:pre [(worlds/world? world)]}
  (->> world
       (:locations)
       (keys)
       (map (partial area-location world))
       (reduce top-bottom-split)))

(defn area-score [world]
  {:pre [(worlds/world? world)]}
  (text :text (str (worlds/get-players-cash world))))

(defn state-of-the-world [world frame]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (do
    (display
      frame
      (top-bottom-split
        (area-score world)
        (area-locations world)))
    (pack! frame)))

;;; GUI-player interactions

(defn ask-human-name [frame player-number]
  {:pre [(frame? frame)
         (number? player-number)]}
  (ask frame (str "Player " player-number ", what do you want to call your faction?")))

(defn ask-human-names [frame nb-players]
  {:pre [(frame? frame)
         (number? nb-players)]}
  (map (partial ask-human-name frame) (range 1 (inc nb-players))))

(defn ask-to-move
  "Returns the chosen location-id."
  [world frame person-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (let [person (get-in world [:people person-id])
        player (get-in world [:players (:player-id person)])
        question (str
                   "Where does "
                   (:name player)
                   " want to put "
                   (:name person)
                   "?")
        options (keys (:locations world))]                  ; TODO turn into names to ask, and then back
    (ask frame question options)))

;;; The big gameplay functions

(defn make-agreements [world frame person-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (let [location-id (get-in world [:people person-id :location-id])
        player-id (get-in world [:people person-id :player-id])]
    ;get other players at that location
    ;for each of them, do (util/contains-many? that-player player-id)
    ;if no, call (make-agreement world frame location-id that-player player-id)
    world))                                                       ; TODO

(defn distribute-person [world frame person-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (as->
    (ask-to-move world frame person-id) $
    (game/change-location world $ person-id)
    (make-agreements $ frame person-id)))

(defn distribute-people [world frame]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (loop [cur-world world]
    (if (zero? (count (worlds/get-people-without-location cur-world)))
      cur-world
      (recur (distribute-person
               cur-world
               frame
               (key
                 (apply min-key
                        #(:speed (:stats (val %)))
                        (worlds/get-people-without-location cur-world))))))))

(defn game-turn [world frame]
  {:pre [(worlds/world? world)]}
  (as-> world $
        (update $ :turn-count inc)
        (game/assign-payoffs $)
        (do (state-of-the-world $ frame)
            (Thread/sleep 1000)                             ; TODO: remove
            $)
        (distribute-people $ frame)
        ; TODO: AIs' people should be distributed automatically.
        ; TODO: (low priority) manage player relationships
        (game/combat-phase $)
        (game/split-payoffs $)
        (game/clear-people-locations $)
        )
  )

(defn show-score [world frame]
  (->>
    (worlds/get-players-cash world)
    (str "Score: \n")
    (text :multi-line? true :text)
    (scrollable)
    (display frame))
  (pack! frame)
  )

(defn play [world frame]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (as-> world $
        (iterate #(game-turn % frame) $)
        (nth $ lib/nb-turns)))

(defn -main
  "Entry point to play the game as a whole."
  []
  (let [frame (frame :title game-title)]
    (native!)
    (-> frame pack! show!)
    (display frame "PLACEHOLDER LOADING MESSAGE")
    (->
      (generation/generate
        (ask-human-names frame (ask-number frame question-nb-humans))
        (ask-number frame question-nb-ais)
        )
      (play frame)
      (#(do
         (show-score % frame)
         %))
      )))
