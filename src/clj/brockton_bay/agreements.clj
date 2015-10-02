(ns brockton-bay.agreements)

;;; HACK: the map-in-a-map thing regularly causes bugs and is probably a design mistake.

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