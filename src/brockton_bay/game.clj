(ns brockton-bay.game
  (:require [clojure.string :as string]
            [clojure.data :as data]
            [clojure.stacktrace :as stacktrace]
            [clojure.test :as test]
            [brockton-bay.library
             :as lib
             :refer [->Person-stats]])
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

(defn add-with-id
  "Add the source to the destination sub-map, indexed by an id (provided or generated)."
  ([destination submap id source]
   (update-in destination [submap] conj {id source}))
  ([destination submap source]
   (add-with-id destination submap (UUID/randomUUID) source)))

;(defn add-with-id
;  "Add the source to the destination sub-map found at the key, indexed by source's id."
;  [destination key source]
;  (update-in destination [key] conj {(:id source) source}))

;;; Player

(defrecord Player
  [name
   ^boolean is-human
   cash])

(defn player? [x] (instance? Player x))

;;; Location

(defrecord Location
  [name
   ^long payoff])

(defn location? [x] (instance? Location x))

;;; Person

(defrecord Person
  [name
   stats
   player-id
   location-id])

(defn person? [x] (instance? Person x))

(defn random-person-name [name-components]
  {:pre  [(seq name-components)]
   :post [(string? %)
          (seq %)]}
  (str
    (string/capitalize (rand-nth name-components))
    (rand-nth name-components)))

;;; World

(defrecord World
  [players
   locations
   people
   turn-count])

(defn world? [x] (instance? World x))

(defn empty-world []
  (->World {} {} {} 0))

(defn get-players-cash [world]
  {:pre [(world? world)]}
  (zipmap
    (map :name (vals (:players world)))
    (map :cash (vals (:players world))))
  )

;; HACK: the add- should be just one generic function.
(defn add-locations
  "Pick some random locations from library and add them to the world."
  [world nb-locations]
  {:pre [(world? world)
         (number? nb-locations)]}
  (->> lib/location-names
       (shuffle)
       (take nb-locations)
       (reduce #(add-with-id %1 :locations (->Location %2 0)) world)))

(defn add-ai-players
  [world cash nb-ais]
  {:pre [(world? world)
         (number? nb-ais)]}
  (let [names (take nb-ais (shuffle lib/ai-names))]
    (reduce #(add-with-id %1 :players (->Player %2 false cash))
            world
            names)))

(defn add-templates
  "Pick a random stat template from library and generate a Person with the player-id from it."
  ([world player-id]
   {:pre [(world? world)]}
   (let [template (rand-nth (seq lib/people-templates))]
     (add-with-id world :people (->Person
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

(defn random-payoff [turn-count]
  {:pre [(number? turn-count)]}
  (rand-in-range (* 50 turn-count) (* 100 turn-count)))

(defn assign-payoffs [world]
  {:pre [(world? world)]
   :post [(world? %)]}
  (reduce
    #(assoc-in %1 [:locations %2 :payoff] (random-payoff (:turn-count world)))
    world
    (keys (:locations world))))

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

(defn change-location [destination world person-id]
  ;; REVIEW: no unit tests and seen a bunch of refactorings go by, who knows if it still works.
  ;; TODO: remove prints once GUI is working.
  {:pre [(seq destination)
         (world? world)
         (seq (get-in world [:people person-id]))]}
  (do
    (println (str
               (get-in world [:people person-id :name])
               " teleported to "
               destination "."))
    (assoc-in world [:people person-id :location] destination)))

;;; Test stuff, HACK: remove

;; (clojure.stacktrace/print-stack-trace *e 5)