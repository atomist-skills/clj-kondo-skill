(ns atomist.main
  (:require [atomist.json :as json]
            [atomist.middleware :as mw]
            [clojure.edn :as edn]
            [atomist.http :as http]
            [goog.string.format]
            [goog.string :as gstring]
            [cljs.core.async :refer-macros [go] :refer [<!]]
            [atomist.cljs-log :as log]))

(def output-config
  #_{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}
  {:output {:format :json}})

(defn construct-clj-kondo-args [request]
  (go
    (concat
     ["--lint" "src"]
     (try
       (cond
         (:config request) ["--config" (-> (:config request)
                                           (edn/read-string)
                                           (merge output-config)
                                           (pr-str))]
         (:config-gist-url request) ["--config" (some-> (<! (http/get-url (:config-gist-url request)))
                                                        (edn/read-string)
                                                        (merge output-config)
                                                        (pr-str))]
         :else ["--config" output-config])
       (catch :default ex
         (log/error ex "error constructing clj-kondo args"))))))

(defn- findings->annotations [findings]
  (->> findings
       (map #(assoc {}
                    :path (:filename %)
                    :start_line (:row %)
                    :end_line (:end-row %)
                    :annotation_level (case (:level %)
                                        "warning" "warning"
                                        "info" "notice"
                                        "error" "failure")
                    :message (:message %)
                    :title (:type %)))
       (take 50)
       (into [])))

(defn on-success [_ _ stdout _]
  (go
    {:checkrun/conclusion "success"
     :checkrun/output {:title "clj-kondo saw no warnings or errors"
                       :summary (apply str (take-last 300 stdout))}}))

(defn on-failure [_ err stdout _]
  (go
    (let [{:keys [findings summary]} (json/->obj stdout)]
      {:checkrun/conclusion "failure"
       :checkrun/output {:title (case (. err -code)
                                  2 "clj-kondo found warnings"
                                  3 "clj-kondo found errors"
                                  "clj-kondo failure")
                         :summary (gstring/format "**Summary**:\nErrors:  %d\nWarnings:  %d\nDuration:  %d"
                                                  (:error summary)
                                                  (:warning summary)
                                                  (:duration summary))
                         :annotations (findings->annotations findings)}})))

(defn ^:export handler
  ""
  []
  ((mw/with-check-run-producing-handler :cmd "/usr/local/bin/clj-kondo"
     :->args construct-clj-kondo-args
     :on-success on-success
     :on-failure on-failure
     :ext ".clj") {}))
