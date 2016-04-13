# batchloop
Minimalistic library to asynchronously process files line by line

## Usage

```clojure
[batchloop "1.0.0"]

(ns my.ns
  (:require [batchloop.core :as b]))

(b/process :path    "myfile.txt"
           :timeout 300        ;; pass a timeout (in ms) for each line processing
           :buffer  10         ;; how many lines to buffer in memory

                       ;;   The action to be performed receives three arguments.
                       ;;   You need to explicitly call (success!) OR (failure!)
                       ;; to indicate that the line has been processed, otherwise
                       ;; batchloop will consider it finished when the timeout
                       ;; explodes.
           :action (fn [line success! failure!]
                     (println line)
                     (success!)))
```
