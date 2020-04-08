(ns encogio.client.core
  (:require
   [encogio.client.io :as io]
   [encogio.client.ui :as ui]
   [rum.core :as rum]
   [cljsjs.clipboard]
   [goog.dom :as dom]
   [promesa.core :as p]))

(defonce app-state (atom ui/empty-state))

(defn start!
  []
  (rum/mount (ui/shortened-links app-state) (dom/getElement "shortened-links"))
  (rum/mount (ui/shorten-form app-state) (dom/getElement "shorten-form"))

  (when-let [short-urls (io/read-shortened-urls!)]
    (swap! app-state assoc :short-urls short-urls))

  (add-watch app-state
             :storage
             (fn [_ _ old new]
               (when-not (= (:short-urls old)
                            (:short-urls new))
                 (io/write-shortened-urls! (:short-urls new))))))

(start!)

