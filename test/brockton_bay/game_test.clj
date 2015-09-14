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
       (seq (:players (game/empty-world lib/locations)))
       => nil

       (:locations (game/empty-world lib/locations))
       => lib/locations

       (seq (:people (game/empty-world lib/locations)))
       => nil)

(facts "About add-player."
       (let [world (game/empty-world lib/locations)
             player (game/->Player (UUID/randomUUID), false, "foo", 0)]

         (-> (game/add-player world player)
             (:players)
             (count))
         => 1

         (-> (game/add-player world player)
             (game/add-player player)
             (:players)
             (count))
         => 2

         (-> (game/add-player world true "bleh")
             (:players)
             (count))
         => 1

         (-> (game/add-player world true "bleh")
             (game/add-player true "blob")
             (:players)
             (count))
         => 2

         (as-> (game/add-player world true "bleh") $
             (game/add-player $ true "blob")
             (:players $)
             (filter #(= (:faction %) "blob") $)
             (count $))
         => 1))

