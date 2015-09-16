(ns brockton-bay.gui
  (require [clojure.test :as test]
           [seesaw.core :refer :all]
           [brockton-bay.game :as game]
           [brockton-bay.library :as lib]))

;;;; TODO: validate all inputs, and get rid of the "Cancel" button.

;;; UI constants. TODO: put all visible strings in here.

(def game-title "Brockton Bay")

;;; Flexible GUI functions.

(defn frame? [x] (instance? javax.swing.JFrame x))

(defn display [fr content]
  {:pre [(frame? fr)]}
  (config! fr :content content)
  (pack! fr)
  content)

(defn ask
  ([fr message]
   {:pre [(frame? fr)]}
   (input fr message))
  ([fr message options]
   {:pre [(frame? fr)
          (seq options)]}                                   ;false if options is nil or empty
   (input fr message :choices options))
  )

;;; Game-specific GUI functions

(defn ask-new-player [world fr player-number]
  {:pre [(game/world? world)
         (frame? fr)
         (number? player-number)]}
  (as->
    (ask fr (str "Player " player-number ", what do you want to call your faction?")) $
    (game/->Player $ true lib/starting-cash)
    (game/add-with-id world :players $))
  )

(defn ask-nb-humans [world fr]
  {:pre [(game/world? world)
         (frame? fr)]}
  (->>
    (ask fr "How many human players?")
    (Integer/parseInt)
    (inc)
    (range 1)
    (reduce #(ask-new-player %1 fr %2) world)
    ))

(defn ask-nb-ais [world fr]
  {:pre [(game/world? world)
         (frame? fr)]}
  (->>
    (ask fr "How many AI players?")
    (Integer/parseInt)
    (game/add-ai-players world lib/starting-cash)
    ))

(defn area-location [world location-id]
  {:pre [(game/world? world)]}
  (let [location-text
        (str
          (get-in world [:locations location-id :name])
          ": $"
          (get-in world [:locations location-id :payoff]))]
    (text :text location-text)))

(defn area-locations [world]
  {:pre [(game/world? world)]}
  (->> world
       (:locations)
       (keys)
       (map (partial area-location world))
       (reduce top-bottom-split)))

(defn area-score [world]
  {:pre [(game/world? world)]}
  (text :text (str (game/get-players-cash world))))

(defn state-of-the-world [world fr]
  {:pre [(game/world? world)
         (frame? fr)]}
  (do
    (display
      fr
      (top-bottom-split
        (area-score world)
        (area-locations world)))
    (pack! fr)))

;;; The big gameplay functions

(defn distribute-person [world fr person-id]
  {:pre [(game/world? world)
         (frame? fr)]}
  (let [person (get-in world [:people person-id])
        player (get-in world [:players (:player-id person)])
        question (str
                   "Where does "
                   (:name player)
                   " want to put "
                   (:name person)
                   " "
                   (:stats person)
                   "?")
        options (keys (:locations world))                   ; TODO: turn into names to ask, and then back
        ]
    (as->
      (ask fr question options) $
      ;(game/change-location )                               ; TODO: make it work
      world                                                 ; TODO: remove
      )))

(defn distribute-people [world fr]
  ;; HACK: should be more like a While (what if placing a person affected other people's speed or location?)
  {:pre [(game/world? world)
         (frame? fr)]}
  (->> world
       (:people)
       (sort-by #(:speed (:stats (val %))))
       (map :id)
       (reduce #(distribute-person %1 fr %2) world)
       ; while there are people without a location
       ;; find person with lowest speed
       ;; ask owner where to put it
       ;; while there's two factions without a relationship
       ;;; establish relationship
       ))

(defn game-turn [world fr]
  {:pre [(game/world? world)]}
  (as-> world $
        (update $ :turn-count inc)
        (game/assign-payoffs $)
        (do (state-of-the-world $ fr)
            (Thread/sleep 5000)                             ;; TODO: delete this
            $)
        (distribute-people $ fr)
        )
  ; TODO: Call something to distribute all People
  ; TODO: For each location, call something to do combat
  )

(defn show-score [world fr]
  (->>
    (game/get-players-cash world)
    (str "Score: \n")
    (text :multi-line? true :text)
    (scrollable)
    (display fr))
  (pack! fr)
  )

(defn -main
  "Entry point to play the game as a whole."
  []
  (let [fr (frame :title game-title)
        world (game/empty-world)]
    (native!)
    (-> fr pack! show!)
    (display fr "PLACEHOLDER LOADING MESSAGE")
    (as->
      (ask-nb-humans world fr) $
      (ask-nb-ais $ fr)
      (game/add-templates-to-everyone $ lib/people-per-faction)
      (game/add-locations $ lib/nb-locations)
      (iterate #(game-turn % fr) $)
      (nth $ lib/nb-turns)
      (do (show-score $ fr)
          $)
      )))

;;; Test stuff, HACK: remove

(def sample-options ["option 1" "option 2" "option 3"])
