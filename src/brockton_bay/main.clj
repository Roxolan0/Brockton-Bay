(ns brockton-bay.main
  (:require [clojure.string :as string]
            [clojure.data :as data]
            [clojure.stacktrace :as stacktrace]
            [clojure.test :as test]
            [brockton-bay.library :as lib])
  (:import (java.util UUID)))

;;; Flexible functions.

(defn dissoc-in
  ;From clojure/core.incubator. Will likely be in clojure core someday.
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

(defn rand-in-range
  "Generates a random int between min and max (both included)."
  [min, max]
  {:pre  [(integer? min)
          (integer? max)
          (<= min max)]
   :post [(integer? %)
          (<= min % max)]}
  (+ min (rand-int (- (inc max) min))))

(defn same-keys
  "Finds all maps in 'source' that have the same values as the 'template' map for each of the 'keys'."
  [template source & keys]
  (filter #(= (select-keys template keys) (select-keys % keys)) source))

(defn different-keys
  ;; HACK : got frustrated on this one, definitely some better solutions.
  [template source & keys]
  "Finds all maps in 'source' that have NONE of the same values as the 'template' map for the 'keys'."
  (filter #(zero? (count (nth
                           (data/diff
                             (select-keys template keys)
                             (select-keys % keys))
                           2)))
          source))

;;; Player

(defrecord Player
  [id
   ^boolean is-human
   faction
   cash])

(defn player? [x] (instance? Player x))

;;; Person

(defrecord Person
  [id
   name
   ^long speed ^long damage ^long armour ^long hp
   faction
   location])

(defn random-name [name-components]
  {:pre  [(seq name-components)]
   :post [(string? %)
          (seq %)]}
  (str
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)))

(defn random-person [name-components factions locations]
  {:pre [(seq factions)
         (seq locations)]}
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

;;; World

(defrecord World
  [players
   locations
   people])

(defn world? [x] (instance? World x))

(defn world->people [world]
  (map val (:people world)))

(defn world->ids [world]
  (map :id (world->people world)))

(defn empty-world [locations]
  (->World []
           locations
           []))

(defn random-world
  ;; HACK: should be based on empty-world and an add-person.
  [name-components factions locations nb-people]
  (let [people
        (repeatedly
          nb-people
          (partial random-person name-components factions locations))]
    (->World
      []
      locations
      (zipmap (map :id people) people))))

(defn add-player
  ([world player]
   {:pre [(world? world)
          (player? player)]}
   (update-in world [:players] conj player))
  ([world is-human faction]
   {:pre [(world? world)
          (instance? Boolean is-human)
          (seq faction)]}
   (add-player
     world
     (->Player
       (UUID/randomUUID)
       is-human
       faction
       0))))

;;; Game-specific functions

(defn inflict
  "Deals damage to the person with that id, taking armour into account."
  [damage world id]
  {:pre [(integer? damage)
         (world? world)
         (seq (get-in world [:people id]))]}
  (let [inflicted (max 1 (- damage (get-in world [:people id :armour])))
        target (get-in world [:people id])
        outcome (update-in world [:people id :hp] - inflicted)]
    (println (str (:name target) " took " inflicted " damage."))
    outcome))

(defn attack-local-enemy
  ;; HACK: can probably be made much cleaner.
  ;; TODO: remove prints once GUI is working.
  "Pick a random person from another faction in the same location, and damage them."
  [world id]
  {:pre [(world? world)
         (seq (get-in world [:people id]))]}
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
          (inflict (:damage attacker) world $2))))))

(defn clean-dead
  ;; TODO: remove prints once GUI is working.
  "Removes all people with <= 0 HP."
  [world]
  {:pre [(world? world)]}
  (let [corpse-ids (map :id (filter #(<= (:hp %) 0) (vals (get-in world [:people]))))
        outcome (reduce #(dissoc-in %1 [:people %2]) world corpse-ids)]
    (print "*** Deaths: ")
    (println (map :name (first
                          (data/diff
                            (set (world->people world))
                            (set (world->people outcome))))))
    outcome))

(defn change-location [destination world id]
  ;; TODO: remove prints once GUI is working.
  {:pre [(seq destination)
         (world? world)
         (seq (get-in world [:people id]))]}
  (do
    (println (str
               (get-in world [:people id :name])
               " teleported to "
               destination "."))
    (assoc-in world [:people id :location] destination)))

(defn teleport-if-bored
  ;; HACK: can probably be made much cleaner.
  "Check if this person's location contains an enemy. If not, teleport it to a location that does."
  [world id]
  {:pre [(world? world)
         (seq (get-in world [:people id]))]}
  (let [person (get-in world [:people id])]
    (as->
      (same-keys person (world->people world) :location) $
      (different-keys person $ :faction)
      (count $)
      (if (zero? $)
        (as->
          (different-keys person (world->people world) :faction) $2
          (if (empty? $2)
            world                                           ;This person has no enemies left anywhere in the world.
            (as-> (rand-nth $2) $3
                  (:location $3)
                  (change-location $3 world id))))
        world))))                                           ;This person has enemies left at its current location.

(defn on-tick
  "Each person attacks a random enemy in the same location (if any).
  Once their location is clear, they teleport to another location."
  [world]
  {:pre [(world? world)]}
  (as->
    (reduce attack-local-enemy world (world->ids world)) $
    (clean-dead $)
    (reduce teleport-if-bored $ (world->ids $))))

;;; Test stuff, remove before selling code for $100k

(def sample-factions ["red" "blue"])

(defn sample-person []
  (random-person
    lib/name-components
    sample-factions
    lib/locations))

(defn sample-world []
  (random-world
    lib/name-components
    sample-factions
    lib/locations
    20))

(def stuff (sample-world))
(def dude (rand-nth (world->people stuff)))
(defn ticks [x] (take x (iterate on-tick stuff)))
;(stacktrace/print-stack-trace *e 5)