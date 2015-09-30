(ns brockton-bay.worlds
  (:require [brockton-bay.util :as util]
            [brockton-bay.library :as lib]
            [brockton-bay.locations :as locations]
            [brockton-bay.agreements :as agreements]))

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

(defn get-people-at [world location-id]
  {:pre [(world? world)]}
  (filter
    #(= location-id (:location-id (val %)))
    (:people world)))

(defn get-people-without-location [world]
  {:pre [(world? world)]}
  (get-people-at world nil))

(defn get-dying-people [world]
  {:pre [(world? world)]}
  (filter
    #(>= 0 (get-in (val %) [:stats :hp]))
    (:people world)))

(defn get-players-ids-at [world location-id]
  {:pre [(world? world)]}
  (->>
    (get-people-at world location-id)
    (vals)
    (map :player-id)
    (distinct)))

(defn get-enemies-ids-near [world person-id]
  {:pre [(world? world)
         (contains? (:people world) person-id)]}
  (let [person (get-in world [:people person-id])]
    (as->
      world $
      (:people $)
      (filter
        #(= (:location-id person) (:location-id (val %))) $)
      (filter
        #(not= (:player-id person) (:player-id (val %))) $)
      (keys $))))

;; HACK: should be in generation.
(defn add-locations
  "Pick some random locations from library and add them to the world."
  [world nb-locations]
  {:pre [(world? world)
         (number? nb-locations)]}
  (->> lib/location-names
       (shuffle)
       (take nb-locations)
       (reduce #(util/add-with-id %1 [:locations] (locations/->Location %2 0 {})) world)))

(defn agreement? [world location-id player1-id player2-id]
  {:pre [(world? world)]}
  (->>
    (get-in world [:locations location-id :agreements])
    (vals)
    (map :choices-by-player-id)
    (filter #(util/contains-many? % player1-id player2-id))
    (empty?)
    (not)))

(defn is-at? [world location-id person-id]
  {:pre [(world? world)]}
  (= location-id (get-in world [:people person-id :location-id])))

(defn higher-speed? [world person1-id person2-id]
  {:pre [(world? world)]}
  (>
    (get-in world [:people person1-id :stats :speed])
    (get-in world [:people person2-id :stats :speed])))

(defn by-speed-decr [world]
  {:pre [(world? world)]}
  (comparator (partial higher-speed? world)))

#_(defn add-agreement [world location-id player1-id player2-id content]
    {:pre [(world? world)]}
    (util/add-with-id
      world
      [:locations location-id :agreements]
      (agreements/->Agreement [player1-id player2-id] content)))