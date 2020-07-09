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
