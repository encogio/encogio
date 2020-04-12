(ns encogio.admin-test
  (:require
   [clojure.test :refer [deftest is]]
   [encogio.redis-test :refer [flush!]]
   [encogio.admin :as admin]))

(def test-server {:pool {} :spec {:url "127.0.0.0.1"}})

(deftest set-check-admin-password
  (admin/set-admin-password test-server "a password")
  (is (admin/check-admin-password test-server "a password"))
  (is (not (admin/check-admin-password test-server "wrong"))))
