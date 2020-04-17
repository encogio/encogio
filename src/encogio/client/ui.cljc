(ns encogio.client.ui
  (:require
   [clojure.spec.alpha :as s]
   #?@(:cljs [[cljsjs.clipboard]
              [encogio.client.io :as io]
              [promesa.core :as p]
              [clojure.string :refer [blank? trim]]])
   [rum.core :as rum]))

(defn empty-state
  [tr]
  {:url ""
   :alias ""
   :input-state :normal
   :notification {:kind :link
                  :message (tr [:home/greeting])}})

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

#?(:cljs
   (defn clipboard
     [el text]
     (js/ClipboardJS. el #js {:text (constantly text)})))

(rum/defc url-copy-button
  [url state tr]
  (let [button (rum/use-ref nil)
        #?@(:cljs [_ (rum/use-effect! (fn []
                                        (let [bt (.-current button)
                                              clip (clipboard bt url)]
                                          (fn []
                                            (.destroy clip)))))])]
    [:button
     {:class "button is-light is-success"
      :ref button
      :on-click (fn [ev]
                  (.preventDefault ev)
                  (notify! state :info
                           (tr [:shorten/copied]))
                  (swap! state assoc
                         :input-state :normal
                         :url ""
                         :short-url ""))}
     [:div
      [:span.icon.is-small
       [:i.fas.fa-copy]]
      [:span (tr [:shorten/copy])]]]))

#?(:cljs
   (defn shorten-url!
     [url alias state tr]
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
                     (tr [:shorten/shortened] [(:short-url shortened)]))))
         (p/catch (fn [err]
                    (case err
                      :invalid-url
                      (notify! state :error
                               (tr [:shorten/invalid-url]))
                      
                      :invalid-alias
                      (notify! state :error
                               (tr [:shorten/invalid-alias]))

                      :used-alias
                      (notify! state :error
                               (tr [:shorten/used-alias]))

                      :server-error
                      (notify! state :error
                               (tr [:shorten/server-error]))

                      :rate-limit
                      (notify! state :error
                               (tr [:shorten/rate-limit]))

                      :network-error
                      (notify! state :error
                               (tr [:shorten/network-error]))

                      :forbidden-domain
                      (notify! state :error
                               (tr [:shorten/forbidden-domain]))

                      nil)
                    (swap! state assoc
                           :error err
                           :input-state :error))))))


(rum/defc url-input < rum/reactive
  [state tr]
  (let [{:keys [input-state
                url
                short-url
                alias
                error]} (rum/react state)
        disabled? (= input-state :waiting)]
    [:.field.has-addons.has-addons-centered
     [:.control.is-expanded
      [:input
       {:type "text"
        :id "url"
        :auto-complete "off"
        :class (cond
                 (= input-state :copy) "input is-success"
                 (and (= input-state :error)
                      (shorten-error? error))  "input is-danger"
                 :else "input")
        :placeholder (tr [:shorten/url-placeholder])
        :disabled (when disabled? "disabled")
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
        (url-copy-button short-url state tr)
        
        :else
        [:button.button.is-primary
         {:disabled (when disabled? "disabled")
          #?@(:cljs [:on-click (fn [ev]
                                 (.preventDefault ev)
                                 (swap! state assoc :input-state :waiting)
                                 (shorten-url! url alias state tr))])}
         [:div
          [:span.icon.is-small
           [:i.fas.fa-compress-arrows-alt]]
          [:span (tr [:shorten/shorten])]]])]]))

(rum/defc alias-input < rum/reactive
  [state tr]
  (let [{:keys [error
                alias
                input-state]} (rum/react state)
        disabled? (= input-state :waiting)]
    [:.field.has-addons
     [:.control
      [:a {:class "button is-static"}
       "http://encog.io/"]]
     [:.control
      [:input.input
       {:type "text"
        :auto-complete "off"
        :class (if (and (= input-state :error) (alias-error? error))
                 "input is-danger"
                 "input")
        :id "alias"
        :placeholder (tr [:shorten/alias-placeholder])
        :disabled (when disabled? "disabled")
        :value alias
        :on-change (fn [ev]
                     (swap! state assoc
                            :input-state :normal
                            :alias (.-value (.-target ev))))}]]]))

(rum/defc shorten-form < rum/reactive
  [state tr]
  (let [{:keys [notification]} (rum/react state)]
    [:form
     (url-input state tr)
     (alias-input state tr)
     (notifications notification)]))
