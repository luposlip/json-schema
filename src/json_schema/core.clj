(ns json-schema.core
  (:require [cheshire.core :as json])
  (:import [java.io Reader StringReader]
           [org.json JSONObject JSONArray JSONTokener]
           [org.everit.json.schema.loader
            SchemaLoader SchemaClient SchemaLoader$SchemaLoaderBuilder]
           [org.everit.json.schema Schema]
           [org.everit.json.schema ValidationException]
           [org.everit.json.schema FormatValidator]
           [java.util Optional]))

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
          (sequential? input)
          (and (string? input)
               (#{\{ \[} (first input))))
    (let [json-tokener ^JSONTokener (prepare-tokener input)]
      (if (or (sequential? input)
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

(defn- format-validator [key validation-fn]
  (reify FormatValidator
    (formatName [_] key)
    (validate [_ value]
      (if-let [validation-error-message (validation-fn value)]
        (Optional/of validation-error-message)
        (Optional/empty)))))

(defn ^JSONObject prepare-schema*
  "Prepares JSON Schema based on input string or map. An optional parameter map
  can be supplied to refer to relative file schemas via classpath in $ref fields.

  Setting :format-validators to map of format name and validation function adds
  them as custom format validators.
  Validator takes value and returns validation error message or nil if success
  (prepare-schema* input {
    :format-validators {\"uuid\" (fn [value]
                                   (when-not (uuid? (parse-uuid value))
                                     (format \"[%s] is not a valid UUID value\" value)))}})

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
   (let [set-format-validators (fn [^SchemaLoader$SchemaLoaderBuilder builder]
                                 (doseq [[k validation-fn] (:format-validators params)]
                                   (.addFormatValidator builder (format-validator k validation-fn)))
                                 builder)]
     (if-not (:classpath-aware? params)
       (.build (.load (-> (SchemaLoader/builder)
                          ^SchemaLoader$SchemaLoaderBuilder (set-format-validators)
                          (.schemaJson ^JSONObject (JSONObject. (prepare-tokener input)))
                          (.build))))
       (let [resolution-scope (:default-resolution-scope params)
             set-resolution-scope (fn [^SchemaLoader$SchemaLoaderBuilder builder]
                                    (if resolution-scope
                                      (.resolutionScope builder ^String resolution-scope)
                                      builder))
             schema-loader (-> (SchemaLoader/builder)
                               (.schemaClient (SchemaClient/classPathAwareClient))
                               ^SchemaLoader$SchemaLoaderBuilder (set-resolution-scope)
                               ^SchemaLoader$SchemaLoaderBuilder (set-format-validators)
                               (.schemaJson ^JSONObject (JSONObject. (prepare-tokener input)))
                               (.build))]
         (.build (.load schema-loader)))))))

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
