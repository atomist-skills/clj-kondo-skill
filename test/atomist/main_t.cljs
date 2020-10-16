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

(ns atomist.main-t
  (:require [cljs.test :refer-macros [is deftest run-tests async]]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [atomist.main :as main]
            [cljs-node-io.core :as io]
            [clojure.edn :as edn]))

(deftest constuct-args-test
  (async
   done
   (go
     (let [project-path (io/file "./project")
           clj-kondo-path (io/file project-path ".clj-kondo")]
       (.mkdirs clj-kondo-path)
       (io/spit (io/file clj-kondo-path "config.edn") (pr-str {:my-weird-config :config}))
       (is (= ["--lint" "src" "--config" (pr-str main/output-config)]
              (<! (main/construct-clj-kondo-args {:config-gist-url ["https://github.com/borkdude/clj-kondo/tree/master/libraries/slingshot"]
                                                  :token "token"
                                                  :project {:path "./project"}}))))
       (is (= (edn/read-string (io/slurp (io/file clj-kondo-path "config.edn")))
              {:my-weird-config :config, :hooks {:analyze-call {'slingshot.slingshot/try+ 'hooks.slingshot.try-plus/try+}}}))
       (is (.exists (io/file clj-kondo-path "hooks" "slingshot" "try_plus.clj"))))
     (done))))

(comment
  (enable-console-print!)
  (run-tests))
