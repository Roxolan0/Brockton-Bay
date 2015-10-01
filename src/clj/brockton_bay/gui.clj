(ns brockton-bay.gui
  (require [seesaw.core :refer :all]
           [brockton-bay.game :as game]
           [brockton-bay.library :as lib]
           [brockton-bay.worlds :as worlds]
           [brockton-bay.generation :as generation]
           [brockton-bay.util :as util]
           [brockton-bay.agreements :as agreements]
           [clojure.string :as str]))

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

(defn ask-with-id
  ; HACK : This only works for a very specific map format; may not be flexible enough later on.
  [frame question options-map key-to-display]
  {:pre [(frame? frame)
         (map? options-map)
         (seq options-map)]}
  (let [options (map #(zipmap
                       [:id :to-display]
                       [(key %) (get (val %) key-to-display)])
                     options-map)]
    (:id (input frame question :choices options :to-string :to-display))))

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

(defn person->string [world person-id]
  {:pre [(worlds/world? world)]}
  (let [person (get-in world [:people person-id])
        stats (:stats person)
        player (get-in world [:players (:player-id person)])
        location (get-in world [:locations (:location-id person)])]
    (str "[" (:name player) "]"
         " " (:name person)
         " (Spd " (:speed stats)
         "/Dmg " (:damage stats)
         "/Arm " (:armour stats)
         "/HP " (:hp stats) ")"
         " - " (:name location))))

(defn area-people [world]
  {:pre [(worlds/world? world)]}
  (->>
    (get-in world [:people])
    (keys)
    (map (partial person->string world))
    (str/join "\n")
    (text :multi-line? true :text)
    (scrollable))
  )

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
        (top-bottom-split
          (area-score world)
          (area-locations world))
        (area-people world)))
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
        options (:locations world)]                  ; TODO turn into names to ask, and then back
    (if (:is-human player)
      (ask-with-id frame question options :name)
      (let [location-id (rand-nth (keys options))
            location (get-in world [:locations location-id])
            alert-text (str
                         (:name player)
                         " has chosen to put "
                         (:name person)
                         " in "
                         (:name location)
                         ".")]
        (alert alert-text)
        location-id)
      )))

(defn ask-agreement
  "Returns the chosen agreement between the two players."
  [world frame location-id asked-player-id other-player-id options]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (let [asked-player (get-in world [:players asked-player-id])
        other-player (get-in world [:players other-player-id])
        location (get-in world [:locations location-id])
        question (str
                   (:name asked-player)
                   ": What are you going to do about "
                   (:name other-player)
                   "'s presence at "
                   (:name location)
                   "?")]
    (if (:is-human asked-player)
      (ask frame question options)
      (let [agreement (rand-nth options)
            alert-text (str
                         "At "
                         (:name location)
                         ", "
                         (:name asked-player)
                         " has chosen to "
                         agreement
                         " with/from "
                         (:name other-player)
                         ".")]
        (alert alert-text)
        agreement))))

(defn ask-agreement-from-both
  "Asks the slower player, then the faster player, what agreement they want, and returns the resulting agreement."
  [world frame location-id slower-player-id faster-player-id]
  {:pre [(worlds/world? world)
         (frame? frame)
         (contains? (:players world) slower-player-id)
         (contains? (:players world) faster-player-id)
         (contains? (:locations world) location-id)]}
  (let [slower-choice (ask-agreement world frame location-id
                                     slower-player-id
                                     faster-player-id
                                     (agreements/agreement-options))
        faster-choice (ask-agreement world frame location-id
                                     faster-player-id
                                     slower-player-id
                                     (agreements/agreement-options slower-choice))]
    (agreements/->Agreement
      {slower-player-id slower-choice faster-player-id faster-choice})))

;;; The big gameplay functions

(defn make-agreement [world frame location-id slower-player-id faster-player-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (if (worlds/agreement? world location-id slower-player-id faster-player-id)
    world
    (util/add-with-id
      world
      [:locations location-id :agreements]
      (ask-agreement-from-both world frame location-id
                               slower-player-id
                               faster-player-id))))

(defn make-agreements [world frame person-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (let [location-id (get-in world [:people person-id :location-id])
        player-id (get-in world [:people person-id :player-id])]
    (as->
      (worlds/get-players-ids-at world location-id) $
      (remove #(= player-id %) $)
      (reduce
        (fn [a-world a-player-id] (make-agreement a-world frame location-id player-id a-player-id)) ; TODO: should identify the slowest/fastest player.
        world
        $))))

(defn distribute-person [world frame person-id]
  {:pre [(worlds/world? world)
         (frame? frame)]}
  (as->
    (ask-to-move world frame person-id) $
    (game/change-location world $ person-id)
    (do
      (state-of-the-world $ frame)
      $)
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

(defn battle-phase
  ([world location-id]
   {:pre [(worlds/world? world)]}
   (-> world
       (game/flee-step location-id)
       (game/fight-step location-id)
       (game/share-step location-id)))
  ([world]
   {:pre [(worlds/world? world)]}
   (reduce battle-phase world (keys (:locations world)))))

(defn game-turn [world frame]
  {:pre [(worlds/world? world)]}
  (as-> world $
        (update $ :turn-count inc)
        (game/assign-payoffs $)
        (do (state-of-the-world $ frame)
            (Thread/sleep 1000)                             ; TODO: remove
            $)
        (distribute-people $ frame)
        (battle-phase $)
        (game/clear-location-of-all $)
        (game/clear-agreements $)
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
