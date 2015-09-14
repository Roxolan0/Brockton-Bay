(ns brockton-bay.gui
  (require [clojure.test :as test]
           [seesaw.core :refer :all]
           [brockton-bay.game :as game]
           [brockton-bay.library :as lib]))

;;; English strings.

(def game-title "Brockton Bay")

;;; Flexible GUI functions.

(defn frame? [x] (instance? javax.swing.JFrame x))

(defn display [frame content]
  (config! frame :content content)
  (pack! frame)
  content)

(defn ask
  ([frame message]
   {:pre [(frame? frame)]}
   (input frame message))
  ([frame message options]
   {:pre [(frame? frame)
          (seq options)]}                                    ;false if options is nil or empty
   (input frame message :choices options))
  )

;;; Game-specific GUI functions

(defn ask-new-player [world frame player-number]
  {:pre [(frame? frame)]}
  (->>
    (ask frame (str "Player " player-number ", what do you want to call your faction?"))
    (game/add-player world true))
  )

(defn -main []
  (let [f (frame :title game-title)
        world (game/empty-world lib/locations)]
    (native!)
    (-> f pack! show!)
    (display f "PLACEHOLDER LOADING MESSAGE")
    (->>
      (ask f "How many human players?")
      (Integer/parseInt)
      (inc)
      (range 1)
      (map (partial ask-new-player world f)))))

;;; Test stuff, remove before selling code for $100k

(def sample-options ["option 1" "option 2" "option 3"])
