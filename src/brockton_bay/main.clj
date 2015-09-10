(ns roxolan.brockton-bay.main
  (:require [clojure.string :as string])
  (:require [clojure.data :as data])
  )

(defrecord World [people])

(defrecord Person [id
                   name
                   ^long damage ^long resistance ^long toughness
                   faction
                   location])

(defn people->world [people]
  (->World
    (zipmap (map :id people) people)))

(defn world->people [world]
  (map val (:people world)))

(defn world->ids [world]
  (map :id (world->people world))
  )

(defn random-name [name-components]
  (str
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)
    " "
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)
    ))

(defn random-person [name-components factions locations]
  (->Person
    (java.util.UUID/randomUUID)
    (random-name name-components)
    (rand-int 11)
    (rand-int 7)
    (rand-int 21)
    (rand-nth factions)
    (rand-nth locations)
    ))

(defn random-world
  [name-components factions locations nb-people]
  (people->world
    (repeatedly nb-people
                (partial random-person name-components factions locations))
    ))

(defn by-key [key source]
  (group-by (keyword key) source))

(defn same-keys [template source & keys]
  (filter #(= (select-keys template keys) (select-keys % keys)) source)
  )

(defn different-keys [template source & keys]
  (filter #(= (count (nth (data/diff (select-keys template keys) (select-keys % keys)) 2)) 0) source)
  )

;(defn different-key [template source key]
;  (filter #(not= (select-keys template [key]) (select-keys % [key])) source)
;  )

;(defn different-keys [template source & keys]
;  (reduce (partial different-key template) source keys)

(defn inflict [^long damage target-id world]                ;TODO shouldn't deal negative damage
  (let [inflicted (- damage (get-in world [:people target-id :resistance]))
        target (get-in world [:people target-id])]
    (println (str (:name target) " took " inflicted " damage."))
    (update-in
      world
      [:people target-id :toughness]
      - inflicted
      )
    ))

(defn attack-local-enemy [id world]
  (let [attacker (get-in world [:people id])]
    (as->
      (same-keys attacker (world->people world) :location) $
      (different-keys attacker $ :faction)
      (rand-nth $)
      (do (println $) $)
      (:id $)
      (inflict (:damage attacker) $ world)
      )))

(defn clean-dead [world]
  (->
    (world->people world)
    (filter #(> (:toughness %) 0))
    (people->world))
  )

(defn teleport [id destination world]
  (do
    (println (str
               (get-in world [:people id :name])
               " teleported to "
               destination "."))
    (assoc
      (get-in world [:people id])
      :location
      destination
      )
    ))

(defn teleport-if-bored [id world]

  )

(defn on-tick [world]
  (as->
    (reduce world attack-local-enemy (world->ids world)) $
    (clean-dead $)
    (reduce $ teleport-if-bored)
    (people->world $)
    ))



(def sample-name-components
  ["angel" "demon" "beast" "monster"
   "fire" "ice"
   "arrow" "knife"
   "eye" "muscle" "skull" "bone" "blood"
   "death" "power"
   "dark" "light"
   "twilight" "dawn"
   "wise" "strong" "cold"
   "killer" "hunter" "stomper"])

(def sample-factions ["red" "blue"])

(def sample-locations ["graveyard" "volcano" "space" "fortress"])

(defn sample-person []
  (random-person
    sample-name-components
    sample-factions
    sample-locations))

(defn sample-world []
  (random-world
    sample-name-components
    sample-factions
    sample-locations
    20))

(def stuff (sample-world))
(def dude (rand-nth (world->people stuff)))