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

(defn change-location [world destination-id person-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:people person-id :location-id] destination-id))

(defn clear-location-of [world person-id]
  {:pre [(worlds/world? world)]}
  (change-location world nil person-id))

(defn clear-location-of-all [world]
  {:pre [(worlds/world? world)]}
  (reduce clear-location-of world (keys (:people world))))

(defn clear-agreements-at [world location-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:locations location-id :agreements] {}))

(defn clear-agreements [world]
  {:pre [(worlds/world? world)]}
  (reduce clear-agreements-at world (keys (:locations world))))

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

(defn attack-random-local-enemy
  ; TODO people on a SHARE/SHARE agreement are NOT enemies
  "Pick a random person from another faction in the same location, and damage them."
  [world person-id]
  {:pre [(worlds/world? world)
         (contains? (:people world) person-id)]}
  (if (zero? (count (worlds/get-local-enemies-ids world person-id)))
    world
    (inflict
      world
      (get-in world [:people person-id :stats :damage])
      (rand-nth (worlds/get-local-enemies-ids world person-id)))))

(defn give-money [world amount player-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (update-in world [:players player-id :cash] + amount))

(defn give-money-via-person [world amount person-id]
  {:pre [(worlds/world? world)
         (number? amount)]}
  (give-money world amount (get-in world [:people person-id :player-id])))

(defn grab-local-money [world amount person-id]
  {:pre [(worlds/world? world)]}
  (let [location-id (get-in world [:people person-id :location-id])
        actual-amount (min amount (get-in world [:locations location-id :payoff]))]
    (-> world
        (give-money-via-person actual-amount person-id)
        (update-in [:locations location-id :payoff] - actual-amount))))

(defn flee [world person-id]
  {:pre [(worlds/world? world)]}
  (-> world
      (grab-local-money lib/cash-taken-by-fleeing-people person-id)
      (clear-location-of person-id)))

(defn attack-or-flee [world person-id]
  {:pre [(worlds/world? world)]}
  (if (worlds/fleeing? world person-id)
    (flee world person-id)
    (attack-random-local-enemy world person-id)))

(defn fight-round [world location-id]
; TODO clean-dead after EACH attack
  {:pre [(worlds/world? world)]}
  (->> (worlds/get-people-ids-by-speed-at world location-id)
       (reduce attack-random-local-enemy world)
       (clean-dead)))

(defn flee-step [world location-id]
  ; TODO clean-dead after EACH attack
  {:pre [(worlds/world? world)]}
  (->> (worlds/get-people-ids-by-speed-at world location-id)
       (reduce attack-or-flee world)
       (clean-dead)))

(defn fight-step [world location-id]
  {:pre [(worlds/world? world)]}
  (-> (iterate #(fight-round % location-id) world)
      (nth lib/nb-fight-rounds)))

(defn share-step
  [world location-id]
  {:pre [(worlds/world? world)]}
  (let [local-people-ids (keys (worlds/get-people-at world location-id))]
    (if (zero? (count local-people-ids))
      world
      (let [payoff (get-in world [:locations location-id :payoff])
            share (int (/ payoff (count local-people-ids)))]
        (reduce #(grab-local-money %1 share %2) world local-people-ids)))))
