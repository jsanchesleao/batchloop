(ns batchloop.core
  (:require [clojure.core.async :refer [go go-loop <! >! chan alt!] :as a])
  )

(defn process [& {:keys [path timeout action buffer]}]
  (let [line-ch (chan buffer)
        count-ch (chan buffer)
        done-ch (chan buffer)
        result (promise) ]
    ;loop through file
    (go
      (with-open [file (clojure.java.io/reader path)]
        (doseq [line (line-seq file)]
          (>! count-ch :line)
          (>! line-ch line))
        (>! count-ch :eof)))

    ;perform actions
    (go-loop [line (<! line-ch)]
      (let [notify-ch (chan)
            timeout-ch (a/timeout timeout)
            done! (fn [] (go (>! notify-ch :done)))
            fail! (fn [] (go (>! notify-ch :fail)))]
        (go
          (alt!
            timeout-ch ([_] (>! done-ch :timeout))
            notify-ch  ([r] (>! done-ch r))))
        (action line done! fail!)
        (recur (<! line-ch))))

    ;collect results
    (go-loop [eof false
              total 0
              failures 0
              successes 0
              timedout 0]
      (if (and eof (= total (+ failures successes timedout)))
        (deliver result {:total total :failures failures :successes successes :timedout timedout})
        (alt!
          done-ch ([r] (case r
                         :timeout (recur eof total failures successes (inc timedout))
                         :done    (recur eof total failures (inc successes) timedout)
                         :fail    (recur eof total (inc failures) successes timedout)))
          count-ch ([r] (case r
                          :line (recur eof (inc total) failures successes timedout)
                          :eof  (recur true total failures successes timedout))))))

    result))
