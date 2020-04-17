(ns encogio.client.macros
  (:require
   [encogio.auth :as auth]
   [encogio.config :as config]))

(defmacro deftoken
  [name]
  (let [t (auth/create-token {:user-id :anonymous})]
    `(def ~name ~t)))

(defmacro defsite
  [name]
  `(def ~name ~config/site))
