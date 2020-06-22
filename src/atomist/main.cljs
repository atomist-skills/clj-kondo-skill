(ns atomist.main
  (:require [atomist.middleware :as mw]
            [atomist.clj-kondo :as clj-kondo]))

(defn ^:export handler
  ""
  []
  ((mw/with-check-run-producing-handler :cmd "/usr/local/bin/clj-kondo"
     :->args clj-kondo/construct-clj-kondo-args
     :on-success clj-kondo/on-success
     :on-failure clj-kondo/on-failure
     :ext ".clj") {}))
