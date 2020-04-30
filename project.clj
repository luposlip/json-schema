(defproject luposlip/json-schema "0.2.1"
  :description "Clojure library for JSON Schema validation and generating - Draft-07 compatible"
  :url "https://github.com/luposlip/json-schema"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :repositories {"jitpack" {:url "https://jitpack.io"}}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.0"]
                 [com.github.everit-org.json-schema/org.everit.json.schema "1.11.1"]]
  :repl-options {:init-ns json-schema.core})
