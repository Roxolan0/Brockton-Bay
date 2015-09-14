(ns brockton-bay.main
  (:require [clojure.string :as string]
            [clojure.data :as data]
            [clojure.stacktrace :as stacktrace])
  (:import (java.util UUID))
  )

(defrecord World [locations
                  people])

(defrecord Person
  [id
   name
   ^long speed ^long damage ^long armour ^long hp
   faction
   location])

;(defn people->world [people]
;  (->World
;    (zipmap (map :id people) people)))

(defn world->people [world]
  (map val (:people world)))

(defn world->ids [world]
  (map :id (world->people world)))

(defn random-name [name-components]
  {:pre [(not-empty name-components)]}
  (str
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)
    ;" "
    ;(string/capitalize (rand-nth name-components))
    ;(rand-nth name-components)
    ))

(defn rand-in-range
  "Generates a random int between min-incl and max-incl (both included)."
  [min-incl, max-incl]
  {:pre [(integer? min-incl)
         (integer? max-incl)
         (<= min-incl max-incl)]}
  (+ min-incl (rand-int (- (+ 1 max-incl) min-incl))))

(defn random-person [name-components factions locations]
  {:pre [(not-empty factions)
         (not-empty locations)]}
  (->Person
    (UUID/randomUUID)                                       ;id
    (random-name name-components)                           ;name
    (rand-in-range 0 10)                                    ;speed
    (rand-in-range 1 5)                                     ;damage
    (rand-in-range 0 3)                                     ;armour
    (rand-in-range 1 10)                                    ;hp
    (rand-nth factions)                                     ;faction
    (rand-nth locations)                                    ;location
    ))

(defn random-world
  [name-components factions locations nb-people]
  (let [people
        (repeatedly
          nb-people
          (partial random-person name-components factions locations))]
    (->World
      locations
      (zipmap (map :id people) people))))

(defn by-key [key source]
  (group-by (keyword key) source))

(defn same-keys [template source & keys]
  (filter #(= (select-keys template keys) (select-keys % keys)) source))

(defn different-keys [template source & keys]
  (filter #(= (count (nth (data/diff (select-keys template keys) (select-keys % keys)) 2)) 0) source))

;(defn different-key [template source key]
;  (filter #(not= (select-keys template [key]) (select-keys % [key])) source)
;  )

;(defn different-keys [template source & keys]
;  (reduce (partial different-key template) source keys)

(defn inflict [damage world id]
  {:pre [(integer? damage)
         (instance? World world)
         (not-empty (get-in world [:people id]))]}
  (let [inflicted (max 1 (- damage (get-in world [:people id :armour])))
        target (get-in world [:people id])
        outcome (update-in world [:people id :hp] - inflicted)]
    (println (str (:name target) " took " inflicted " damage."))
    outcome))

(defn attack-local-enemy [world id]
  {:pre [(instance? World world)
         (not-empty (get-in world [:people id]))]}
  (let [attacker (get-in world [:people id])]
    (as->
      (same-keys attacker (world->people world) :location) $
      (different-keys attacker $ :faction)
      (if (empty? $)
        world
        (as->
          (rand-nth $) $2
          (do
            (println (str "*"
                          (:location attacker)
                          ": "
                          (:name attacker)
                          " ("
                          (:faction attacker)
                          ") attacks "
                          (:name $2)
                          " ("
                          (:faction $2)
                          ")."))
            $2)
          (:id $2)
          (inflict (:damage attacker) world $2)
          )))))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn clean-dead [world]
  ;{:pre [(instance? World world)]}
  (let [corpse-ids (map :id (filter #(<= (:hp %) 0) (vals (get-in world [:people]))))
        outcome (reduce (fn [x y] (dissoc-in x [:people y])) world corpse-ids)]
    (print "*** Deaths: ")
    (println (map :name (first
                          (data/diff
                            (set (world->people world))
                            (set (world->people outcome))))))
    outcome
    ))

(defn teleport [destination world id]
  {:pre [(not-empty destination)
         (instance? World world)
         (not-empty (get-in world [:people id]))]}
  (do
    (println (str
               (get-in world [:people id :name])
               " teleported to "
               destination "."))
    (assoc-in world [:people id :location] destination)
    )
  ;(assoc-in world [:people id :location] destination)
  )

(defn teleport-if-bored [world id]
  {:pre [(instance? World world)
         (not-empty (get-in world [:people id]))]}
  (let [person (get-in world [:people id])]
    (as->
      (same-keys person (world->people world) :location) $
      (different-keys person $ :faction)
      (count $)
      (if (= $ 0)
        (as->
          (different-keys person (world->people world) :faction) $2
          (if (empty? $2)
            world                                           ;This person has no enemies left anywhere in the world.
            (as-> (rand-nth $2) $3
                  (:location $3)
                  (teleport $3 world id))))
        world                                               ;This person has enemies left at its current location.
        ))))

(defn on-tick
  "Each person attacks a random enemy in the same location (if any).
  Once their location is clear, they teleport to another location."
  [world]
  {:pre [(instance? World world)]}
  (as->
    (reduce attack-local-enemy world (world->ids world)) $
    (clean-dead $)
    (reduce teleport-if-bored $ (world->ids $))
    ))



(def sample-name-components
  ["angel" "demon" "beast" "monster"
   "fire" "ice"
   "arrow" "knife" "rainbow"
   "eye" "muscle" "skull" "bone" "blood"
   "death" "power"
   "dark" "light"
   "twilight" "dawn"
   "wise" "strong" "cold"
   "killer" "hunter" "stomper"])

(def sample-factions ["red" "blue"])

(def sample-locations ["Graveyard" "Volcano" "Space" "Fortress"])

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

;test stuff
(def stuff (sample-world))
(def dude (rand-nth (world->people stuff)))
(defn affect-dude [thing-to-do] (clojure.pprint/pprint (get-in thing-to-do [:people (:id dude)])))
(defn ticks [x] (take x (iterate on-tick stuff)))
;(stacktrace/print-stack-trace *e 5)