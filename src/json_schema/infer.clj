(ns json-schema.infer
  (:require [clojure.walk :as walk]
            [cheshire.core :as json]))


(defn eradicate-lists [m]
  (if (or (associative? m)
          (sequential? m))
    (walk/postwalk
     #(if (sequential? %)
        (vec %) %)
     m)
    m))

(defn key-paths
  "Returns a sequence of all key paths in a given map using DFS walk.
   Converts lists to vectors to enable associative lookup.

   Based on code from:
   https://dnaeon.github.io/clojure-map-ks-paths/"
  [m]
  {:pre [(map? m)]}
  (let [m (eradicate-lists m)]
    (letfn [(children [node]
              (let [v (get-in m node)]
                (cond (map? v)
                      (map (fn [x] (conj node x)) (keys v))
                      (and (sequential? v)
                           (every? map? v))
                      (let [d (apply merge (filter map? v))]
                        (map (fn [x] (conj node 0 x)) (keys d)))
                      :else [])))
            (branch? [node] (-> (children node) seq boolean))]
      (->> (keys m)
           (map vector)
           (mapcat (partial tree-seq branch? children))
           (remove #(and (-> % last keyword?)
                         (-> % count (not= 1))))
           vec))))

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

(defn- required-path [path]
  (-> path butlast butlast vec (conj :required)))

(defn- update-required [data {:keys [optional manipulator]} path]
  (if (-> path count (> 1))
    (let [k (-> path last)
          optional? (-> optional (get (keyword k)) boolean)]
      (update-in data
                 (required-path path)
                 (fnil (if optional? disj manipulator) #{})
                 (sanitize-key k)))
    data))

(defn- assoc-in-from-either [data path d1 d2]
  (let [type? (> (count path) 1)
        ignore-one? (some empty? [d1 d2])
        root-keeper (fn [data]
                      (let [pd (map #(get-in % path) [d1 d2])]
                        (if (and (some nil? pd)
                                 (some (comp not nil?) pd))
                          (assoc-in data path (first (remove nil? pd)))
                          data)))
        update-type (fn [data]
                      (if type?
                        (let [type-path (conj path :type)]
                          (assoc-in data type-path
                                    (if ignore-one?
                                      (-> (remove empty? [d1 d2])
                                          first
                                          (get-in type-path))
                                      (into (get-in d1 type-path #{:null})
                                            (get-in d2 type-path #{:null})))))
                        data))]
    (-> data
        root-keeper
        update-type)))

(defn- unroot [params]
  (dissoc params :title :description :uri))

(declare infer) ;; bc/o mutual recursion

(defn infer-strict
  "Naive but strict schema inference from associative data.
   Strict in the sense that everything is required, and nothing
   else is allowed - unless additional-props is explicitly set to true.

   Params:
     optional         - keys that shouldn't be required
     additional-props - if additional properties are allowed in schema objects
  
   Optional params:
     title       - schema title
     description - schema description
     uri         - schema uri
     schema      - continue building on schema"
  [{:keys [title schema optional additional-props] :as params} data]
  {:pre [(and
          (or (associative? data)
              (or (nil? title) (string? title)))
          (or (nil? optional) (set? optional)))]}
  (let [sch (if title
              (init-schema params)
              (or schema {}))]
    (cond (map? data) (merge sch
                             (let [sks (map sanitize-key (keys data))]
                               {:type #{:object}
                                :additionalProperties (or additional-props false)
                                :properties (zipmap sks
                                                    (mapv
                                                     (comp (partial infer-strict
                                                                    (unroot params))
                                                           second)
                                                     data))
                                :required (set (if optional
                                                 (map sanitize-key (remove optional (keys data)))
                                                 sks))}))
          (vector? data) (merge sch
                                {:type #{:array}
                                 :items (trampoline
                                         apply
                                         (partial infer (unroot params))
                                         data)})
          :else
          (merge sch
                 (data-type
                  (assoc params :recur-fn (partial infer-strict (unroot params)))
                  data)))))

(defn infer
  "Schema inference from multiple documents of associative data.
   Takes n sample documents as input, and returns a schema that allows only
   data represented in at least one of the documents.

   Data that is present in all documents will be required.
   Data only represented in a subset of documents will be optional.
   Data not represented at all, will not be allowed.

   If a single document is passed, infer-strict is used directly.

   Params:
     title       - schema title
     description - schema description
     uri         - schema uri
     schema      - continue building on schema
     optional    - keys that shouldn't be required
     nullable    - optionality by nullability"
  [params & docs]
  (if (= 1 (count docs))
    (infer-strict params (first docs))
    (let [schemas (map (partial infer-strict params) docs)]
      (reduce
       (fn [a i]
         (let [f (frequencies (into (key-paths a) (key-paths i)))]
           (reduce-kv
            (fn [a2 k v]
              (if (= 2 v) ;; required
                (-> a2
                    (assoc-in-from-either k a i)
                    (update-required (assoc params :manipulator conj) k))
                (-> a2 ;; optional
                    (assoc-in-from-either k a i)
                    (update-required (assoc params :manipulator disj) k))))
            a
            f)))
       {}
       schemas))))

(defn- data-type
  "Return af JSON Schema type map based on input"
  [{:keys [recur-fn nullable] :as params} data]
  (cond
    (integer? data) {:type #{:integer}}
    (number? data) {:type #{:number}}
    (string? data) {:type #{:string}}
    (boolean? data) {:type #{:boolean}}
    (inst? data) {:type #{:string}
                  :minLength 20
                  :maxLength 20}
    (associative? data) (trampoline recur-fn data)
    (and (nil? data) nullable) {:type #{:null}}
    :else (throw (ex-info "Not yet supporting data-type" {:data data
                                                          :params params}))))

(defn infer->json
  "A helper function that returns inferred schema as JSON"
  [params data]
  (->> data (infer-strict params) json/encode))

