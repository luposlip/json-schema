[![CircleCI](https://circleci.com/gh/luposlip/json-schema/tree/master.svg?style=svg)](https://circleci.com/gh/luposlip/json-schema/tree/master) [![Clojars Project](https://img.shields.io/clojars/v/luposlip/json-schema.svg)](https://clojars.org/luposlip/json-schema) [![Dependencies Status](https://versions.deps.co/luposlip/json-schema/status.svg)](https://versions.deps.co/luposlip/json-schema) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Downloads](https://versions.deps.co/luposlip/json-schema/downloads.svg)](https://versions.deps.co/luposlip/json-schema)

# Clojure JSON Schema Validator & Generator

```clojure
[luposlip/json-schema "0.2.1"]
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

## Usage: Generation

The generation of JSON Schemas is pretty naive, but should work in many scenarios. The generated JSON Schemas doesn't use definitions, pointers or references, but is instead nested.

Currently only strict Schema generation is possible. This means that all found keys are set as required, and no other keys are allowed.

To generate a schema:

```clojure
(json-schema.infer/infer-strict
    {:things [{:quantity 1}]}
    {:schema-name "ent-1"})
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

There's a helper function generating the Schema directly to a JSON string:

```clojure
(json-schema.infer/infer-strict->json
    {:thing {:quantities [1.3 2.2 3.1]}}
    {:schema-name "ent-1"})
```

More usage examples can be found in the tests.

NB: In Clojure any data is allowed as key in a map. This is not the case in JSON, where keys can only be string. Because of that all non-string keys are converted to strings in the generated JSON schema. Clojure data with other types of keys will still validate based on the generated schema.


### Future

Only a single input sample is currently used to generate the Schema. In a future release the generation will allow a list of sample documents, and use their potential differences to infer the value of certain Schema property attributes. In example automatically setting `required` to either `true` or `false`.

Also a future release will allow for a configuration map, setting property attributes such as minimum and maximum values for numbers, length constraints for strings etc.

## deps.edn

If you use `tools.deps` (as opposed to Leiningen), you'll have to copy all dependencies from the Java library manually into `deps.edn`. This is due to [a bug in `tools.deps`](https://dev.clojure.org/jira/browse/TDEPS-46). Refer to [issue #1](https://github.com/luposlip/json-schema/issues/1) for more information.

## Thanks

To the maintainers of: https://github.com/everit-org/json-schema, on which _validation_ in this Clojure Library is based.

## Copyright & License

Copyright (C) 2020 Henrik Mohr

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0
            
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
