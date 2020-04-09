(ns encogio.client.ui
  (:require
   [rum.core :as rum]
   [promesa.core :as p]
   [clojure.spec.alpha :as s]
   [encogio.client.io :as io]))

(def empty-state {:url ""
                  :alias ""
                  :input-state :normal
                  :short-urls []})

(s/def ::state (s/keys ::req-un [::url
                                 ::alias
                                 ::input-state]
                       ::opt-un [::error]))

(s/def ::input-state #{:normal :waiting :copy :error})
(s/def ::error #{:invalid-url :invalid-alias :used-alias :server-error :rate-limit :network-error :forbidden-domain})
(s/def ::alias string?)
(s/def ::url string?)

(def shorten-error? #{:invalid-url :server-error :rate-limit :network-error})
(def alias-error? #{:invalid-alias :used-alias})

(def copy-mixin
  {:after-render
   (fn [state]
     (let [[text] (:rum/args state)
           copied? (get state ::copied?)
           button (rum/ref-node state "button")
           clip (js/ClipboardJS. button
                                 #js {:text (constantly text)})]
       (.on clip "success" (fn [result]
                             (reset! copied? true)
                             (js/setTimeout #(reset! copied? false) 2000)))
       (assoc state :clip clip)))
   
   :will-unmount
   (fn [state]
     (let [clip (:clip state)]
       (.off clip "success")
       (.destroy clip)
       (dissoc state :clip)))})

(rum/defcs url-copy-button < (rum/local false ::copied?)  rum/reactive copy-mixin
  
  [local url state]
  (let [copied? (rum/react (::copied? local))]
    [:button
     {:class "button is-light is-success"
      :ref "button"
      :on-click (fn [ev]
                  (.preventDefault ev)
                  (swap! state assoc
                         :input-state :normal
                         :url ""
                         :short-url ""))}
     [:div
      [:span.icon.is-small
       [:i.fas.fa-copy]]
      [:span "Copiar"]]]))

(rum/defc url-input-error < rum/static
  [error]
  (cond
    (= error :invalid-url)
    [:p.help.is-danger "URL no válida"]

    (= error :forbidden-domain)
    [:p.help.is-danger "Las URLs de ese dominio no están permitidas"]

    (= error :server-error)
    [:p.help.is-danger "Error en el servidor"]

    (= error :rate-limit)
    [:p.help.is-danger "Has encogido demasiadas URLs, prueba más tarde"]

    (= error :network-error)
    [:p.help.is-danger "No hemos podido contactar con el servidor"]))



(defn shorten-url!
  [url alias short-urls state]
  (-> (if (not (empty? (clojure.string/trim alias)))
        (io/alias! url alias)
        (io/shorten! url))
      (p/then
       (fn [shortened]
         (let [short-urls (conj (take 2 short-urls) shortened)]
           (swap! state assoc
                  :url (:url shortened)
                  :short-url (:short-url shortened)
                  :alias ""
                  :short-urls short-urls
                  :input-state :copy))))
      (p/catch (fn [err]
                 (swap! state assoc
                        :error err
                        :input-state :error)))))

(rum/defc url-input < rum/reactive
  [state]
  (let [{:keys [input-state
                url
                short-url
                alias
                error
                short-urls]} (rum/react state)]
    [:.field.has-addons.has-addons-centered
     [:.control.is-expanded
      [:input
       {:type "text"
        :id "url"
        :class (cond
                 (= input-state :copy) "input is-success"
                 (shorten-error? error)  "input is-danger"
                 :else "input")
        :placeholder "Escribe aquí tu enlace para encogerlo"
        :disabled (case input-state
                    :waiting "disabled"
                    "")
        :value (case input-state
                 :copy short-url
                 url)
        :on-change (fn [ev]
                     (swap! state assoc
                            :input-state :normal
                            :url (.-value (.-target ev))))}]]
     [:.control
      (cond
        (= input-state :copy)
        (url-copy-button short-url state)
        
        :else
        [:button.button.is-primary
         {:disabled (case input-state
                      :waiting "disabled"
                      "")
          :on-click (fn [ev]
                      (.preventDefault ev)
                      (swap! state assoc :input-state :waiting)
                      (shorten-url! url alias short-urls state))}
         [:div
          [:span.icon.is-small
           [:i.fas.fa-compress-arrows-alt]]
          [:span "Cortar"]]])]]))

(rum/defc alias-input < rum/reactive
  [state]
  (let [{:keys [error
                alias
                input-state]} (rum/react state)]
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
        :disabled (case input-state
                    :waiting "disabled"
                    "")
        :value alias
        :on-change (fn [ev]
                     (swap! state assoc :alias (.-value (.-target ev))))}]
      (cond
        (= error :invalid-alias)
        [:p.help.is-danger "Alias no válido"]

        (= error :used-alias)
        [:p.help.is-danger "Alias en uso"])]]))

(rum/defc shorten-form
  [state]
  [:form
   (url-input state)
   (alias-input state)])

(rum/defcs copy-button < (rum/local false ::copied?)  rum/reactive copy-mixin
  
  [state url]
  (let [copied? (rum/react (::copied? state))
        classes (if copied?
                  "button is-small is-light is-success"
                  "button is-small is-light")]
    [:button
     {:class classes
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
