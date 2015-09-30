(ns brockton-bay.util-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.util :as util]))

(facts "same-keys"
  (let [template {:a 1 :b 2 :c 3}
        source [{:a 2 :b 1 :c 3}
                {:a 1 :b 2 :c 4}
                {:a 4 :b 2 :c 3}]]

    (fact "Finds only the two entries with that key in common."
          (util/same-keys source template :b)
          => '({:a 1 :b 2 :c 4} {:a 4 :b 2 :c 3}))

    (fact "Finds only the one entry with that key in common."
          (util/same-keys source template :b :c)
          => '({:a 4 :b 2 :c 3}))

    (fact "Finds no entry that has all those keys in common."
          (util/same-keys source template :a :b :c)
          => '())

    (fact "Without a filtering criteria, finds all entries."
          (util/same-keys source template nil)
          => '({:a 2 :b 1 :c 3}
                {:a 1 :b 2 :c 4}
                {:a 4 :b 2 :c 3}))))

(facts "different-keys."
  (let [template {:a 1 :b 2 :c 3}
        source [{:a 2 :b 1 :c 3}
                {:a 1 :b 2 :c 4}
                {:a 4 :b 2 :c 3}]]

    (util/different-keys source template :a)
    => '({:a 2 :b 1 :c 3} {:a 4 :b 2 :c 3})

    (util/different-keys source template :a :b)
    => '({:a 2 :b 1 :c 3})

    (util/different-keys source template :a :b :c)
    => '()

    (util/different-keys source template nil)
    => '({:a 2 :b 1 :c 3}
          {:a 1 :b 2 :c 4}
          {:a 4 :b 2 :c 3})))


(facts "About add-with-id."
  (let [original {:players {}}
        player1 {:name "Player 1"}
        player2 {:name "Player 2"}]

    (util/add-with-id original [:players] "some-id" player1)
    => {:players {"some-id" player1}}

    (-> (util/add-with-id original [:players] "player-1-id" player1)
        (util/add-with-id [:players] "player-2-id" player2))
    => {:players {"player-1-id" player1
                  "player-2-id" player2}}))

(def test-map {:a 0 :b 1 :c 2 :d 3})

(facts "About contains-many?."
  (fact "contains-many? returns true if all keys are present in the given collection."
        (util/contains-many? test-map :a :b :c)
        => true)
  (fact "contains-many? returns false if one of the keys is missing from the given collection."
        (util/contains-many? test-map :a :b :missing-key))
  (fact "contains-many? returns true if no key is required."
        (util/contains-many? test-map)
        => true))