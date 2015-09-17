(ns brockton-bay.game-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.game :as game]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.generation :as generation]
            [brockton-bay.util :as util]
            [brockton-bay.people :as people]
            [brockton-bay.library :as lib]))

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
          (generation/add-templates $ 5 "Bob")
          (assoc-in $ [:people (first (keys (:people $))) :stats :hp] 0)
          (assoc-in $ [:people (last (keys (:people $))) :stats :hp] 0)
          (game/clean-dead $)
          (:people $)
          (count $)
          )
        => 3))

(facts "inflict"
  (fact "Lowers HP of the unarmoured."
        (as-> worlds/empty-world $
              (util/add-with-id $ :people "x" (people/->Person
                                            nil
                                            (lib/->Person-stats 0 0 0 10)
                                            nil
                                            nil))
              (game/inflict $ 4 "x")
              (get-in $ [:people "x" :stats :hp]))
        => 6)
  (fact "Partly lowers HP of the armoured."
                   (as-> worlds/empty-world $
                         (util/add-with-id $ :people "x" (people/->Person
                                                           nil
                                                           (lib/->Person-stats 0 0 3 10)
                                                           nil
                                                           nil))
                         (game/inflict $ 4 "x")
                         (get-in $ [:people "x" :stats :hp]))
                   => 9)
  (fact "Doesn't change HP of the well-armoured."
        (as-> worlds/empty-world $
              (util/add-with-id $ :people "x" (people/->Person
                                                nil
                                                (lib/->Person-stats 0 0 10 10)
                                                nil
                                                nil))
              (game/inflict $ 4 "x")
              (get-in $ [:people "x" :stats :hp]))
        => 10)
  )

(facts "attack-random-local-enemy"
  (fact "Attacks the enemy in the same location (and no-one else)."
        (as-> worlds/empty-world $
              (util/add-with-id $ :people "x" (people/->Person
                                                nil
                                                (lib/->Person-stats 0 1 0 10)
                                                "blue"
                                                "bank"))
              (util/add-with-id $ :people "y" (people/->Person
                                                nil
                                                (lib/->Person-stats 0 1 0 10)
                                                "red"
                                                "bank"))
              (util/add-with-id $ :people "z" (people/->Person
                                                nil
                                                (lib/->Person-stats 0 1 0 10)
                                                "blue"
                                                "bank"))
              (util/add-with-id $ :people "t" (people/->Person
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

;(facts "combat-turn"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "combat-phase"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "change-location"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "clear-people-locations"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "give-money"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "give-money-via-person"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "split-payoff"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
;
;(facts "split-payoffs"
;  (fact "placeholder"
;        (identity 1)
;        => 1))
