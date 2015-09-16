(ns brockton-bay.worlds-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.worlds :as worlds]))

(facts "About empty-world."
  (worlds/world? (worlds/empty-world))
  => true

  (seq (:locations (worlds/empty-world)))
  => nil)