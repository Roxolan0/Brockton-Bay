(ns brockton-bay.main_test
  (:require [midje.sweet :refer :all]
            [brockton-bay.main :as main]
            [brockton-bay.library :as lib])
  (:import (java.util UUID)))

(facts "About same-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (main/same-keys template source :b)
         => '({:a 1 :b 2 :c 4} {:a 4 :b 2 :c 3})

         (main/same-keys template source :b :c)
         => '({:a 4 :b 2 :c 3})

         (main/same-keys template source :a :b :c)
         => '()

         (main/same-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))

(facts "About different-keys."
       (let [template {:a 1 :b 2 :c 3}
             source [{:a 2 :b 1 :c 3}
                     {:a 1 :b 2 :c 4}
                     {:a 4 :b 2 :c 3}]]

         (main/different-keys template source :a)
         => '({:a 2 :b 1 :c 3} {:a 4 :b 2 :c 3})

         (main/different-keys template source :a :b)
         => '({:a 2 :b 1 :c 3})

         (main/different-keys template source :a :b :c)
         => '()

         (main/different-keys template source nil)
         => '({:a 2 :b 1 :c 3}
               {:a 1 :b 2 :c 4}
               {:a 4 :b 2 :c 3})))

(facts "About empty-world."
       (seq (:players (main/empty-world lib/locations)))
       => nil

       (:locations (main/empty-world lib/locations))
       => lib/locations

       (seq (:people (main/empty-world lib/locations)))
       => nil)

(facts "About add-player."
       (let [world (main/empty-world lib/locations)
             player (main/->Player (UUID/randomUUID), false, "foo", 0)]

         (-> (main/add-player world player)
             (:players)
             (count))
         => 1

         (-> (main/add-player world player)
             (main/add-player player)
             (:players)
             (count))
         => 2

         (-> (main/add-player world true "bleh")
             (:players)
             (count))
         => 1

         (-> (main/add-player world true "bleh")
             (main/add-player true "blob")
             (:players)
             (count))
         => 2

         (as-> (main/add-player world true "bleh") $
             (main/add-player $ true "blob")
             (:players $)
             (filter #(= (:faction %) "blob") $)
             (count $))
         => 1))

