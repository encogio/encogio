(ns encogio.client.core
  (:require
   [encogio.client.ui :as ui]
   [rum.core :as rum]
   [goog.dom :as dom]))

(defonce app-state (atom ui/empty-state))

(defn start!
  []
  (rum/mount (ui/shorten-form app-state) (dom/getElement "shorten-form")))

(start!)

