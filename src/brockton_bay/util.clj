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
  "Add the source to the destination sub-map, indexed by an id (provided or generated)."
  ([destination submap id source]
   (update-in destination [submap] conj {id source}))
  ([destination submap source]
   (add-with-id destination submap (UUID/randomUUID) source)))
