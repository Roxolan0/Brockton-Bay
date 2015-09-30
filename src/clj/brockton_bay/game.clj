(ns brockton-bay.game
  (:require [brockton-bay.util :as util]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.library :as lib]))


;;; Game-specific functions

(defn random-payoff [turn-count]
  {:pre [(number? turn-count)]}
  (util/rand-in-range (* 50 turn-count) (* 100 turn-count)))

(defn assign-payoffs [world]
  {:pre  [(worlds/world? world)]
   :post [(worlds/world? %)]}
  (reduce
    #(assoc-in %1 [:locations %2 :payoff] (random-payoff (:turn-count world))) ; HACK: use fn with argument names
    world
    (keys (:locations world))))

(defn clean-dead [world]
  {:pre [(worlds/world? world)]}
  (->> (worlds/get-dying-people world)
       (keys)
       (reduce
         (fn [world person-id] (util/dissoc-in world [:people person-id]))
         world
         )))

(defn inflict [world damage victim-id]
  {:pre [(worlds/world? world)
         (number? damage)
         (contains? (:people world) victim-id)]}
  (let [armour (get-in world [:people victim-id :stats :armour])]
    (update-in world
               [:people victim-id :stats :hp]
               -
               (max 0 (- damage armour)))))

(defn local-enemies-id [world person-id]
  {:pre [(worlds/world? world)
         (contains? (:people world) person-id)]}
  (let [person (get-in world [:people person-id])]
    (as->
      world $
      (:people $)
      (filter
        #(= (:location-id person) (:location-id (val %))) $)
      (filter
        #(not= (:player-id person) (:player-id (val %))) $)
      (keys $))))

(defn attack-random-local-enemy
  "Pick a random person from another faction in the same location, and damage them."
  [world person-id]
  {:pre [(worlds/world? world)
         (not (nil? person-id))]}
  (if (zero? (count (local-enemies-id world person-id)))
    world
    (inflict
      world
      (get-in world [:people person-id :stats :damage])
      (rand-nth (local-enemies-id world person-id)))))

(defn combat-turn [world]
  {:pre [(worlds/world? world)]}
  (->
    (reduce
      attack-random-local-enemy
      world
      (keys (:people world)))
    (clean-dead))
  )

(defn combat-phase [world]
  {:pre [(worlds/world? world)]}
  (-> (iterate combat-turn world)
      (nth lib/nb-combat-turns)))

(defn change-location [world destination-id person-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:people person-id :location-id] destination-id))

(defn clear-people-locations [world]
  {:pre [(worlds/world? world)]}
  (reduce
    #(change-location %1 nil %2)
    world
    (keys (:people world))))

(defn clear-agreements-at [world location-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:locations location-id :agreements] {}))

(defn clear-agreements [world]
  {:pre [(worlds/world? world)]}
  (reduce clear-agreements-at world (keys (:locations world))))

(defn give-money [world amount player-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (update-in world [:players player-id :cash] + amount))

(defn give-money-via-person [world amount person-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (give-money world amount (get-in world [:people person-id :player-id])))

(defn split-payoff [world location-id]
  {:pre [(worlds/world? world)]}
  (let [locals-id (keys (worlds/get-people-at-location world location-id))]
    (if (zero? (count locals-id))
      world
      (let [payoff (get-in world [:locations location-id :payoff])
            share (int (/ payoff (count locals-id)))
            remainder (rem payoff (count locals-id))]
        (->
          (reduce #(give-money-via-person %1 share %2) world locals-id)
          (assoc-in [:locations location-id :payoff] remainder)))))
  )

(defn split-payoffs [world]
  {:pre [(worlds/world? world)]}
  (reduce split-payoff world (keys (:locations world))))