(ns brockton-bay.ui-console
  (require [clojure.test :as test])
  (:use [seesaw.core])
  (:use brockton-bay.main))

(def game-title "Brockton Bay")

;(defrecord Option [message effect])

(defn display [frame content]
  (config! frame :content content)
  content)

(defn ask [frame message options]
  {:pre [(instance? javax.swing.JFrame frame)
         (map? options)
         (test/function? (val (first options)))]}
  (do
    (display frame message)
    (def lb (listbox :model (keys options)))
    (display frame (scrollable lb))
    (listen lb :selection
              (fn [option] ((get options (selection option)))))
    )
  )

(defn ask-freeform [frame message]
  {:pre [(instance? javax.swing.JFrame frame)]}
  (do
    ())
  )

(defn play-game []
  (do
    (native!)
    (def f (frame :title game-title))
    (-> f pack! show!))
  )

;Test stuff
(def sample-options {"option 1" #(println "picked 1")
                     "option 2" #(println "picked 2")
                     "option 3" #(println "picked 3")})
;(def sample-options
;  [(->Option
;     "option 1" #(println "picked 1"))
;   (->Option
;     "option 2" #(println "picked 2"))
;   (->Option
;     "option 3" #(println "picked 3"))]
;  )
