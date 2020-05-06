(ns json-schema.infer
  (:require [cheshire.core :as json]))

(defn- init-schema [{:keys [title uri description]}]
  (merge {:$schema "http://json-schema.org/draft-07/schema#"
          :title title}
         (when (string? uri) {:uri uri})
         (when (string? description) {:description description})))

(declare data-type)

(defn sanitize-key [k]
  (if (or (keyword? k) (symbol? k))
    (name k)
    (str k)))

(defn infer-strict
  "Naive but strict schema inference from associative data.
   Strict in the sense that everything is required, and nothing
   else is allowed.

   Params:
     optional    - keys that shouldn't be required
  
   Optional params:
     title       - schema title
     description - schema description
     uri         - schema uri
     schema      - continue building on schema"
  [{:keys [title schema optional] :as params} data]
  {:pre [(and
          (or (associative? data)
              (or (nil? title) (string? title)))
          (or (nil? optional) (set? optional)))]}
  (let [sch (if (and title (not schema))
              (init-schema params)
              (or schema {}))]
    (cond (map? data) (merge sch
                             (let [sks (map sanitize-key (keys data))
                                   d {:type :object
                                      :additionalProperties false
                                      :properties (zipmap sks (mapv (comp (partial infer-strict (dissoc params :title :description :uri)) second) data))
                                      :required (vec (if optional
                                                       (map sanitize-key (remove optional (keys data)))
                                                       sks))}]
                               (println 'optional optional)
                               (println 'sks sks)
                               d
                               ))
          (vector? data) (merge sch
                                {:type :array
                                 :items (trampoline (partial infer-strict (dissoc params :title :description :uri)) (first data))})
          :else (merge sch (data-type (partial infer-strict (dissoc params :title :description :uri)) data)))))

(defn- data-type
  "Return af JSON Schema type map based on input"
  [recur-fn data]
  (cond
    (integer? data) {:type :integer}
    (number? data) {:type :number}
    (string? data) {:type :string}
    (boolean? data) {:type :boolean}
    (inst? data) {:type :string
                  :minLength 20
                  :maxLength 20}
    (associative? data) (trampoline recur-fn data)
    :else (throw (ex-info "Not yet supporting data-type" {:data data}))))

(defn infer->json
  "A helper function that returns inferred schema as JSON"
  [params data]
  (-> (infer-strict params data) json/encode))

(defn ^:deprecated infer-strict->json
  "Use infer->schema instead"
  [data params]
  (infer->json params data))

