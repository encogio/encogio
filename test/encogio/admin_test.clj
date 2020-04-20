(ns encogio.admin-test
  (:require
   [clojure.test :refer [deftest is]]
   [encogio.redis-test :refer [flush! test-server]]
   [encogio.admin :as admin]))

(deftest set-check-admin-password
  (admin/set-admin-password test-server "a password")
  (is (admin/check-admin-password test-server "a password"))
  (is (not (admin/check-admin-password test-server "wrong"))))
