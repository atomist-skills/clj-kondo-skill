(ns atomist.proc
  (:require [atomist.cljs-log :as log]
            [cljs.core.async :refer [chan >! <!] :refer-macros [go]]
            [clojure.string :as s]
            [cljs-node-io.core :as io]
            [goog.string.format]
            [goog.string :as gstring]))

(def childproc (js/require "child_process"))

(defn- child-process [s ->sub-process]
  (let [c (chan)
        subprocess (->sub-process (fn [err stdout stderr]
                                    (go (>! c [err stdout stderr]))))]
    (.on (. subprocess -stdout) "data" (fn [d] (print (s/trim-newline (gstring/format "(%s):  %s" s (str d))))))
    (.on (. subprocess -stderr) "data" (fn [d] (print (s/trim-newline (gstring/format "(%s):  %s" s (str d))))))
    c))

(defn aexec [cmdline opts]
  (child-process
   (-> cmdline (s/split #" ") first)
   (fn [callback] (childproc.exec cmdline (clj->js opts) callback))))

(defn aexecFile [pathstr args opts]
  (child-process
   (-> pathstr (io/file) (.getName))
   (fn [callback] (childproc.execFile pathstr (into-array args) (clj->js opts) callback))))

(comment
  (go
    (let [[_ stdout stderr] (<! (aexecFile "./atomist.sh" [] {:cwd "/Users/slim/atmhq/bruce"}))]
      (log/infof "finished %d %d" (count stdout) (count stderr)))))
