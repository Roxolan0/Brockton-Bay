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
  (config! fr :content content)
  (pack! fr)
  content)

(defn ask
  ([fr message]
   {:pre [(frame? fr)]}
   (input fr message))
  ([frame message options]
   {:pre [(frame? frame)
          (seq options)]}                                   ;false if options is nil or empty
   (input frame message :choices options))
  )

;;; Game-specific GUI functions

(defn ask-new-player [world fr player-number]
  {:pre [(frame? fr)]}
  (as->
    (ask fr (str "Player " player-number ", what do you want to call your faction?")) $
    (game/->Player $ true lib/starting-cash)
    (game/add-with-id world :players $))
  )

(defn ask-nb-humans [world fr]
  {:pre [(frame? fr)]}
  (->>
    (ask fr "How many human players?")
    (Integer/parseInt)
    (inc)
    (range 1)
    (reduce #(ask-new-player %1 fr %2) world)
    ))

(defn ask-nb-ais [world fr]
  {:pre [(frame? fr)]}
  (->>
    (ask fr "How many AI players?")
    (Integer/parseInt)
    (game/add-ai-players world lib/starting-cash)
    ))

;;; The big gameplay functions

(defn game-turn [world]
  (-> world
      (update :turn-count inc))
  ; TODO: For each Location, set a random Payoff.
  ; TODO: Print full state of the World.
  ; TODO: Call something to distribute all People
  ; TODO: For each location, call something to do combat
  )

(defn show-score [world fr]
  (->>
     (game/get-players-cash world)
     (str "Final score: \n")
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
      (iterate game-turn $)
      (nth $ lib/nb-turns)
      (show-score $ fr)
      )))

;;; Test stuff, HACK: remove

(def sample-options ["option 1" "option 2" "option 3"])
