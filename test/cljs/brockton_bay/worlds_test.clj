(ns brockton-bay.worlds-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.people :as people]
            [brockton-bay.library :as lib]
            [brockton-bay.util :as util]
            [brockton-bay.locations :as locations]
            [brockton-bay.agreements :as agreements]))

(def test-world-with-people
  (->
    worlds/empty-world
    (util/add-with-id [:people] "x" (people/->Person
                                      "Alice"
                                      (lib/->Person-stats 1 1 0 10)
                                      "blue"
                                      "bank"))
    (util/add-with-id [:people] "y" (people/->Person
                                      "Bob"
                                      (lib/->Person-stats 3 1 0 10)
                                      "red"
                                      "bank"))
    (util/add-with-id [:people] "z" (people/->Person
                                      "Carol"
                                      (lib/->Person-stats 2 1 0 10)
                                      "blue"
                                      "bank"))
    (util/add-with-id [:people] "t" (people/->Person
                                      "Dave"
                                      (lib/->Person-stats 4 1 0 10)
                                      "green"
                                      "volcano"))))

(def test-world-with-agreements
  (->
    worlds/empty-world
    (util/add-with-id [:locations] "bank" (locations/->Location nil 0 {}))
    (util/add-with-id [:locations] "volcano" (locations/->Location nil 0 {}))
    (util/add-with-id [:locations] "cemetary" (locations/->Location nil 0 {}))
    (util/add-with-id [:locations] "space" (locations/->Location nil 0 {}))
    (util/add-with-id [:locations "bank" :agreements]
                      (agreements/->Agreement {"red" :flee "blue" :share}))
    (util/add-with-id [:locations "bank" :agreements]
                      (agreements/->Agreement {"red" :share "yellow" :share}))
    (util/add-with-id [:locations "volcano" :agreements]
                      (agreements/->Agreement {"red" :share "yellow" :flee}))
    (util/add-with-id [:locations "volcano" :agreements]
                      (agreements/->Agreement {"blue" :flee "yellow" :flee}))
    (util/add-with-id [:locations "cemetary" :agreements]
                      (agreements/->Agreement {"red" :flee "blue" :flee}))
    (util/add-with-id [:people] "x" (people/->Person
                                      "Alice"
                                      (lib/->Person-stats 1 1 0 10)
                                      "red"
                                      "bank"))
    (util/add-with-id [:people] "y" (people/->Person
                                      "Bob"
                                      (lib/->Person-stats 1 1 0 10)
                                      "red"
                                      "volcano"))
    (util/add-with-id [:people] "z" (people/->Person
                                      "Carol"
                                      (lib/->Person-stats 1 1 0 10)
                                      "yellow"
                                      "bank"))
    (util/add-with-id [:people] "t" (people/->Person
                                      "Dave"
                                      (lib/->Person-stats 1 1 0 10)
                                      "blue"
                                      "bank"))))

(facts "About empty-world."
  (fact "It's a World."
        (worlds/world? worlds/empty-world)
        => true)

  (fact "It's empty."
        (seq (:locations worlds/empty-world))
        => nil))

(facts "About get-people-at-location."
  (fact "Finds all people at the location (and no-one else)."
        (->>
          (worlds/get-people-at test-world-with-people "bank")
          (keys)
          (flatten))
        => '("x" "y" "z"))

  (fact "Find each person only once."
        (->
          (worlds/get-people-at test-world-with-people "bank")
          (count))
        => 3))

(facts "About get-players-ids-at-location."
  (fact "Finds all players with people at the location (and no-one else)."
        (->
          (worlds/get-players-ids-at test-world-with-people "bank")
          (flatten))
        => '("blue" "red"))

  (fact "Finds each player only once."
        (->
          (worlds/get-players-ids-at test-world-with-people "bank")
          (count))
        => 2))

(facts "About agreement?."
  (fact "agreement? returns true if there is an agreement between the two players at that location."
        (worlds/agreement? test-world-with-agreements "bank" "blue" "red")
        => true)
  (fact "agreement? returns false if there isn't an agreement between the two players at that location."
        (worlds/agreement? test-world-with-agreements "volcano" "blue" "red")
        => false)
  (fact "agreement? returns false on a location with no agreements."
        (worlds/agreement? test-world-with-agreements "space" "blue" "red")
        => false))

(facts "About fleeing?."
  (fact "fleeing? returns true if that person's player has at least one :flee agreement at that person's location."
        (worlds/fleeing? test-world-with-agreements "x")
        => true)
  (fact "fleeing? returns true if that person's player has no :flee agreement at that person's location."
        (worlds/fleeing? test-world-with-agreements "y")
        => false))

(facts "About sharing?."
  (fact "sharing? returns true if both players have at least one :share agreement at those people's location."
        (worlds/sharing? test-world-with-agreements "x" "z")
        => true)
  (fact "sharing? returns false if those players don't have a :share agreement at those people's location."
        (worlds/sharing? test-world-with-agreements "x" "t")
        => false))