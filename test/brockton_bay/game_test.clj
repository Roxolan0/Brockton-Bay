(ns brockton-bay.game_test
  (:require [midje.sweet :refer :all]
            [brockton-bay.game :as game]
            [brockton-bay.library :as lib])
  (:import (java.util UUID)))

(facts "About same-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (game/same-keys template source :b)
         => '({:a 1 :b 2 :c 4} {:a 4 :b 2 :c 3})

         (game/same-keys template source :b :c)
         => '({:a 4 :b 2 :c 3})

         (game/same-keys template source :a :b :c)
         => '()

         (game/same-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))

(facts "About different-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (game/different-keys template source :a)
         => '({:a 2 :b 1 :c 3} {:a 4 :b 2 :c 3})

         (game/different-keys template source :a :b)
         => '({:a 2 :b 1 :c 3})

         (game/different-keys template source :a :b :c)
         => '()

         (game/different-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))

(facts "About empty-world."
       (game/world? (game/empty-world))
       => true

       (seq (:locations (game/empty-world)))
       => nil)

(facts "About add-with-id."
       (let [world (game/empty-world)
             player1 (game/->Player "foo", false, 0)
             player2 (game/->Player "bar", false, 0)]

         (-> (game/add-with-id world :players player1)
             (game/world?))
         => true

         (-> (game/add-with-id world :players player1)
             (:players)
             (count))
         => 1

         (-> (game/add-with-id world :players player1)
             (game/add-with-id :players player2)
             (game/world?))
         => true

         (-> (game/add-with-id world :players player1)
             (game/add-with-id :players player2)
             (:players)
             (count))
         => 2))