(ns atomist.main
  (:require
   [atomist.json :as json]
   [atomist.middleware :as mw]
   [atomist.config :as config]
   [clojure.edn :as edn]
   [goog.string.format]
   [goog.string :as gstring]
   [cljs.core.async :refer-macros [go] :refer [<!] :as async]
   [atomist.cljs-log :as log]
   [atomist.api :as api]
   [cljs-node-io.proc :as proc]))

(def output-config
  {:output {:format :json}})

(defn construct-clj-kondo-args
  [request]
  (go
    (concat
     ["--lint" "src"]
     (try
       (cond
        ;; config in skill
         (:config request)
         ["--config" (->
                      (:config request)
                      (edn/read-string)
                      (merge output-config)
                      (pr-str))]
        ;; config url is a git repo (potentially with hooks)
         (seq (:config-gist-url request))
         (do
           (<! (->> (for [url (seq (:config-gist-url request))]
                      (config/merge-url request url))
                    (async/merge)
                    (async/reduce conj [])))
           ["--config" (pr-str output-config)])

         :else
         ["--config" (pr-str output-config)])
       (catch :default ex (log/error ex "error constructing clj-kondo args"))))))

(defn- findings->annotations
  [findings]
  (->>
   findings
   (map
    #(assoc {}
            :path (:filename %)
            :start_line (:row %)
            :end_line (:end-row %)
            :annotation_level
            (case (:level %)
              "warning" "warning"
              "info" "notice"
              "error" "failure")
            :message (:message %)
            :title (:type %)))
   (take 50)
   (into [])))

(defn on-success
  [_ _ stdout _]
  (go
    (let [{:keys [summary]} (json/->obj stdout)]
      {:checkrun/conclusion "success",
       :checkrun/output
       {:title "clj-kondo saw no warnings or errors",
        :summary
        (gstring/format
         "**Summary**:\nErrors:  %d\nWarnings:  %d\nDuration:  %d"
         (:error summary)
         (:warning summary)
         (:duration summary))}})))

(defn on-failure
  [_ err stdout _]
  (go
    (let [{:keys [findings summary]} (json/->obj stdout)]
      {:checkrun/conclusion "failure",
       :checkrun/output
       {:title
        (case (. err -code)
          2 "clj-kondo found warnings"
          3 "clj-kondo found errors"
          "clj-kondo failure"),
        :summary
        (gstring/format
         "**Summary**:\nErrors:  %d\nWarnings:  %d\nDuration:  %d"
         (:error summary)
         (:warning summary)
         (:duration summary)),
        :annotations (findings->annotations findings)}})))

(defn show-clj-kondo-version
  [handler binary]
  (fn [request]
    (go
      (api/trace "show-clj-kondo-version")
      (let [[_ stdout _]
            (<! (proc/aexecFile binary ["--version"]
                                {:cwd (.getPath (or
                                                 (:atm-home request)
                                                 (->
                                                  request
                                                  :project
                                                  :path)))}))]
        (log/info stdout))
      (<! (handler request)))))

(defn ^:export handler
  ""
  []
  (let [binary "/usr/local/bin/clj-kondo"]
    ((mw/with-check-run-producing-handler
       :cmd binary
       :->args construct-clj-kondo-args
       :on-success on-success
       :on-failure on-failure
       :ext ".clj"
       :middleware (api/compose-middleware [show-clj-kondo-version binary]))
     {:check-name "clj-kondo-skill"})))
