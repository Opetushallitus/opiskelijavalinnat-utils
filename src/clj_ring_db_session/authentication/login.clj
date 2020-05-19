(ns clj-ring-db-session.authentication.login
  (:require [ring.util.response :as resp]))

(defn login [params]
  (let [{:keys [username
                henkilo
                ticket
                success-redirect-url]} params]
    (-> (resp/redirect success-redirect-url)
        (assoc :session {:identity  {:username   username
                                     :first-name (:kutsumanimi henkilo)
                                     :last-name  (:sukunimi henkilo)
                                     :oid        (:oidHenkilo henkilo)
                                     :lang       (or (some #{(-> henkilo :asiointiKieli :kieliKoodi)}
                                                           ["fi" "sv" "en"])
                                                     "fi")
                                     :ticket     ticket}
                         :logged-in true}))))

(defn logout [session logout-url]
  (-> (resp/redirect logout-url)
      (assoc :session {:identity  nil
                       :logged-in false})))

(defn logged-in? [request]
  (-> request :session :logged-in))
