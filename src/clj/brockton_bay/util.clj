(ns brockton-bay.util
  (:require [clojure.data :as data])
  (:import (java.util UUID)))

(defn dissoc-in
  ;From clojure/core.incubator. Will likely be in clojure core someday.
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as _keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn rand-in-range
  "Generates a random int between min and max (both included)."
  [min, max]
  {:pre  [(integer? min)
          (integer? max)
          (<= min max)]
   :post [(integer? %)
          (<= min % max)]}
  (+ min (rand-int (- (inc max) min))))

(defn same-keys
  "Finds all maps in 'source' that have the same values as the 'template' map for each of the 'keys'."
  [source template & keys]
  (filter #(= (select-keys template keys) (select-keys % keys)) source))

(defn different-keys
  ;; HACK : got frustrated on this one, definitely some better solutions.
  [source template & keys]
  "Finds all maps in 'source' that have NONE of the same values as the 'template' map for the 'keys'."
  (filter #(zero? (count (nth
                           (data/diff
                             (select-keys template keys)
                             (select-keys % keys))
                           2)))
          source))

(defn add-with-id
  "Add the to-add to the destination sub-map, indexed by an id (provided or generated)."
  ([destination submap-vector id to-add]
   (update-in destination submap-vector conj {id to-add}))
  ([destination submap-vector to-add]
   (add-with-id destination submap-vector (UUID/randomUUID) to-add)))

(defn add-many-with-id
  ; HACK: should be a multimethod combined with add-with-id.
  ([destination submap-vector ids to-add]
   (reduce
     (fn [a-destination pair]
       (add-with-id a-destination submap-vector (key pair) (val pair)))
     destination
     (zipmap ids to-add)))
  ([destination submap-vector to-add]
   (reduce
     (fn [a-destination a-to-add]
       (add-with-id a-destination submap-vector a-to-add))
     destination
     to-add)))

(defn contains-many?
  "Returns true if all keys are present in the given collection, otherwise returns false."
  [col & keys]
  (every? true? (map #(contains? col %) keys)))

(defn rand-no-repeat
  "Returns n random elements taken, without repeat, from col."
  [n col]
  {:pre [(number? n)
         (<= n (count col))]}
  (take n (shuffle col)))