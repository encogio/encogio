(ns encogio.client.token
  (:require
   [encogio.auth :as auth]))

(defmacro deftoken
  [name]
  (let [t (auth/create-token {:user-id :anonymous})]
    `(def ~name ~t)))
