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
    (fn [a-world location-id]
      (assoc-in a-world
                [:locations location-id :payoff]
                (random-payoff (:turn-count world))))
    world
    (keys (:locations world))))

(defn change-location [world destination-id person-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:people person-id :location-id] destination-id))

(defn clear-locations
  ([world person-id]
   {:pre [(worlds/world? world)]}
   (change-location world nil person-id))
  ([world]
   {:pre [(worlds/world? world)]}
   (reduce clear-locations world (keys (:people world)))))

(defn clear-agreements
  ([world location-id]
   {:pre [(worlds/world? world)]}
   (assoc-in world [:locations location-id :agreements] {}))
  ([world]
   {:pre [(worlds/world? world)]}
   (reduce clear-agreements world (keys (:locations world)))))

(defn clear-person [world person-id]
  {:pre [(worlds/world? world)]}
  (util/dissoc-in world [:people person-id]))

(defn clear-deads [world]
  {:pre [(worlds/world? world)]}
  (reduce clear-person world (worlds/get-dying-people-ids world)))

(defn calculate-damage [world attacker-id victim-id]
  {:pre [(worlds/world? world)]}
  (let [base-damage (get-in world [:people attacker-id :stats :damage])
        betrayal-damage (worlds/get-betrayal-damage world attacker-id victim-id)
        armour (get-in world [:people victim-id :stats :armour])]
    (-> base-damage
        (+ betrayal-damage)
        (- armour)
        (max 0))))

(defn inflict [world damage victim-id]
  {:pre [(worlds/world? world)
         (number? damage)]}
  (-> world
      (update-in [:people victim-id :stats :hp] - damage)
      (clear-deads)))

(defn attack [world attacker-id victim-id]
  {:pre [(worlds/world? world)
         (contains? (:people world) victim-id)]}
  (inflict world (calculate-damage world attacker-id victim-id) victim-id))

(defn attack-random-local-enemy
  "Pick a random person from another faction in the same location, and damage them."
  [world person-id]
  {:pre [(worlds/world? world)
         (contains? (:people world) person-id)]}
  (let [local-enemies-ids (worlds/get-local-enemies-ids world person-id)]
    (if (zero? (count local-enemies-ids))
      world
      (attack world person-id (rand-nth local-enemies-ids)))))

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
      (clear-locations person-id)))

(defn attack-or-flee [world person-id]
  {:pre [(worlds/world? world)]}
  (if (worlds/fleeing? world person-id)
    (flee world person-id)
    (attack-random-local-enemy world person-id)))

(defn fight-round [world location-id]
  {:pre [(worlds/world? world)]}
  (->> (worlds/get-people-ids-by-speed world location-id)   ; HACK: overly similar to flee-step (DRY)
       (reduce
         (fn [a-world person-id]
           (if (contains? (:people a-world) person-id)
             (attack-random-local-enemy a-world person-id)
             world))
         world)))

(defn flee-step [world location-id]
  {:pre [(worlds/world? world)]}
  (->> (worlds/get-people-ids-by-speed world location-id)
       (reduce
         (fn [a-world person-id]
           (if (contains? (:people a-world) person-id)
             (attack-or-flee a-world person-id)
             world))
         world)))

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
