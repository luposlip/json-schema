(ns json-schema.infer
  (:require [clojure.core.reducers :as r]
            [cheshire.core :as json]))

(defn- init-schema [{:keys [schema-name uri description]}]
  (merge {:$schema "http://json-schema.org/draft-07/schema#"
          :title schema-name}
         (when (string? uri) {:uri uri})
         (when (string? description) {:description description})))

(declare data-type)

(defn infer-strict
  "Naive schema inference from associative data
  
   Optional params:
   - schema-name - translates to schema title
   - uri - uri for the schema
   - description -  description for the schema
   - schema - if you already started on a schema def
   - type-meta - extra meta data for properties (WIP!)"
  [data & [{:keys [schema-name schema uri description type-meta] :as params}]]
  {:pre [(or (associative? data)
             (or (nil? schema-name) (string? schema-name)))]}
  (let [sch (if schema-name
              (init-schema params)
              (or schema {}))]
    (cond (map? data) (merge sch
                             {:type :object
                              :additionalProperties false
                              :properties (zipmap (keys data) (mapv (comp data-type second) data))
                              :required (vec (keys data))})
          (vector? data) (merge sch
                                {:type :array
                                 :items (trampoline infer-strict (first data))})
          :else (merge sch (data-type data)))))

(defn- data-type
  "Return af JSON Schema type map based on input"
  [data]
  (cond
    (integer? data) {:type :integer}
    (number? data) {:type :number}
    (string? data) {:type :string}
    (associative? data) (trampoline infer-strict data)
    :else (throw (ex-info "Not yet supporting data-type" {:data data}))))

(defn infer-strict->json
  "A helper function that returns inferred schema as JSON"
  [data params]
  (-> data (infer-strict params) json/encode))
