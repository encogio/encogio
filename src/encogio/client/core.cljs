(ns encogio.client.core
  (:require
   [rum.core :as rum]
   [goog.dom :as dom]
   [goog.net.XhrIo :as xhr]))

;; io

(defn shorten!
  [state url]
  (swap! state assoc :ongoing-request true)
  (xhr/send "/api/shorten"
            (fn [event]
              ;; todo: error handling
              (let [short (-> event .-target .getResponseText)
                    short-urls (conj (:short-urls @state) {:url url
                                                           :short-url short})]
                (println :req-recv short-urls)
                (swap! state assoc
                       :url ""
                       :short-urls short-urls
                       :ongoing-request false)))
            "POST"
            url
            (clj->js {"Content-Type" "text/plain"})))

;; ui

(rum/defc url-input < rum/reactive
  [state]
  (let [{:keys [url ongoing-request]} (rum/react state)]
    (println :ongoing? ongoing-request)
    [:form
     [:input {:type "text"
              :id "url"
              :disabled (if ongoing-request "disabled" "")
              :auto-focus true
              :value url
              :on-change (fn [ev]
                           (swap! state assoc :url (.-value (.-target ev))))}]
     [:button
      {:disabled (if ongoing-request "disabled" "")       
       :on-click (fn [ev]
                   (.preventDefault ev)
                   (shorten! state url))}
      "Encoger"]]))

(rum/defc shortened-links < rum/reactive
  [state]
  [:ul
   (for [{:keys [url
                 short-url]} (:short-urls (rum/react state))]
     [:li {:key short-url}
      [:p url]
      [:p short-url]])])

;; app

(defonce app-state
  (atom {:url ""
         :short-urls []
         :ongoing-request false}))

(rum/mount (shortened-links app-state) (dom/getElement "shortened-links"))
(rum/mount (url-input app-state) (dom/getElement "shorten-form"))
