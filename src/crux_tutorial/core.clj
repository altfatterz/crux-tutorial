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

; tutorial from https://nextjournal.com/crux-tutorial

; SETUP ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

; PUT //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

; store the document into Crux with PUT command

(crux/submit-tx crux [[:crux.tx/put manifest]])

; Check that this was successful by asking Crux to show the whole entity.
(crux/entity (crux/db crux) :manifest)

; ------------------------ easy-ingenst --------------------------------------------------------------------------------
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

(def data
  [{:crux.db/id :commodity/Pu
    :common-name "Plutonium"
    :type :element/metal
    :density 19.816
    :radioactive true}

   {:crux.db/id :commodity/N
    :common-name "Nitrogen"
    :type :element/gas
    :density 1.2506
    :radioactive false}

   {:crux.db/id :commodity/CH4
    :common-name "Methane"
    :type :molecule/gas
    :density 0.717
    :radioactive false}

   {:crux.db/id :commodity/Au
    :common-name "Gold"
    :type :element/metal
    :density 19.300
    :radioactive false}

   {:crux.db/id :commodity/C
    :common-name "Carbon"
    :type :element/non-metal
    :density 2.267
    :radioactive false}

   {:crux.db/id :commodity/borax
    :common-name "Borax"
    :IUPAC-name "Sodium tetraborate decahydrate"
    :other-names ["Borax decahydrate" "sodium borate" "sodium tetraborate" "disodium tetraborate"]
    :type :mineral/solid
    :appearance "white solid"
    :density 1.73
    :radioactive false}])

(easy-ingest crux data)

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

; DATALOG-QUERIES //////////////////////////////////////////////////////////////////////////////////////////////////////

; This basic query is returning all the elements that are defined as :element/metal. The :find clause tells Crux the variables you want to return
(crux/q (crux/db crux)
        '{:find [element]
          :where [[element :type :element/metal]]} )
;#{[:commodity/Pu] [:commodity/Au]}

; different flavours for writing the crux/q
(=
 (crux/q (crux/db crux)
         '{:find [element]
           :where [[element :type :element/metal]]})

 (crux/q (crux/db crux)
         {:find '[element]
          :where '[[element :type :element/metal]]})

 (crux/q (crux/db crux)
         (quote
           {:find [element]
            :where [[element :type :element/metal]]})))
; true

; Return the name of metal elements.
; Here we have bound the results of type :element/metal to e.
(crux/q (crux/db crux)
        '{:find [name]
          :where [[e :type :element/metal]
                  [e :common-name name]]} )
;#{["Gold"] ["Plutonium"]

; You can pull out as much data as you want into your result tuples by adding additional variables to the :find clause.
(crux/q (crux/db crux)
        '{:find [name density]
          :where [[e :density density]
                  [e :common-name name]]})
;#{["Nitrogen" 1.2506] ["Carbon" 2.267] ["Methane" 0.717] ["Borax" 1.73] ["Gold" 19.3] ["Plutonium" 19.816]}

; ------------------------ arguments -----------------------------------------------------------------------------------

; We could have done that before inside the :where clause, but using :args removes the need for hard-coding inside the query clauses.
(crux/q (crux/db crux)
        {:find '[name]
         :where '[[e :type t]
                  [e :common-name name]]
         :args [{'t :element/metal}]})

; ------------------------ filters -------------------------------------------------------------------------------------

(defn filter-type
  [type]
  (crux/q (crux/db crux)
          {:find '[name]
           :where '[[e :type t]
                    [e :common-name name]]
           :args [{'t type}]}))

(filter-type :element/metal)
; #{["Gold"] ["Plutonium"]}

(defn filter-appearance
  [description]
  (crux/q (crux/db crux)
          {:find '[name IUPAC]
           :where '[[e :common-name name]
                    [e :IUPAC-name IUPAC]
                    [e :appearance ?appearance]]
           :args [{'?appearance description}]}))

(filter-appearance "white solid")
; #{["Borax" "Sodium tetraborate decahydrate"]}

; You update your manifest with the latest badge. :)
(crux/submit-tx
 crux
 [[:crux.tx/put
   {:crux.db/id :manifest
    :pilot-name "Johanna"
    :id/rocket "SB002-sol"
    :id/employee "22910x2"
    :badges ["SETUP" "PUT" "DATALOG-QUERIES"]
    :cargo ["stereo" "gold fish" "slippers" "secret note"]}]])
; #:crux.tx{:tx-id 1614436347963394, :tx-time #inst "2019-12-17T15:53:16.058-00:00"}

; BITEMP ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

; CAS //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

; DELETE ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

; EVICT ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

