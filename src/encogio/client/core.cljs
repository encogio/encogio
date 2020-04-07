(ns encogio.client.core
  (:require
   [rum.core :as rum]
   [cljsjs.clipboard]
   [goog.dom :as dom]
   [cljs.reader :as r]
   [goog.net.XhrIo :as xhr]))

;; io

;; todo: http error handling, timeout, offline
(defn shorten!
  [state url]
  (swap! state assoc :ongoing-request true)
  (xhr/send "/api/shorten"
            (fn [event]
              (let [response (-> event .-target)
                    status (.getStatus response)]
                (case status
                  200
                  (let [body (.getResponseJson response)
                        shortened {:url (.-url body)
                                   :short-url (aget body "short-url")}
                        short-urls (take 3 (conj (:short-urls @state) shortened))]
                    (swap! state assoc
                           :url (:url shortened)
                           :short-urls short-urls
                           :ongoing-request false))

                  500 (do (println :server-error))

                  400 (do (println :invalid-url))

                  429 (do (println :rate-limit))

                  (do
                    (println :unknown-error)))))
            "POST"
            (js/JSON.stringify #js {:url url})
            (clj->js {"Accept" "application/json"
                      "Content-Type" "application/json"})))

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
                   (shorten! state url))}
      "Encoger"
      ]]))

(rum/defcs copy-button
  < (rum/local false ::copied?)
    rum/reactive
    {:after-render (fn [state]
                     (let [[url] (:rum/args state)
                           copied? (::copied? state)
                           button (rum/ref-node state "button")
                           clip (js/ClipboardJS. button
                                                 #js {:text (constantly url)})]
                       (.on clip "success" (fn [result]
                                             (reset! copied? true)
                                             (js/setTimeout #(reset! copied? false) 2000)))
                       (.on clip "error" (fn [result]
                                           (println :copy-error)))
                       (assoc state :clip clip)))
     :will-unmount (fn [state]
                     (let [clip (:clip state)]
                       (.off clip "success")
                       (.off clip "error")
                       (.destroy clip)
                       (dissoc state :clip)))}
  [state url]
  (let [copied? (rum/react (::copied? state))]
    [:button
     {:disabled (if copied? "disabled" "")
      :ref "button"
      :class "copy-button"}
     (if copied?
       "Copiado!"
       "Copiar")]))

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

(defn mount!
  []
  (rum/mount (shortened-links app-state) (dom/getElement "shortened-links"))
  (rum/mount (url-input app-state) (dom/getElement "shorten-form")))

(mount!)
