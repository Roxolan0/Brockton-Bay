(ns brockton-bay.game-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.game :as game]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.generation :as generation]
            [brockton-bay.util :as util]
            [brockton-bay.people :as people]
            [brockton-bay.library :as lib]))

(def test-world-for-combat
  (-> worlds/empty-world
      (util/add-with-id [:people] "attacker" (people/->Person
                                               nil
                                               (lib/->Person-stats 0 4 0 10)
                                               nil
                                               nil))
      (util/add-with-id [:people] "naked" (people/->Person
                                            nil
                                            (lib/->Person-stats 0 0 0 10)
                                            nil
                                            nil))
      (util/add-with-id [:people] "armoured" (people/->Person
                                             nil
                                             (lib/->Person-stats 0 0 3 10)
                                             nil
                                             nil))
      (util/add-with-id [:people] "well-armoured" (people/->Person
                                           nil
                                           (lib/->Person-stats 0 0 10 10)
                                           nil
                                           nil))))

(facts "random-payoff"
  (fact "Returns a positive number."
        (pos? (game/random-payoff 1))
        => true))

(facts "assign-payoffs"
  (fact "Sets the payoff of every location as a positive number."
        (as->
          worlds/empty-world $
          (worlds/add-locations $ 3)
          (assoc $ :turn-count 1)
          (game/assign-payoffs $)
          (:locations $)
          (vals $)
          (map :payoff $)
          (every? pos? $))
        => true))

(facts "clean-dead"
  (fact "Removes dead people and no-one else."
        (as->
          worlds/empty-world $
          (generation/add-rand-people $ 5 "Bob")
          (assoc-in $ [:people (first (keys (:people $))) :stats :hp] 0)
          (assoc-in $ [:people (last (keys (:people $))) :stats :hp] 0)
          (game/clear-deads $)
          (:people $)
          (count $)
          )
        => 3))

(facts "About attack."
  (fact "attack lowers HP of the unarmoured."
        (-> (game/attack test-world-for-combat "attacker" "naked")
            (get-in [:people "naked" :stats :hp]))
        => 6)
  (fact "attack partly lowers HP of the armoured."
        (-> (game/attack test-world-for-combat "attacker" "armoured")
            (get-in [:people "armoured" :stats :hp]))
        => 9)
  (fact "attack doesn't change HP of the well-armoured."
        (-> (game/attack test-world-for-combat "attacker" "well-armoured")
            (get-in [:people "well-armoured" :stats :hp]))
        => 10))

(facts "attack-random-local-enemy"
  (fact "Attacks the enemy in the same location (and no-one else)."
        (as->
          worlds/empty-world $
          (util/add-with-id $ [:people] "x" (people/->Person
                                              nil
                                              (lib/->Person-stats 0 1 0 10)
                                              "blue"
                                              "bank"))
          (util/add-with-id $ [:people] "y" (people/->Person
                                              nil
                                              (lib/->Person-stats 0 1 0 10)
                                              "red"
                                              "bank"))
          (util/add-with-id $ [:people] "z" (people/->Person
                                              nil
                                              (lib/->Person-stats 0 1 0 10)
                                              "blue"
                                              "bank"))
          (util/add-with-id $ [:people] "t" (people/->Person
                                              nil
                                              (lib/->Person-stats 0 1 0 10)
                                              "red"
                                              "volcano"))
          (game/attack-random-local-enemy $ "x")
          (:people $)
          (vals $)
          (map :stats $)
          (map :hp $))
        => '(10 9 10 10)))
