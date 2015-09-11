(ns brockton-bay.ui-console
  (:use brockton-bay.main))

(defrecord Option [message effect])

(defn ask [message options]
  {:pre [(instance? clojure.lang.PersistentArrayMap options)
         (not-empty options)]}
  (let [numbered-options (zipmap (range (count options)) options)]
    (println message)
    (map
      #(println (str "- " (:message (second %)) " (" (first %) ")"))
      numbered-options)
    (->
      "0"                     ;TODO should be (readline)
      (. Integer parseInt)
      (find numbered-options)
      (:effect)
      )))

(defrecord play-game []

  )
