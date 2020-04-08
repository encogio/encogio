(ns encogio.client.ui
  (:require
   [rum.core :as rum]
   [promesa.core :as p]
   [encogio.client.io :as io]))

(def empty-state {:url ""
                  :alias ""
                  :error nil
                  :short-urls []
                  :ongoing-request false})

(def shorten-error? #{:invalid-url :server-error :rate-limit :network-error})
(def alias-error? #{:invalid-alias :used-alias})

(rum/defc shorten-form < rum/reactive
  [state]
  (let [{:keys [url
                alias
                error
                ongoing-request]} (rum/react state)]
    [:form
     [:.field.has-addons.has-addons-centered
      [:.control.is-expanded
       [:input
        {:type "text"
         :id "url"
         :class (if (shorten-error? error)
                  "input is-danger"
                  "input")
         :placeholder "Escribe aquí tu enlace para encogerlo"
         :disabled (if ongoing-request "disabled" "")
         :auto-focus true
         :value url
         :on-change (fn [ev]
                      (swap! state assoc :error nil :url (.-value (.-target ev))))}]
       (cond
         (= error :invalid-url)
         [:p.help.is-danger "URL no válida"]

         (= error :server-error)
         [:p.help.is-danger "Error en el servidor"]

         (= error :rate-limit)
         [:p.help.is-danger "Has encogido demasiadas URLs, prueba más tarde"]

         (= error :network-error)
         [:p.help.is-danger "No hemos podido contactar con el servidor"])]
      [:.control
       [:button.button.is-primary
        {:disabled (if ongoing-request "disabled" "")
         :on-click (fn [ev]
                     (.preventDefault ev)
                     (swap! state assoc :ongoing-request true)
                     (-> (if (not (empty? (clojure.string/trim alias)))
                           (io/alias! url alias)
                           (io/shorten! url))
                         (p/then
                          (fn [shortened]
                            (let [short-urls (take 3 (conj (:short-urls @state) shortened))]
                              (swap! state assoc
                                     :url (:url shortened) ;; todo: short url instead, copy interaction
                                     :error nil
                                     :short-urls short-urls
                                     :ongoing-request false))))
                         (p/catch (fn [err]
                                    (swap! state assoc
                                           :error err
                                           :ongoing-request false)))))}
        "Encoger"]]]
     [:.field.has-addons
      [:.control
       [:a {:class "button is-static"}
        "http://encog.io/"]]
      [:.control
       [:input.input
        {:type "text"
         :class (if (alias-error? error)
                  "input is-danger"
                  "input")
         :id "alias"
         :placeholder "Elige un alias (opcional)"
         :disabled (if ongoing-request "disabled" "")
         :value alias
         :on-change (fn [ev]
                      (swap! state assoc :alias (.-value (.-target ev))))}]
       (cond
         (= error :invalid-alias)
         [:p.help.is-danger "Alias no válido"]

         (= error :used-alias)
         [:p.help.is-danger "Alias en uso"])]]]))

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
                       ; todo: handle copy errors
                       #_(.on clip "error" (fn [result]
                                           (println :copy-error)))
                       (assoc state :clip clip)))
     :will-unmount (fn [state]
                     (let [clip (:clip state)]
                       (.off clip "success")
                       #_(.off clip "error")
                       (.destroy clip)
                       (dissoc state :clip)))}
  [state url]
  (let [copied? (rum/react (::copied? state))
        classes (if copied?
                  "button is-small is-light is-success"
                  "button is-small is-light")]
    [:button
     {:disabled (if copied? "disabled" "")
      :class classes
      :ref "button"}
     (if copied?
       [:div
        [:span.icon.is-small
         [:i.fas.fa-check]]
        [:span "Copiar"]]
       [:div
        [:span.icon.is-small
         [:i.fas.fa-copy]]
        [:span "Copiar"]])]))

(rum/defc shortened-links < rum/reactive
  [state]
  [:ul
   (for [{:keys [url
                 short-url]} (:short-urls (rum/react state))]
     [:li.level {:key short-url}
      [:.level-left [:p url]]
      [:.level-right
       [:a {:href short-url} short-url]
       (copy-button short-url)]])])
