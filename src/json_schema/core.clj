(ns json-schema.core
  (:require [cheshire.core :as json])
  (:import [java.io Reader StringReader]
           [org.json JSONObject JSONArray JSONTokener]
           [org.everit.json.schema.loader
            SchemaLoader SchemaClient SchemaLoader$SchemaLoaderBuilder]
           [org.everit.json.schema Schema]
           [org.everit.json.schema ValidationException]))

(defn- ^JSONTokener prepare-tokener
  "Prepares a JSONTokener instance for further processing"
  [input]
  (JSONTokener.
   ^Reader (StringReader.
            ^String (if (string? input)
                      input
                      (json/generate-string input)))))

(defn- prepare-json
  "Prepares JSON instance based on input string, map or vector"
  [input]
  (if (or (associative? input)
          (and (string? input)
               (#{\{ \[} (first input))))
    (let [json-tokener ^JSONTokener(prepare-tokener input)]
      (if (or (vector? input)
              (= \[ (first input))) 
        (JSONArray. json-tokener)
        (JSONObject. json-tokener)))
    (throw (ex-info "Unsupported JSON input" {:input input}))))

(defn- assert-schema-input-valid! [input]
  (let [is-input-valid? (or (map? input)
                      (and (string? input)
                           (= \{ (first input))))]
    (when-not is-input-valid?
      (throw (ex-info "Unsupported Schema input" {:input input})))))

(defn ^JSONObject prepare-schema*
  "Prepares JSON Schema based on input string or map. An optional parameter map
  can be supplied to refer to relative file schemas via classpath in $ref fields.

  Setting :classpath-aware? to true enables absolute classpath resolution.
  (prepare-schema* input {:classpath-aware? true})

  Setting :default-resolution-scope enables relative classpath resolution.
  Make sure to not use HTTP URIs in schema $id fields, as this
  will cause it to resort to default $ref resolution via HTTP.
  (prepare-schema* input {:classpath-aware? true
                          :default-resolution-scope \"classpath://schemas/\"})"
  ([input]
   (assert-schema-input-valid! input)
   (SchemaLoader/load (JSONObject. (prepare-tokener input))))
  ([input params]
   (assert-schema-input-valid! input)
   (if-not (:classpath-aware? params)
     (prepare-schema* input)
     (let [resolution-scope (:default-resolution-scope params)
           set-resolution-scope (fn [^SchemaLoader$SchemaLoaderBuilder builder]
                                  (if resolution-scope
                                    (.resolutionScope builder ^String resolution-scope)
                                    builder))
           schema-loader (-> (SchemaLoader/builder)
                             (.schemaClient (SchemaClient/classPathAwareClient))
                             ^SchemaLoader$SchemaLoaderBuilder (set-resolution-scope)
                             (.schemaJson
                              ^JSONObject (JSONObject. (prepare-tokener input)))
                             (.build))]
       (.build (.load schema-loader))))))

(def ^JSONObject prepare-schema (memoize prepare-schema*))

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
