(ns clj-ring-db-session.authentication.cas-ticketstore
  (:require [clj-ring-db-session.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/cas-ticketstore-queries.sql")

(defn login [ticket datasource]
  (exec datasource yesql-add-ticket-query! {:ticket ticket}))

(defn logout [ticket datasource]
  (exec datasource yesql-remove-ticket-query! {:ticket ticket}))

(defn logged-in? [ticket datasource]
  (first (exec datasource yesql-ticket-exists-query {:ticket ticket})))
