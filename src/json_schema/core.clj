(ns json-schema.core
  (:require [cheshire.core :as json])
  (:import [org.json JSONObject JSONArray JSONTokener]
           [org.everit.json.schema.loader SchemaLoader]
           [org.everit.json.schema ValidationException]))

(defn- ^JSONTokener prepare-tokener
  "Prepares a JSONTokener instance for further processing"
  [input] 
  (JSONTokener. ^String (if (string? input)
                          input
                          (json/generate-string input))))

(defn- ^JSONObject prepare-schema
  "Prepares JSON Schema based on input string or map"
  [input]
  {:pre [(or (map? input)
             (and (string? input)
                  (= \{ (first input))))]} 
  (SchemaLoader/load (JSONObject. (prepare-tokener input))))

(defn- prepare-json
  "Prepares JSON instance based on input string, map or vector"
  [input]
  {:pre [(or (map? input) (vector? input)
             (and (string? input)
                  (#{\{ \[} (first input))))]}
  (let [json-tokener (prepare-tokener input)]
    (condp = (first input) 
      \[ (JSONArray. ^JSONTokener json-tokener)
      (JSONObject. ^JSONTokener json-tokener))))

(defn validate
  "Validate JSON according to JSON Schema.

   If validation passes without errors, returns nil.
   Throws ex-info with data on all errors.

   Supports draft-04 -> draft-07.

   To switch draft versions,
   simply use the according version notation in the $schema uri:
   http://json-schema.org/draft-04/schema
   http://json-schema.org/draft-06/schema
   http://json-schema.org/draft-07/schema

   JSON and JSON Schema params has to be input as either a
   JSON encoded string or EDN (map for both or vector for JSON)."
  [json json-schema] 
  (try
    (.validate (prepare-schema json-schema) (prepare-json json))
    (catch ValidationException e 
      (throw (ex-info
              "JSON Validation error"
              {:errors (into [] (.getAllMessages ^ValidationException e))})))))
