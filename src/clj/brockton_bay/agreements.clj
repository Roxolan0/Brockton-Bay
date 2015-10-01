(ns brockton-bay.agreements)

(defrecord Agreement
  [choices-by-player-id])

(defn agreement-options
  ([]
    [:share :fight :flee])
  ([other-player-choice]
    (case other-player-choice
      :share [:share :fight :flee]
      :fight [:fight :flee]
      :flee [:fight])))