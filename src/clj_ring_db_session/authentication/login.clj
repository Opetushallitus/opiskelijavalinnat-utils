(ns clj-ring-db-session.authentication.login
  (:require [clj-ring-db-session.authentication.cas-ticketstore :as cas-store]
            [ring.util.response :as resp]))

(defn login [params]
  (let [{:keys [username
                henkilo
                ticket
                success-redirect-url
                datasource]} params]
    (cas-store/login ticket datasource)
    (-> (resp/redirect success-redirect-url)
        (assoc :session {:identity {:username   username
                                    :first-name (:kutsumanimi henkilo)
                                    :last-name  (:sukunimi henkilo)
                                    :oid        (:oidHenkilo henkilo)
                                    :lang       (or (some #{(-> henkilo :asiointiKieli :kieliKoodi)}
                                                          ["fi" "sv" "en"])
                                                    "fi")
                                    :ticket     ticket}}))))

(defn logout [session logout-url datasource]
  (cas-store/logout (-> session :identity :ticket) datasource)
  (-> (resp/redirect logout-url)
      (assoc :session {:identity nil})))

(defn logged-in? [request datasource]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket datasource)))
