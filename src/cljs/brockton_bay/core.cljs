(ns brockton-bay.core
  (:require
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [brockton-bay.game :as game]
            [brockton-bay.library :as lib]
            [brockton-bay.worlds :as worlds]
            [brockton-bay.generation :as generation]))

(enable-console-print!)

(defonce app-state (atom {:text "PLACEHOLDER LOADING MESSAGE"}))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/h1 nil (:text app)))))
    app-state
    {:target (. js/document (getElementById "app"))}))
