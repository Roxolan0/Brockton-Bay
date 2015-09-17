(ns brockton-bay.game
  (:require [brockton-bay.util :as util]
            [brockton-bay.worlds :as worlds]))


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

(defn change-location [world destination-id person-id]
  {:pre [(worlds/world? world)]}
  (assoc-in world [:people person-id :location-id] destination-id)
  )

(defn clear-people-locations [world]
  {:pre [(worlds/world? world)]}
  (reduce
    #(change-location %1 nil %2)
    world
    (keys (:people world))))

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
  (let [locals-id (keys (worlds/people-at-location world location-id))]
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