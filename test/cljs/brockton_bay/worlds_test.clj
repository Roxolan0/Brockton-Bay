(ns brockton-bay.worlds-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.people :as people]
            [brockton-bay.library :as lib]
            [brockton-bay.util :as util]
            [brockton-bay.locations :as locations]))

(def test-world
  (as->
    worlds/empty-world $
    (util/add-with-id $ [:people] "x" (people/->Person
                                        "Alice"
                                        (lib/->Person-stats 0 1 0 10)
                                        "blue"
                                        "bank"))
    (util/add-with-id $ [:people] "y" (people/->Person
                                        "Bob"
                                        (lib/->Person-stats 0 1 0 10)
                                        "red"
                                        "bank"))
    (util/add-with-id $ [:people] "z" (people/->Person
                                        "Carol"
                                        (lib/->Person-stats 0 1 0 10)
                                        "blue"
                                        "bank"))
    (util/add-with-id $ [:people] "t" (people/->Person
                                        "Dave"
                                        (lib/->Person-stats 0 1 0 10)
                                        "green"
                                        "volcano"))))

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
          (worlds/get-people-at-location test-world "bank")
          (keys)
          (flatten))
        => '("x" "y" "z"))

  (fact "Find each person only once."
        (->
          (worlds/get-people-at-location test-world "bank")
          (count))
        => 3))

(facts "About get-players-ids-at-location."
  (fact "Finds all players with people at the location (and no-one else)."
        (->
          (worlds/get-players-ids-at-location test-world "bank")
          (flatten))
        => '("blue" "red"))

  (fact "Finds each player only once."
        (->
          (worlds/get-players-ids-at-location test-world "bank")
          (count))
        => 2))
