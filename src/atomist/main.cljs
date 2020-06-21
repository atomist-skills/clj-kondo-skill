(ns atomist.main
  (:require [atomist.api :as api]
            [atomist.container :as container]
            [atomist.github :as github]
            [cljs.core.async :refer [<!]]
            [goog.string.format]
            [goog.string :as gstring]
            [clojure.data]
            [cljs-node-io.core :as io]
            [atomist.proc :as proc]
            [atomist.cljs-log :as log]
            [clojure.string :as s]
            [atomist.json :as json]
            [cljs.pprint :refer [pprint]]
            [clojure.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn stop-if-no-clj-files-detected
  "this will halt on Repos that have no clj(sc) files
   we can remove this when the content filters are complete"
  [handler]
  (fn [request]
    (go
      (api/trace "stop-if-no-clj-files-detected")
      (if-let [atm-home (.. js/process -env -ATOMIST_HOME)]
        (let [clojure-files (->> (io/file-seq atm-home)
                                 (filter #(s/includes? (.getName (io/file %)) ".clj"))
                                 (count))]
          (if (> clojure-files 0)
            (<! (handler (assoc request :atm-home (io/file atm-home))))
            (<! (api/finish request :success "skipping repo that contains no clj files" :visibility :hidden))))
        (do
          (log/warn "this skill requires an ATOMIST_HOME environment variable")
          (<! (api/finish request :failure "no ATOMIST_HOME env variable set")))))))

(def output-config
  #_{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}
  {:output {:format :json}})

(defn run-clj-kondo [handler]
  (fn [request]
    (go
      (try
        (api/trace "run-clj-kondo")
        (let [atm-home (:atm-home request)
              clj-kondo-params (concat
                                ["--lint" "src"]
                                (cond
                                  (:config request) ["--config" (-> (:config request)
                                                                    (edn/read-string)
                                                                    (merge output-config)
                                                                    (pr-str))]
                                  (:config-gist request) ["--config" (-> (<! (github/gist-content
                                                                              request
                                                                              (:config-gist request)))
                                                                         (edn/read-string)
                                                                         (merge output-config)
                                                                         (pr-str))]
                                  :else ["--config" output-config]))
              sub-process-port (proc/aexecFile
                                "/usr/local/bin/clj-kondo"
                                clj-kondo-params
                                {:cwd (.getPath atm-home)})
              [err stdout _] (<! sub-process-port)]
          (log/info "params:  " clj-kondo-params)
          (log/info "stdout: " stdout)
          (if err
            (let [{:keys [findings summary]} (json/->obj stdout)]
              (log/error "process exited with code " (. err -code))
              (<! (handler (assoc request
                                  :checkrun/conclusion "failure"
                                  :checkrun/output {:title (case (. err -code)
                                                             2 "clj-kondo found warnings"
                                                             3 "clj-kondo found errors"
                                                             "clj-kondo failure")
                                                    :summary (gstring/format "```%s```" (with-out-str (pprint summary)))
                                                    :annotations (->> findings
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
                                                                      (into []))}))))
            (<! (handler (assoc request :checkrun/conclusion "success"
                                :checkrun/output {:title "clj-kondo saw no warnings or errors"
                                                  :summary (apply str (take-last 300 stdout))})))))
        (catch :default ex
          (log/error ex)
          (<! (api/finish request :failure "failed to run clj-kondo")))))))

(defn ^:export handler
  ""
  []
  ((-> (api/finished)
       (run-clj-kondo)
       (api/with-github-check-run :name :check-name :default "clj-kondo")
       (api/extract-github-token)
       (api/create-ref-from-event)
       (api/add-skill-config :config :config-gist :check-name)
       (stop-if-no-clj-files-detected)
       (api/status)
       (container/mw-make-container-request)) {}))
