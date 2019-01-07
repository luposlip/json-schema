# Clojure JSON Schema Validator

A Clojure library for JSON validation according to JSON Schema https://json-schema.org.

It's very simple, and supports JSON Schema Draft-07.

## Usage

Currently this library can only be used to validate data based on a JSON Schema.

It has a single public function, `validate` that return nil when no errors are found.

All found errors will cause the function to throw an instance of `clojure.lang.ExceptionInfo`, which can be inspected with the help of `ex-data` and the likes.

If you're using Clojure 1.7 or newer, you can convert any Throwable to a map via `Throwable->map`.

To switch draft versions, simply use the according version notation in the `$schema` uri (in the root of the JSON document):

http://json-schema.org/draft-04/schema
http://json-schema.org/draft-06/schema
http://json-schema.org/draft-07/schema

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
  (validate json schema)
  (comment do whatever you only wanna do when JSON is valid)
  :success)
```

More usage examples can be seen in the tests.

## Thanks

Thanks to the maintainers of: https://github.com/everit-org/json-schema, on which this Clojure Library is based.

## License

Copyright © 2019 Henrik Mohr

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
