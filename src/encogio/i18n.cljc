(ns encogio.i18n
  (:require
   [taoensso.tempura :as tempura]))

(defn pluralize
  [word]
  (fn [[n]]
    (if (= n 1)
      word
      (str word "s"))))

(def spanish-locale
  {:missing ":es missing text"
   :time
   {:week (pluralize "semana")
    :day (pluralize "día")
    :hour (pluralize "hora")
    :min (pluralize "minuto")
    :sec (pluralize "segundo")}
   :admin
   {:title "Panel de administración"

    :retry-after
    (fn [[t]]
      (str "Inténtalo despúes de " t))

    :attempts
    (fn [[n]]
      (if (= n 1)
        "Queda un intento"
        (str "Quedan " n " intentos")))

    :site "Sitio"
    :db "Redis"
    :urls "URLs"
    :rate-limit "Límite"
    :clients "Clientes de API"
    :api-clients "Clientes de API"
    :login-attempts "Intentos de login"
    :ip "IP"
    :ttl "Tiempo restante"
    :requests "Peticiones"
    :password "Password"
    :login "Admin login"
    :links "Links"
    :link "Link"
    :url "URL"}
   :html
   {:title "Encog.io | Encoge enlaces"

    :footer
    [:span "Hecho con " [:i.fa.fa-heart.is-heart] " y " [:i.fa.fa-coffee.is-coffee] " por " [:a {:href "http://github.com/purrgrammer"} "Alejandro Gómez"]]}
   :home
   {:title "Encoge enlaces"

    :greeting
    [:p "Si te interesa este proyecto puedes colaborar " [:a {:href "http://encog.io/code"} "aquí"]]

    :subtitle
    "Crea enlaces cortos y reconocibles para compartirlos con el mundo"

    :pitch
    [:span "Anónimo y de " [:a {:href "http://encog.io/code"} "código abierto"]]

    :long-description
    [:span [:a {:href "/"} "encog.io"] " redirige tus enlaces sin auditarlos y está disponible de forma pública aunque sujeto a restricciones de uso. Si estás interesado en usar su " [:a {:href "/api/docs/index.html"} "API"] ", la gestión y edición de enlaces, utilizar dominios propios, o desplegar tu propia instancia, ponte en contacto con nosotros."]

    :get-in-touch "Ponte en contacto"}
   :shorten
   {:url-placeholder "Escribe o pega aquí tu enlace para encogerlo"

    :alias-placeholder "Elige un alias (opcional)"

    :shorten "Encoger"

    :copy "Copiar"

    :copied
    [:span "Enlace copiado con éxito al portapapeles."]

    :shortened
    (fn [[url long-url]]
      [:span "Enlace encogido con éxito, tu enlace corto es  "
       [:a {:href url} url]
       " Redirige al enlace original "
       [:span.has-text-weight-semibold long-url]])

    :invalid-url
    [:span "Enlace no válido. Asegúrate de que es una URL."]

    :invalid-alias
    [:span "Puede usar letras mayúsculas y minúsculas, guiones y guiones bajos."]

    :used-alias
    [:span "El alias elegido ya está en uso, escoge uno diferente."]

    :server-error
    [:span "Ha habido un problema en el servidor y no hemos podido encoger el enlace, inténtelo de nuevo más tarde."]

    :rate-limit
    [:span "Ha usado mucho el servicio, póngase en contacto con nosotros si necesita encoger más enlaces."]

    :network-error
    [:span "Error al hacer la petición, inténtelo de nuevo más tarde."]

    :forbidden-domain
    [:span "No está permitido acortar enlaces de este dominio."]}})

(def english-locale
  {:missing ":en missing text"
   :time
   {:week (pluralize "week")
    :day (pluralize "day")
    :hour (pluralize "hour")
    :min (pluralize "minute")
    :sec (pluralize "second")}
   :admin
   {:title "Admin panel"

    :retry-after
    (fn [[t]]
      (str "Retry after " t))

    :attempts
    (fn [[n]]
      (if (= n 1)
        "One attempt remaining"
        (str n " attempts remaining")))

    :site "Site"
    :db "Redis"
    :urls "URLs"
    :rate-limit "Rate limit"
    :clients "API clients"
    :api-clients "API clients"
    :login-attempts "Login attempts"
    :ip "IP"
    :ttl "TTL"
    :requests "Requests"
    :password "Password"
    :login "Admin login"
    :links "Links"
    :link "Link"
    :url "URL"}
   :html
   {:title "Encog.io | Shorten links"

    :footer
    [:span "Made with " [:i.fa.fa-heart.is-heart] " and " [:i.fa.fa-coffee.is-coffee] " by " [:a {:href "http://github.com/purrgrammer"} "Alejandro Gómez"]]}
   :home
   {:title "Shorten links"

    :greeting
    [:p "If you are interested in contributing take a look " [:a {:href "http://encog.io/code"} "at the code"]]

    :subtitle
    "Create short and memorable links to share them with the world"

    :pitch
    [:span "Anonymous and " [:a {:href "http://encog.io/code"} "open source"]]

    :long-description
    [:span [:a {:href "/"} "encog.io"] " redirects your links without auditing them and is publicly available (rate limited). If you are interested in using its " [:a {:href "/api/docs/index.html"} "API"] ", link edition and managements, using custom domains, or deploying your own instance, get in touch with us."]

    :get-in-touch "Get in touch"}
   :shorten
   {:url-placeholder "Write or paste your link here to shorten it"

    :alias-placeholder "Pick an alias (optional)"

    :shorten "Shorten"

    :copy "Copy"

    :copied
    [:span "The link has been copied to the clipboard"]

    :shortened
    (fn [[url long-url]]
      [:span  "The link has been shortened, your short link is "
       [:a {:href url} url]
       " Redirects to the original link "
       [:span.has-text-weight-semibold long-url]])

    :invalid-url
    [:span "Invalid link. Make sure it is a valid URL."]

    :invalid-alias
    [:span "You can use upper and lowercase letters from the spanish alphabet, hyphens and underscores."]

    :used-alias
    [:span "The chosen alias is being used, choose a different one."]

    :server-error
    [:span "There was a problem in the server and we could't shorten the link, please try again later."]

    :rate-limit
    [:span "You have used the service too much, get in touch with us if you need to shorten more links."]

    :network-error
    [:span "Network error, try again later."]

    :forbidden-domain
    [:span "Is forbidden to shorten URLs in the specified domain."]}})

(def tempura-dictionary
  {:es-ES ; Locale
   spanish-locale
   :es
   spanish-locale
   :en
   english-locale
   :en-GB
   english-locale
   :en-US
   english-locale})

(def opts {:dict tempura-dictionary
           :default-locale :es-ES})

(def ^:private default-tr
  (partial tempura/tr opts [:en]))

(defn make-tr [langs]
  (partial tempura/tr opts langs))

#?(:clj
   (defn middleware
     [handler]
     (tempura/wrap-ring-request handler {:tr-opts opts})))

#?(:clj
   (defn request->tr
     [req]
     (or (:tempura/tr req) default-tr)))

#?(:clj
   (defn request->langs
     [req]
     (:tempura/accept-langs req)))

#?(:cljs
   (defn get-languages
     []
     (mapv keyword
           (or (.-languages js/navigator) []))))
