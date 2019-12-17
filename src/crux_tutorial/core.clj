(ns crux-tutorial.core
  (:gen-class)
  (:require [crux.api :as crux]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn hello
  []
  (println "Hello"))


; start node
(def crux
    (crux/start-node
       {:crux.node/topology :crux.standalone/topology
        :crux.node/kv-store "crux.kv.memdb/kv"
        :crux.standalone/event-log-dir "data/eventlog-1"
        :crux.kv/db-dir "data/db-dir"
        :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))


; a document
(def manifest
  {:crux.db/id :manifest
   :pilot-name "Johanna"
   :id/rocket "SB002-sol"
   :id/employee "22910x2"
   :badges "SETUP"
   :cargo ["stereo" "gold fish" "slippers" "secret note"]})

; store the document into Crux with PUT command

(crux/submit-tx crux [[:crux.tx/put manifest]])

; Check that this was successful by asking Crux to show the whole entity.
(crux/entity (crux/db crux) :manifest)

(defn easy-ingest
  "Uses Crux put transaction to add a vector of documents to a specified node"
  [node docs]
  (crux/submit-tx node
                  (vec (for [doc docs]
                         [:crux.tx/put doc]))))

(easy-ingest crux [{:crux.db/id :demo :name "Zoltan"}])
;#:crux.tx{:tx-id 1614422045402113, :tx-time #inst "2019-12-17T12:00:28.713-00:00"}

(crux/entity (crux/db crux) :demo)
;{:crux.db/id :demo, :name "Zoltan"}

; batch all updates into a transaction
(crux/submit-tx crux
                [[:crux.tx/put
                  {:crux.db/id :commodity/Pu
                   :common-name "Plutonium"
                   :type :element/metal
                   :density 19.816
                   :radioactive true}]

                 [:crux.tx/put
                  {:crux.db/id :commodity/N
                   :common-name "Nitrogen"
                   :type :element/gas
                   :density 1.2506
                   :radioactive false}]

                 [:crux.tx/put
                  {:crux.db/id :commodity/CH4
                   :common-name "Methane"
                   :type :molecule/gas
                   :density 0.717
                   :radioactive false}]])

; Get back
(crux/entity (crux/db crux) :commodity/Pu)
(crux/entity (crux/db crux) :commodity/N)
(crux/entity (crux/db crux) :commodity/CH4)

; the PUT transaction has the form of [:crux.tx/put doc valid-time-start valid-time-end]
; format: #inst "1999-01-02T21:30:00"
(crux/submit-tx crux
                [[:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 21 }
                  #inst "2115-02-13T18"] ;; valid-time

                 [:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 23 }
                  #inst "2115-02-14T18"]])

; will return :stock/Pu with :weight-ton 21
(crux/entity (crux/db crux #inst "2115-02-13T19") :stock/Pu)

; will return :stock/Pu with :weight-ton 23
(crux/entity (crux/db crux #inst "2115-02-14T19") :stock/Pu)

; will return nil
(crux/entity (crux/db crux) :stock/Pu)

; searching
; #{[:commodity/Pu]}
(crux/q (crux/db crux)
        '{:find [element]
          :where [[element :type :element/metal]]} )
;#{[:commodity/Pu]}

; you can also write it like this as well
(crux/q (crux/db crux)
         (quote
          {:find [element]
           :where [[element :type :element/metal]]}) )
#{[:commodity/Pu]}

; instead of returning the id we can return the name
(crux/q (crux/db crux)
        '{:find [name]
          :where [[e :type :element/metal]
                  [e :common-name name]]} )
;#{["Plutonium"]}
