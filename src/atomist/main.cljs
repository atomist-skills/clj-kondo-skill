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
            [atomist.cljs-log :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn run-clj-kondo [handler]
  (fn [request]
    (go
      (try
        (api/trace "run-clj-kondo")
        (if-let [atm-home (.. js/process -env -ATOMIST_HOME)]
          (let [f (io/file atm-home)
                sub-process-port (do
                                   (log/info "run /usr/local/bin/clj-kondo")
                                   (proc/aexecFile "/usr/local/bin/clj-kondo"
                                                   (concat
                                                    ["--lint" "src"]
                                                    (cond
                                                      (:config request) ["--config" (:config request)]
                                                      (:config-gist request) ["--config" (<! (github/gist-content request (:config-gist request)))]
                                                      :else nil))
                                                   {:cwd (.getPath f)}))
                [err stdout stderr] (<! sub-process-port)]
            (if err
              (do
                (log/error "process exited with code " (. err -code))
                (<! (handler (assoc request :checkrun/conclusion "failure"
                                    :checkrun/output {:title (case (. err -code)
                                                               2 "clj-kondo found warnings"
                                                               3 "clj-kondo found errors"
                                                               "clj-kondo failure")
                                                      :summary (gstring/format "## stdout\n%s\n## stderr\n%s"
                                                                               stdout stderr)}))))
              (<! (handler (assoc request :checkrun/conclusion "success"
                                  :checkrun/output {:title "clj-kondo saw no warnings or errors"
                                                    :summary (apply str (take-last 300 stdout))})))))
          (do
            (log/warn "this skill requires an ATOMIST_HOME environment variable")
            (<! (api/finish request :failure "no ATOMIST_HOME env variable set"))))
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
       (api/status)
       (container/mw-make-container-request)) {}))
