(ns encogio.client.core
  (:require
   [rum.core :as rum]
   [cljsjs.clipboard]
   [goog.dom :as dom]
   [cljs.reader :as r]
   [goog.net.XhrIo :as xhr]))

;; io

(defn shorten!
  [state url]
  (swap! state assoc :ongoing-request true)
  (xhr/send "/api/shorten"
            (fn [event]
              ;; todo: error handling
              (let [resp (-> event .-target .getResponseText)
                    shortened (r/read-string resp)
                    short-urls (take 3 (conj (:short-urls @state) shortened))]
                (swap! state assoc
                       :url (:url shortened)
                       :short-urls short-urls
                       :ongoing-request false)))
            "POST"
            (pr-str {:url url})
            (clj->js {"Accept" "application/edn"
                      "Content-Type" "application/edn"})))

;; ui

(rum/defc url-input < rum/reactive
  [state]
  (let [{:keys [url ongoing-request]} (rum/react state)]
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
                   (shorten! state url)
                   #_(if displaying-shortened
                     (copy! state)
                     (shorten! state url)))}
      "Encoger"
      ]]))

(rum/defc copy-button
  < {:after-render (fn [state]
                     (println (pr-str state))
                     (let [[url] (:rum/args state)
                           button (rum/ref-node state "button")
                           clip (js/ClipboardJS. button (clj->js {:text (constantly url)}))]
                       (assoc state :clip clip)))
     :will-unmount (fn [state]
                     (let [clip (:clip state)]
                       (println :destroying-clip!)
                       (.destroy clip)
                       (dissoc state :clip)))}
  [url]
  [:button
   {:ref "button"
    :class "copy-button"}
   "Copiar"])

(rum/defc shortened-links < rum/reactive
  [state]
  [:ul
   (for [{:keys [url
                 short-url]} (:short-urls (rum/react state))]
     [:li {:key short-url}
      [:p url]
      [:a {:href short-url} short-url]
      (copy-button short-url)])])

;; app

(defonce app-state
  (atom {:url ""
         :short-urls []
         :ongoing-request false}))

(rum/mount (shortened-links app-state) (dom/getElement "shortened-links"))
(rum/mount (url-input app-state) (dom/getElement "shorten-form"))
