(ns brockton-bay.players)

(defrecord Player
  [name
   ^boolean is-human
   cash])