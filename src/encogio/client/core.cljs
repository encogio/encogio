(ns encogio.client.core
  (:require
   [encogio.client.io :as io]
   [rum.core :as rum]
   [cljsjs.clipboard]
   [goog.dom :as dom]
   [promesa.core :as p]))

(rum/defc url-input < rum/reactive
  [state]
  (let [{:keys [url
                error
                ongoing-request]} (rum/react state)]
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
                   (swap! state assoc :ongoing-request true)
                   (-> (io/shorten! url)
                       (p/then
                        (fn [shortened]
                          (let [short-urls (take 3 (conj (:short-urls @state) shortened))]
                            (swap! state assoc
                                   :url (:url shortened)
                                   :error nil
                                   :short-urls short-urls
                                   :ongoing-request false))))
                       (p/catch (fn [err]
                                  (swap! state assoc
                                         :error err
                                         :ongoing-request false)))))}
      "Encoger"]]))

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

(rum/defc error-message < rum/reactive
  [state]
  (let [{:keys [error]} (rum/react state)]
    (when error
      [:p (pr-str error)])))

;; app

(defonce app-state
  (atom {:url ""
         :error nil
         :short-urls []
         :ongoing-request false}))

(defn mount!
  []
  (rum/mount (error-message app-state) (dom/getElement "error-message"))
  (rum/mount (shortened-links app-state) (dom/getElement "shortened-links"))
  (rum/mount (url-input app-state) (dom/getElement "shorten-form")))

(mount!)
