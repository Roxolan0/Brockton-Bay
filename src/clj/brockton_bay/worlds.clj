(ns brockton-bay.worlds
  (:require [brockton-bay.util :as util]
            [brockton-bay.library :as lib]
            [brockton-bay.locations :as locations]))

(defrecord World
  [players
   locations
   people
   turn-count])

(defn world? [x] (instance? World x))

(def empty-world
  (->World {} {} {} 0))

(defn get-players-cash [world]
  {:pre [(world? world)]}
  (zipmap
    (map :name (vals (:players world)))
    (map :cash (vals (:players world))))
  )

(defn get-people-at-location [world location-id]
  {:pre [(world? world)]}
  (filter
    #(= location-id (:location-id (val %)))
    (:people world)))

(defn get-people-without-location [world]
  {:pre [(world? world)]}
  (get-people-at-location world nil))

(defn get-dying-people [world]
  {:pre [(world? world)]}
  (filter
    #(>= 0 (get-in (val %) [:stats :hp]))
    (:people world)))

;; HACK: should be in generation.
(defn add-locations
  "Pick some random locations from library and add them to the world."
  [world nb-locations]
  {:pre [(world? world)
         (number? nb-locations)]}
  (->> lib/location-names
       (shuffle)
       (take nb-locations)
       (reduce #(util/add-with-id %1 :locations (locations/->Location %2 0)) world)))
