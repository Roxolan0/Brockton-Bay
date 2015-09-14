(ns brockton-bay.gui
  (require [clojure.test :as test])
  (:use [seesaw.core])
  (:use brockton-bay.main)
  (:use brockton-bay.library))

(def game-title "Brockton Bay")

;(defrecord Option [message effect])

(defn display [frame content]
  (config! frame :content content)
  (pack! frame)
  content)

(defn ask
  ([frame message]
   {:pre [(instance? javax.swing.JFrame frame)]}
   (input frame message))
  ([frame message options]
   {:pre [(instance? javax.swing.JFrame frame)
          (seq options)]}                                    ;false if options is nil or empty
   (input frame message :choices options))
  )

(defn ask-new-player [world frame player-number]
  {:pre [(instance? javax.swing.JFrame frame)]}
  (->>
    (ask frame (str "Player " player-number ", what do you want to call your faction?"))
    (add-player world true))
  )

(defn play-game []
  (do
    (native!)
    (def f (frame :title game-title))
    (-> f pack! show!)
    (display f "PLACEHOLDER LOADING MESSAGE")
    (def world (empty-world default-locations))

    (->>
      (ask f "How many human players?")
      (. Integer parseInt)
      (+ 1)
      (range 1)
      (map (partial ask-new-player world f))
      )
    )
  )

;Test stuff
(def sample-options ["option 1" "option 2" "option 3"])
;(def sample-options {"option 1" #(println "picked 1")
;                     "option 2" #(println "picked 2")
;                     "option 3" #(println "picked 3")})
;(def sample-options
;  [(->Option
;     "option 1" #(println "picked 1"))
;   (->Option
;     "option 2" #(println "picked 2"))
;   (->Option
;     "option 3" #(println "picked 3"))]
;  )
