(ns brockton-bay.main-test
                (:require [midje.sweet :refer :all]
                          [brockton-bay.main :refer :all]
                          [brockton-bay.library :refer :all]
                          [clojure.java.io :as io]))

(fact "We can create an empty World."
      (empty-world default-locations)
      => true)