(ns encogio.client.core
  (:require
   [encogio.client.ui :as ui]
   [encogio.i18n :as i18n]
   [rum.core :as rum]
   [goog.dom :as dom]))

(def tr (i18n/make-tr (i18n/get-languages)))
(defonce app-state (atom (ui/empty-state tr)))
(rum/hydrate (ui/shorten-form app-state tr) (dom/getElement "shorten-form"))


