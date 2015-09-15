(ns brockton-bay.game
  (:require [clojure.string :as string]
            [clojure.data :as data]
            [clojure.stacktrace :as stacktrace]
            [clojure.test :as test]
            [brockton-bay.library
             :as lib
             :refer [->Person-stats]])
  (:import (java.util UUID)
           (brockton_bay.library Location)))

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

(defn add-with-id
  "Add the source to the destination sub-map found at the key, indexed by source's id."
  [destination key source]
  (update-in destination [key] conj {(:id source) source}))

;;; Player

(defrecord Player
  [id
   name
   ^boolean is-human
   cash])

(defn player? [x] (instance? Player x))

(defn new-player [name is-human cash]
  (->Player (UUID/randomUUID)
            name
            is-human
            cash))

;;; Location

(defn location? [x] (instance? Location x))

;;; Person

(defrecord Person
  [id
   name
   stats
   player-id
   location-id])

(defn person? [x] (instance? Person x))

(defn new-person [name stats player-id location-id]
  (->Person (UUID/randomUUID)
            name
            stats
            player-id
            location-id))

(defn random-person-name [name-components]
  {:pre  [(seq name-components)]
   :post [(string? %)
          (seq %)]}
  (str
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)))

;(defn random-person [name-components player-ids location-ids]
;  {:pre [(seq player-ids)
;         (seq location-ids)]}
;  (->Person
;    (UUID/randomUUID)                                       ;id
;    (random-person-name name-components)                           ;name
;    (->Person-stats
;      (rand-in-range 0 10)                                  ;speed
;      (rand-in-range 1 5)                                   ;damage
;      (rand-in-range 0 3)                                   ;armour
;      (rand-in-range 1 10))                                 ;hp
;    (rand-nth player-ids)                                   ;player-id
;    (rand-nth location-ids)                                 ;location-id
;    ))

;;; World

(defrecord World
  [players
   locations
   people])

(defn world? [x] (instance? World x))

;(defn world->people [world]
;  (map val (:people world)))

;(defn world->people-ids [world]
;  (map :id (world->people world)))

;(defn world->factions [world]
;  (set (map :faction (:players world))))

(defn empty-world []
  (->World {} {} {}))

;(defn random-world
;  ;; HACK: should be based on empty-world and the add- functions.
;  [name-components factions locations nb-people]
;  (let [people
;        (repeatedly
;          nb-people
;          (partial random-person name-components factions (keys locations)))]
;    (->World
;      []
;      locations
;      (zipmap (map :id people) people))))

;(defn add-player [world player]
;  {:pre [(world? world)
;         (player? player)]}
;  (update-in world [:players] conj {(:id player) player}))

(defn add-ai-players
  [world cash ai-number]
  {:pre [(world? world)
         (number? ai-number)]}
  (let [names (take ai-number (shuffle lib/ai-names))]
    (reduce #(add-with-id %1 :players (new-player %2 false cash))
            world
            names)))

;(defn add-person
;  ([world person]
;   {:pre [(world? world)
;          (person? person)]}
;   (update-in world [:people] conj person)))

(defn add-templates
  "Pick a random stat template from library and generate a Person with the player-id from it."
  ([world player-id]
   {:pre [(world? world)]}
   (let [template (rand-nth (seq lib/people-templates))]
     (add-with-id world :people (new-person
                                  (key template)
                                  (val template)
                                  player-id
                                  nil))))
  ([nb-templates world player-id]
   {:pre [(world? world)]}
   (reduce add-templates world (repeat nb-templates player-id))
    ))

(defn add-templates-to-everyone
  [world nb-templates]
  {:pre [(world? world)]}
  (reduce
    (partial add-templates nb-templates)
    world (keys (:players world))))

;;; Game-specific functions

(defn inflict
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  "Deals damage to the person with that id, taking armour into account."
  [damage world id]
  {:pre [(integer? damage)
         (world? world)
         (seq (get-in world [:people id]))]}
  (let [inflicted (max 1 (- damage (get-in world [:people id :stats :armour])))
        target (get-in world [:people id])
        outcome (update-in world [:people id :stats :hp] - inflicted)]
    (println (str (:name target) " took " inflicted " damage."))
    outcome))

(defn attack-local-enemy
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  ;; HACK: can probably be made much cleaner.
  ;; TODO: remove prints once GUI is working.
  "Pick a random person from another faction in the same location, and damage them."
  [world id]
  {:pre [(world? world)
         (seq (get-in world [:people id]))]}
  (let [attacker (get-in world [:people id])]
    (as->
      (same-keys attacker (vals (:people world)) :location) $
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
          (inflict (:damage (:stats attacker)) world $2))))))

(defn clean-dead
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  ;; TODO: remove prints once GUI is working.
  "Removes all people with <= 0 HP."
  [world]
  {:pre [(world? world)]}
  (let [corpse-ids (map :id (filter #(<= (:hp (:stats %)) 0) (vals (get-in world [:people]))))
        outcome (reduce #(dissoc-in %1 [:people %2]) world corpse-ids)]
    (print "*** Deaths: ")
    (println (map :name (first
                          (data/diff
                            (set (vals (:people world)))
                            (set (vals (:people outcome)))))))
    outcome))

(defn change-location [destination world id]
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
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
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  ;; HACK: can probably be made much cleaner.
  ;; Who cares anyway, this code was only useful for the old version of the game.
  "Check if this person's location contains an enemy. If not, teleport it to a location that does."
  [world id]
  {:pre [(world? world)
         (seq (get-in world [:people id]))]}
  (let [person (get-in world [:people id])]
    (as->
      (same-keys person (vals (:people world)) :location) $
      (different-keys person $ :faction)
      (count $)
      (if (zero? $)
        (as->
          (different-keys person (vals (:people world)) :faction) $2
          (if (empty? $2)
            world                                           ;This person has no enemies left anywhere in the world.
            (as-> (rand-nth $2) $3
                  (:location $3)
                  (change-location $3 world id))))
        world))))                                           ;This person has enemies left at its current location.

(defn on-tick
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  ;; Who cares anyway, this code was only useful for the old version of the game.
  "Each person attacks a random enemy in the same location (if any).
  Once their location is clear, they teleport to another location."
  [world]
  {:pre [(world? world)]}
  (as->
    (reduce attack-local-enemy world (keys (:people world))) $
    (clean-dead $)
    (reduce teleport-if-bored $ (keys (:people world)))))

;;; Test stuff, HACK: remove

(def sample-factions ["red" "blue"])

;(defn sample-person []
;  (random-person
;    lib/person-name-components
;    sample-factions
;    lib/locations))
;
;(defn sample-world []
;  (random-world
;    lib/person-name-components
;    sample-factions
;    lib/locations
;    20))
;
;(def stuff (sample-world))
;(def dude (rand-nth (world->people stuff)))
;(defn ticks [x] (take x (iterate on-tick stuff)))
;;(stacktrace/print-stack-trace *e 5)