(ns brockton-bay.util-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.util :as util]))

(facts "About same-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (util/same-keys template source :b)
         => '({:a 1 :b 2 :c 4} {:a 4 :b 2 :c 3})

         (util/same-keys template source :b :c)
         => '({:a 4 :b 2 :c 3})

         (util/same-keys template source :a :b :c)
         => '()

         (util/same-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))

(facts "About different-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (util/different-keys template source :a)
         => '({:a 2 :b 1 :c 3} {:a 4 :b 2 :c 3})

         (util/different-keys template source :a :b)
         => '({:a 2 :b 1 :c 3})

         (util/different-keys template source :a :b :c)
         => '()

         (util/different-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))


(facts "About add-with-id."
       (let [original {:players {}}
             player1 {:name "Player 1"}
             player2 {:name "Player 2"}]

         (util/add-with-id original :players "some-id" player1)
         => {:players {"some-id" player1}}

         (-> (util/add-with-id original :players "player-1-id" player1)
             (util/add-with-id :players "player-2-id" player2))
         => {:players {"player-1-id" player1
                       "player-2-id" player2}}))