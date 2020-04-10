(ns encogio.client.ui
  (:require
   [rum.core :as rum]
   [cljsjs.clipboard]
   [clojure.string :refer [trim blank?]]
   [promesa.core :as p]
   [clojure.spec.alpha :as s]
   [encogio.client.io :as io]))

(def greeting
  [:p
   "Si te interesa este proyecto puedes colaborar "
   [:a {:href "http://encog.io/code"} "aquí"]])

(def empty-state {:url ""
                  :alias ""
                  :input-state :normal
                  :notification {:kind :link
                                 :message greeting}})

(s/def ::state (s/keys ::req-un [::url
                                 ::alias
                                 ::input-state]
                       ::opt-un [::error
                                 ::notification
                                 ::short-url]))

(s/def ::input-state #{:normal :waiting :copy :error})

(s/def ::alias string?)
(s/def ::url string?)

(s/def ::error #{:invalid-url :invalid-alias :used-alias :server-error :rate-limit :network-error :forbidden-domain})


(s/def ::notification (s/keys ::req-un [::kind
                                        ::message]))
(s/def ::kind #{:info :error :success :link})
(s/def ::message any?)

(s/def ::short-url string?)

(def shorten-error?
  #{:invalid-url :server-error :rate-limit :network-error :forbidden-domain})
(def alias-error?
  #{:invalid-alias :used-alias})

(defn notify!
  [state kind message]
  (let [notification {:kind kind
                      :message message}]
    (swap! state assoc :notification notification)))

(rum/defc notifications < rum/static
  [notification]
  (case (:kind notification)
    :error
    [:.notification.is-danger.is-light
     (:message notification)]

    :link
    [:.notification.is-link.is-light
     (:message notification)]

    :info
    [:.notification.is-primary.is-light
     (:message notification)]

    :success
    [:.notification.is-success.is-light
     (:message notification)]

    nil))

(def copy-mixin
  {:after-render
   (fn [state]
     (let [[text] (:rum/args state)
           button (rum/ref-node state "button")
           clip (js/ClipboardJS. button  #js {:text (constantly text)})]
       (assoc state :clip clip)))
   :will-unmount
   (fn [state]
     (dissoc state :clip))})

(rum/defc url-copy-button <  copy-mixin
  [url state]
  [:button
   {:class "button is-light is-success"
    :ref "button"
    :on-click (fn [ev]
                (.preventDefault ev)
                (notify! state :info
                         [:span "Enlace copiado con éxito al portapapeles."])
                (swap! state assoc
                       :input-state :normal
                       :url ""
                       :short-url ""))}
   [:div
    [:span.icon.is-small
     [:i.fas.fa-copy]]
    [:span "Copiar"]]])

(defn shorten-url!
  [url alias state]
  (-> (if-not (blank? alias)
        (io/alias! url (trim alias))
        (io/shorten! url))
      (p/then
       (fn [shortened]
         (swap! state assoc
                :url (:url shortened)
                :short-url (:short-url shortened)
                :alias ""
                :input-state :copy)
         (notify! state :success
                  [:span "Enlace encogido con éxito, tu enlace corto es  "
                   [:a {:href (:short-url shortened)} (:short-url shortened)]])))
      (p/catch (fn [err]
                 (case err
                   :invalid-url
                   (notify! state :error
                            [:span "Enlace no válido. Asegúrate de que es una URL."])

                   :invalid-alias
                   (notify! state :error
                            [:span "Puede usar letras mayúsculas y minúsculas, guiones y guiones bajos."])

                   :used-alias
                   (notify! state :error
                            [:span "El alias elegido ya está en uso, escoge uno diferente."])

                   :server-error
                   (notify! state :error
                            [:span "Ha habido un problema en el servidor y no hemos podido encoger el enlace, inténtelo de nuevo más tarde."])

                   :rate-limit
                   (notify! state :error
                            [:span "Ha usado mucho el servicio, póngase en contacto con nosotros si necesita encoger más enlaces."])

                   :network-error
                   (notify! state :error
                            [:span "Error al hacer la petición, inténtelo de nuevo más tarde."])

                   :forbidden-domain
                   (notify! state :error
                            [:span "No está permitido acortar enlaces de este dominio."])

                   nil)
                 (swap! state assoc
                        :error err
                        :input-state :error)))))

(rum/defc url-input < rum/reactive
  [state]
  (let [{:keys [input-state
                url
                short-url
                alias
                error]} (rum/react state)]
    [:.field.has-addons.has-addons-centered
     [:.control.is-expanded
      [:input
       {:type "text"
        :id "url"
        :class (cond
                 (= input-state :copy) "input is-success"
                 (and (= input-state :error)
                      (shorten-error? error))  "input is-danger"
                 :else "input")
        :placeholder "Escribe o pega aquí tu enlace para encogerlo"
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
                      (shorten-url! url alias state))}
         [:div
          [:span.icon.is-small
           [:i.fas.fa-compress-arrows-alt]]
          [:span "Encoger"]]])]]))

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
        :class (if (and (= input-state :error) (alias-error? error))
                 "input is-danger"
                 "input")
        :id "alias"
        :placeholder "Elige un alias (opcional)"
        :disabled (case input-state
                    :waiting "disabled"
                    "")
        :value alias
        :on-change (fn [ev]
                     (swap! state assoc
                            :input-state :normal
                            :alias (.-value (.-target ev))))}]]]))

(rum/defc shorten-form < rum/reactive
  [state]
  (let [{:keys [notification]} (rum/react state)]
    [:form
     (url-input state)
     (alias-input state)
     (notifications notification)]))
