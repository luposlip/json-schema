(ns json-schema.infer
  (:require [clojure.core.reducers :as r]
            [cheshire.core :as json]))

(defn init-schema [schema-name data]
  {:$schema "http://json-schema.org/draft-07/schema#"
   :title schema-name})

(declare data-type)

(defn infer-strict
  "SIMPLE schema inference from associative data
   Schema name translates to title.
  
   Optional params:
   - uri - uri for the schema
   - description -  description for the schema
   - type-meta - extra meta data for properties (super simple..)
   - schema - if you already started on a schema def"
  [data & [{:keys [schema-name schema uri description type-meta]}]]
  {:pre [(or (associative? data)
             (or (nil? schema-name) (string? schema-name)))]}
  (let [sch (if schema-name
              (init-schema schema-name data)
              (or schema {}))]
    (cond (map? data)
          (merge sch
                 {:type :object
                  :additionalProperties false
                  :properties (zipmap (keys data) (mapv (comp data-type second) data))
                  :required (vec (keys data))}
                 (when (string? uri) {:uri uri})
                 (when (string? description) {:description description}))
          (and schema-name (map? (first data)))
          (throw (ex-info "Not yet supporting root vectors with map values" {:schema-name schema-name
                                                                             :data data}))
          (vector? data)
          (merge sch
                 {:type :array
                  :items (trampoline infer-strict (first data))})
          :else (merge sch (data-type data)))))

(defn data-type
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
