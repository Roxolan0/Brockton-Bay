(ns brockton-bay.game-test
  (:require [midje.sweet :refer :all]
            [brockton-bay.game :as game]))

(facts "About empty-world."
  (game/world? (game/empty-world))
  => true

  (seq (:locations (game/empty-world)))
  => nil)