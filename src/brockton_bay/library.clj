(ns brockton-bay.library
  (:import (java.util UUID)))

;;; Game constants

(def people-per-faction 6)
(def starting-cash 0)
(def nb-locations 4)
(def nb-turns)

;;; Game defrecords (here to avoid circular dependencies)

(defrecord Person-stats
  [^long speed
   ^long damage
   ^long armour
   ^long hp])

;;; Predefined game elements

(def location-names
  ["Drug trade"
   "Bank robbery"
   "Mercenary work"
   "Kidnapping"])
;(map (partial apply ->Location)
;     [[(UUID/randomUUID) "Drug trade" 0]
;      [(UUID/randomUUID) "Bank robbery" 0]
;      [(UUID/randomUUID) "Mercenary work" 0]
;      [(UUID/randomUUID) "Kidnapping" 0]]))

(def ai-names
  ["The Azian Bad Boys"
   "The Pure"
   "Fenrir's Chosen"
   "The Merchants"
   "The Travelers"
   "The Undersiders"
   "The Protectorate"
   "New Wave"
   "The Ambassadors"
   "The Fallen"
   "The Teeth"
   "The Adepts"])

(def person-name-components
  ["angel" "demon" "beast" "monster"
   "fire" "ice"
   "arrow" "knife" "rainbow"
   "eye" "muscle" "skull" "bone" "blood"
   "death" "power"
   "dark" "light"
   "twilight" "dawn"
   "wise" "strong" "cold"
   "killer" "hunter" "stomper"])

(def people-templates
  (zipmap
    ["Average Lad"
     "Lightning Bruiser"
     "Glass Cannon"
     "Runner"
     "Tortoise"
     "Tank"
     "Beater"
     "Survivor"]
    (map (partial apply ->Person-stats)
         ['(5 3 1 5)                                        ;Average Lad
          '(8 4 0 4)                                        ;Lightning Bruiser
          '(0 5 0 8)                                        ;Glass Cannon
          '(10 1 1 5)                                       ;Runner
          '(4 3 3 3)                                        ;Tortoise
          '(1 2 2 8)                                        ;Tank
          '(3 4 2 7)                                        ;Beater
          '(6 2 1 8)                                        ;Survivor
          ])))
