![Clojure CI](https://github.com/luposlip/json-schema/workflows/Clojure%20CI/badge.svg?branch=master) [![Clojars Project](https://img.shields.io/clojars/v/luposlip/json-schema.svg)](https://clojars.org/luposlip/json-schema) [![Dependencies Status](https://versions.deps.co/luposlip/json-schema/status.svg)](https://versions.deps.co/luposlip/json-schema) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Downloads](https://versions.deps.co/luposlip/json-schema/downloads.svg)](https://versions.deps.co/luposlip/json-schema)

# Clojure JSON Schema Validator & Generator

```clojure
[luposlip/json-schema "0.4.5"]
```

A Clojure library for:
- validation of EDN or JSON data according to JSON Schema https://json-schema.org
- generation of JSON Schemas based on EDN data

Supports up until JSON Schema Draft-07, for validation. Generates Draft-07 Schemas.

## Usage: Validation

This library can be used to validate data (EDN or JSON strings) based on a JSON Schema.

It has a single public function, `validate` that return the validated data when no errors are found. This makes it very easy to incorporate validation in a pipeline (see pseudo example below).

All found errors will cause the function to throw an instance of `clojure.lang.ExceptionInfo`, which can be inspected with the help of `ex-data` and the likes.

If you're using Clojure 1.7 or newer, you can convert any Throwable to a map via `Throwable->map`.

To switch draft versions, simply use the according version notation in the `$schema` uri (in the root of the JSON document):

```
{"$schema": "http://json-schema.org/draft-04/schema", ...}

or:
{"$schema": "http://json-schema.org/draft-06/schema", ...}

or:
{"$schema": "http://json-schema.org/draft-07/schema", ...}
```

JSON and JSON Schema params has to be input as either a JSON encoded string or EDN (map for both or vector for JSON).

Example usage:

```clojure
(let [schema {:$schema "http://json-schema.org/draft-07/schema#"
              :id "https://luposlip.com/some-schema.json"
              :type "object"
              :properties {:id {:type "number"
                                :exclusiveMinimum 0}}
              :required [:id]}
      json "{\"id\": 0.001}"] ;; get from url, or anywhere
  (validate schema json)
  (comment do whatever you only wanna do when JSON is valid)
  :success)
```

With version 0.3.x you can reuse schemas via the [`$ref` attribute](https://json-schema.org/understanding-json-schema/structuring.html#reuse). See `/test/json-schema/core_test.clj#classpath-ref-resolution` for an example.

Pseudo-example for pipelining (note the reuse of the prepared schema):

```clojure
(let [schema (json-schema/prepare-schema
                (-> "resources/json-schema.json"
                    slurp
                    (cheshire.core/parse-string true)))]
  (->> huge-seq-of-edn-or-jsonstrings
       (map do-stuff-to-each-doc)
       (map do-even-more-to-each)
       (map (partial json-schema/validate schema))
     (lazily-save-docs-to-disk "/path/to/output-filename.ndjson")
     dorun))
```

More usage examples can be seen in the tests.

### Custom Format Validators

Check the unit test for examples on how to do this.

## Usage: Generation

The generation of JSON Schemas is pretty naive, but should work in many scenarios. The generated JSON Schemas doesn't use definitions, pointers or references, but is instead nested.

Schema generation is strict possible. This means that all found keys are set as required, and no other keys are allowed. But it's possible to add a `:optional` set of keys, which will be treated as non-required.

Please note that the explicit optionality will be schema wide, so keys used in different places in the structure having the same name will be treated the same! This might (probably) change in future releases, so you can supply a set keys for global optionality, or path vectors for specificity. A combination with wildcard paths could also be possible in a future release.

To generate a schema:

```clojure
(json-schema.infer/infer-strict
    {:title "ent-1"})
    {:things [{:quantity 1}]})
```

This will generate the following schema:

```clojure
{:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1"
          :type :object
          :additionalProperties false
          :properties {"things" {:type :array
                                 :items {:type :object
                                         :additionalProperties false
                                         :properties {"quantity" {:type :integer}}
                                         :required ["quantity"]}}}
          :required ["things"]}
```

**Implicit optionality** is supported by passing in multiple documents to `infer`:

```clojure
(json-schema.infer/infer
    {:title "ent-1"}
    {:things [{:quantity 1}]}
    {:things [{:quantity 1 :limit 2}]})
```

or if you have multiple documents pre-ordered in a sequence:

```clojure
(apply
    (partial json-schema.infer/infer {:title "ent-1"})
    [{:things [{:quantity 1}]}
     {:things [{:quantity 1 :limit 2}]}])
```

If you want optionality by allowing null values, this is the way:

```clojure
(json-schema.infer/infer
    {:title "ent-1"
     :nullable true}
    {:things [{:quantity 1}]
     :meta {:foo "bar}}
    {:things [{:quantity 1
               :limit 2}]
     :meta nil})
```

**Explicit optionality** is set like this:

```clojure
(json-schema.infer/infer-strict
    {:title "ent-1"
     :optional #{:meta}}
    {:things [{:quantity 1}]
     :meta 123})
```

If you want to allow **additional properties**, set `additional-props` to true:

```clojure
(json-schema.infer/infer-strict
    {:title "ent-1"
     :additional-props true}
    {:things [{:quantity 1}]
     :meta 123})
```

There's a helper function generating the Schema directly to a JSON string:

```clojure
(json-schema.infer/infer->json
    {:title "ent-1"}
    {:thing {:quantities [1.3 2.2 3.1]}})
```

More usage examples can be found in the tests.

NB: In Clojure any data is allowed as key in a map. This is not the case in JSON, where keys can only be string. Because of that all non-string keys are converted to strings in the generated JSON schema. Clojure data with other types of keys will still validate based on the generated schema.


### Future

A future release might allow for a configuration map, setting property attributes such as minimum and maximum values for numbers, length constraints for strings etc.

## deps.edn

If you use `tools.deps` (as opposed to Leiningen), you'll have to copy all dependencies from the Java library manually into `deps.edn`. This is due to [a bug in `tools.deps`](https://dev.clojure.org/jira/browse/TDEPS-46). Refer to [issue #1](https://github.com/luposlip/json-schema/issues/1) for more information.

## Thanks

To the maintainers of: https://github.com/everit-org/json-schema, on which _validation_ in this Clojure Library is based.

To the contributors!

## Copyright & License

Copyright (C) 2020-2024 Henrik Mohr

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
