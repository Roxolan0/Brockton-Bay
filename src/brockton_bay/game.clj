(ns brockton-bay.game
  (:require [brockton-bay.util :as util]
            [brockton-bay.library
             :as lib
             :refer [->Person-stats]]))

;;; Player

(defrecord Player
  [name
   ^boolean is-human
   cash])

#_(defn player? [x] (instance? Player x))

;;; Location

(defrecord Location
  [name
   ^long payoff])

#_(defn location? [x] (instance? Location x))

;;; Person

(defrecord Person
  [name
   stats
   player-id
   location-id])

#_(defn person? [x] (instance? Person x))

#_(defn random-person-name [name-components]
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
       (reduce #(util/add-with-id %1 :locations (->Location %2 0)) world)))

(defn add-ai-players
  [world cash nb-ais]
  {:pre [(world? world)
         (number? nb-ais)]}
  (let [names (take nb-ais (shuffle lib/ai-names))]
    (reduce #(util/add-with-id %1 :players (->Player %2 false cash))
            world
            names)))

(defn add-templates
  "Pick a random stat template from library and generate a Person with the player-id from it."
  ([world player-id]
   {:pre [(world? world)]}
   (let [template (rand-nth (seq lib/people-templates))]
     (util/add-with-id world :people (->Person
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
  (util/rand-in-range (* 50 turn-count) (* 100 turn-count)))

(defn assign-payoffs [world]
  {:pre [(world? world)]
   :post [(world? %)]}
  (reduce
    #(assoc-in %1 [:locations %2 :payoff] (random-payoff (:turn-count world)))
    world
    (keys (:locations world))))

#_(defn inflict
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

#_(defn attack-local-enemy
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

#_(defn clean-dead
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

#_(defn change-location [destination world person-id]
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