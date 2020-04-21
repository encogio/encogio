(ns encogio.html
  (:require
   [encogio.client.ui :refer [shorten-form empty-state]]
   [rum.core :as rum]))

(rum/defc page
  [{:keys [title
           lang
           stylesheets
           scripts
           author
           description]
    :or {title "Encog.io"
         lang "es"
         stylesheets ["/css/font-awesome.css"
                      "/css/main.css"]
         scripts []}}
   body]
  [:html
   {:lang lang}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    (when author
      [:meta {:name "author"
              :content author}])
    (when description
      [:meta {:name "description"
              :content description}])
    [:title title]
    (for [s stylesheets]
      [:link {:rel "stylesheet"
              :href s}])
    (for [s scripts]
      [:script {:src s}])]
   body])

(rum/defc home
  [site tr]
  (page {:title (tr [:home/title])
         :description (tr [:home/description])}
        [:body
         [:section.hero.is-primary.is-bold.has-text-centered
          ; hero
          [:.hero-body
           [:.container
            [:h1.title (tr [:home/title])]
            [:h2.subtitle (tr [:home/subtitle])]]]]
         ; form
         [:section#shorten-form.section
          (shorten-form (atom (empty-state {:tr tr
                                            :site site})) tr)]
         [:hr]
         ; message
         [:section.section
          [:.container.has-text-centered
           [:h1.title.is-spaced
            (tr [:home/pitch])]
           [:p.subtitle
            (tr [:home/long-description])]
           [:.buttons.is-centered
            [:a.button.is-primary
             {:href "mailto:bandarra@protonmail.com"}
             (tr [:home/get-in-touch])]]]]
         ; footer
         [:footer.footer
          [:.content.has-text-centered
           [:p
            (tr [:html/footer])]]]
         [:script {:src "/js/app.js"}]]))

#?(:clj
   (defn render-home
     [site tr]
     (rum/render-static-markup (home site tr))))
