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

(defn attack-random-enemy-near
  ; TODO people on a SHARE/SHARE agreement are NOT enemies
  "Pick a random person from another faction in the same location, and damage them."
  [world person-id]
  {:pre [(worlds/world? world)
         (contains? (:people world) person-id)]}
  (if (zero? (count (worlds/get-enemies-ids-near world person-id)))
    world
    (inflict
      world
      (get-in world [:people person-id :stats :damage])
      (rand-nth (worlds/get-enemies-ids-near world person-id)))))

(defn fight-round [world location-id]                       ; TODO clean-dead after EACH attack
  {:pre [(worlds/world? world)]}
  (->> world
       (:people)
       (keys)
       (filter (partial worlds/is-at? world location-id))
       (sort (worlds/by-speed-decr world))
       (reduce attack-random-enemy-near world)
       (clean-dead)))

(defn give-money [world amount player-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (update-in world [:players player-id :cash] + amount))

(defn give-money-via-person [world amount person-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (give-money world amount (get-in world [:people person-id :player-id])))

(defn flee-step [world location-id]
  {:pre [(worlds/world? world)]}
  world)                                                    ; TODO this is a stub

(defn fight-step [world location-id]
  {:pre [(worlds/world? world)]}
  (-> (iterate #(fight-round % location-id) world)
      (nth lib/nb-fight-rounds)))

(defn share-step
  [world location-id]
  {:pre [(worlds/world? world)]}
  (let [locals-id (keys (worlds/get-people-at world location-id))]
    (if (zero? (count locals-id))
      world
      (let [payoff (get-in world [:locations location-id :payoff])
            share (int (/ payoff (count locals-id)))
            remainder (rem payoff (count locals-id))]
        (->
          (reduce #(give-money-via-person %1 share %2) world locals-id)
          (assoc-in [:locations location-id :payoff] remainder))))))

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