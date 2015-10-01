(ns brockton-bay.generation
  (:require [brockton-bay.worlds :as worlds]
            [brockton-bay.players :as players]
            [brockton-bay.util :as util]
            [brockton-bay.library :as lib]
            [brockton-bay.people :as people]))

(defn add-ai-players
  [world cash nb-ais]
  {:pre [(worlds/world? world)
         (number? nb-ais)
         (<= nb-ais (count lib/ai-names))]}
  (let [names (take nb-ais (shuffle lib/ai-names))]
    (reduce (fn [a-world name]
              (util/add-with-id a-world [:players] (players/->Player name false cash)))
            world
            names)))

(defn add-human-players [world human-names]
  {:pre [(worlds/world? world)]}
  (reduce
    (fn [a-world player-name]
      (util/add-with-id a-world [:players] (players/->Player
                                             player-name
                                             true
                                             lib/starting-cash)))
    world
    human-names))

(defn rand-person
  "Pick a random stat template from library and generate a Person with it and the player-id."
  [player-id]
  (let [template (rand-nth (seq lib/people-templates))]
    (people/->Person
      (key template)
      (val template)
      player-id
      nil)))

(defn add-rand-people
  "Adds a randomly-generated Person with the player-id to the world."
  ([world player-id]
   {:pre [(worlds/world? world)]}
   (util/add-with-id world [:people] (rand-person player-id)))
  ([world nb-templates player-id]
   {:pre [(worlds/world? world)]}
   (reduce add-rand-people world (repeat nb-templates player-id))))

(defn add-rand-people-to-everyone
  "Adds an equal number of randomly-generated People to all players."
  [world nb-templates]
  {:pre [(worlds/world? world)]}
  (reduce
    (fn [a-world player-id] (add-rand-people a-world nb-templates player-id))
    world
    (keys (:players world))))

(defn generate [human-names nb-ais]
  {:pre [(number? nb-ais)]}
  (-> worlds/empty-world
      (add-human-players human-names)
      (add-ai-players lib/starting-cash nb-ais)
      (add-rand-people-to-everyone lib/people-per-player)
      (worlds/add-locations lib/nb-locations)))