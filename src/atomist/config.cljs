;; Copyright © 2020 Atomist, Inc.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns atomist.config
  (:require
   [clojure.edn :as edn]
   [atomist.http :as http]
   [goog.string.format]
   [goog.string :as gstring]
   [cljs.core.async :refer-macros [go] :refer [<!] :as async]
   [atomist.cljs-log :as log]
   [atomist.api :as api]
   [cljs-node-io.core :as io]
   [cljs-node-io.proc :as proc]))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond
     (and (map? a) (map? b)) (merge-with deep-merge a b)
     (and (sequential? a) (sequential? b)) (into a b)
     (and (set? a) (set? b)) (into a b)
     (false? b) b
     :else (or b a)))
  ([a b & more] (apply merge-with deep-merge a b more)))

(defn merge-config!
  [cfg* cfg]
  (if (empty? cfg)
    cfg*
    (let [cfg
          (cond-> cfg
            (:skip-comments cfg)
            (->
             (update
              :skip-args
              (fnil conj [])
              'clojure.core/comment
              'cljs.core/comment))
            (contains? (:linters cfg) :if)
            (assoc-in [:linters :missing-else-branch] (:if (:linters cfg))))]
      (if (:replace (meta cfg)) cfg (deep-merge cfg* cfg)))))

(def re-github-repo-path #"https://github.com/(.*?)/(.*?)/tree/(.*?)/(.*)")

(defn merge-clj-kondo-lib
  "copy any hooks"
  [{{clj-kondo-basedir :path} :project} project-basedir clj-kondo-lib-path]
  (go
    (let [project-clj-kondo (io/file project-basedir ".clj-kondo")
          lib-clj-kondo
          (io/file clj-kondo-basedir clj-kondo-lib-path ".clj-kondo")
          project-config-edn (io/file project-clj-kondo "config.edn")
          lib-config-edn (io/file lib-clj-kondo "config.edn")]
      (when (and (.exists project-config-edn) (.exists lib-config-edn))
        (if-let [new-config (merge-config!
                             (edn/read-string (io/slurp project-config-edn))
                             (edn/read-string (io/slurp lib-config-edn)))]
          (io/spit project-config-edn (pr-str new-config))))
      (.mkdir project-clj-kondo)
      (when (.exists lib-config-edn) (.delete lib-config-edn))
      (<!
       (proc/aexec
        (gstring/format
         "cp -R %s/ %s"
         (.getPath lib-clj-kondo)
         (.getPath project-clj-kondo)))))))

(defn merge-config-content [basedir url]
  (let [content (<! (http/get-url url))
        clj-kondo-config-edn (io/file basedir ".clj-kondo" "config.edn")]
    (if-let [edn (and
                  content
                  (try
                    (edn/read-string content)
                    (catch :default _
                      (log/warnf "failure to read edn at url: %s" url))))]
      (if (.exists clj-kondo-config-edn)
        (io/spit clj-kondo-config-edn (-> edn
                                          (merge-config! (edn/read-string (io/slurp clj-kondo-config-edn)))
                                          (pr-str)))
        (io/spit clj-kondo-config-edn (pr-str edn))))))

(defn merge-url
  "merge"
  [request url]
  (go
    (if-let [[_ org repo branch path] (re-find re-github-repo-path url)]
      (<! ((-> #(go
                  (<! (merge-clj-kondo-lib % (-> request :project :path) path)))
               (api/clone-ref))
           (assoc request :ref {:owner org, :repo repo, :branch branch})))
      (merge-config-content (-> request :project :path) url))))
