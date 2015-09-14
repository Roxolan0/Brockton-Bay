(defproject brockton-bay "0.1.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [seesaw "1.4.5"]]

  :profiles {:dev  {:dependencies [[midje "1.7.0"]]
                    :plugins      [[lein-set-version "0.4.1"]
                                   [lein-midje "3.1.0"]]
                    }
             ;:user {:plugins [[lein-kibit "0.1.2"]]}
             })
