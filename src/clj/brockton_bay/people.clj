(ns brockton-bay.people)

(defrecord Person
  [name
   stats
   player-id
   location-id]

  ;Object                                                    ; HACK: get rid of this when there's a proper GUI.
  ;(toString [this]
  ;  (pr this))
  )



#_(defn random-person-name [name-components]
    {:pre  [(seq name-components)]
     :post [(string? %)
            (seq %)]}
    (str
      (string/capitalize (rand-nth name-components))
      (rand-nth name-components)))