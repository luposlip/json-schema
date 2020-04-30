(ns json-schema.core
  (:require [cheshire.core :as json])
  (:import [org.json JSONObject JSONArray JSONTokener]
           [org.everit.json.schema.loader SchemaLoader]
           [org.everit.json.schema Schema]
           [org.everit.json.schema ValidationException]))

(defn- ^JSONTokener prepare-tokener
  "Prepares a JSONTokener instance for further processing"
  [input] 
  (JSONTokener. ^String (if (string? input)
                          input
                          (json/generate-string input))))

(defn- prepare-json
  "Prepares JSON instance based on input string, map or vector"
  [input]
  (if (or (associative? input)
          (and (string? input)
               (#{\{ \[} (first input))))
    (let [json-tokener (prepare-tokener input)]
      (if (or (vector? input)
              (= \[ (first input))) 
        (JSONArray. ^JSONTokener json-tokener)
        (JSONObject. ^JSONTokener json-tokener)))
    (throw (ex-info "Unsupported JSON input" {:input input}))))

(defn ^JSONObject prepare-schema
  "Prepares JSON Schema based on input string or map"
  [input]
  {:pre [(or (map? input)
             (string? input))]}
  (if (or (map? input)
          (and (string? input)
               (= \{ (first input))))
    (SchemaLoader/load (JSONObject. (prepare-tokener input)))
    (throw (ex-info "Unsupported Schema input" {:input input}))))

(defmulti validate
  "Validate JSON according to JSON Schema.

   If validation passes without errors, returns json.
   Throws ex-info with data on all errors.

   Supports draft-04 -> draft-07.

   To switch draft versions, simply use the according version notation
   in the $schema uri:
   http://json-schema.org/draft-04/schema
   http://json-schema.org/draft-06/schema
   http://json-schema.org/draft-07/schema

   JSON and JSON Schema params has to be input as either a
   JSON encoded string or EDN (map for both or vector for JSON).

   You can pass optional parameter :include-original-exception, in
   case you want the underlying exception to bubble up."
  (fn [schema _ & params]
    (cond (or (string? schema)
              (associative? schema))
          :string-or-edn
          (instance? Schema schema)
          :schema
          :else :error)))

(defmethod validate :string-or-edn
  [schema json & params]
  (apply (partial validate (prepare-schema schema) json) params))

(defmethod validate :error
  [schema _ & params]
  (throw (ex-info "Unsupported Schema input" {:input schema
                                              :params params})))

(defmethod validate :schema
  [schema json & params]
  (let [params (set params)]
    (try
      (.validate ^Schema schema (prepare-json json))
      json
      (catch ValidationException e 
        (let [errors (into [] (.getAllMessages ^ValidationException e))
              c (count errors)
              [n s] (if (> c 1)
                      [(format "%d " c) "s"]
                      ["" ""])]
          (throw (ex-info
                  (format "%sJSON Validation error%s:" n s)
                  {:errors errors}
                  (when (params :include-original-exception) e))))))))
