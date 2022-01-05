(defproject luposlip/json-schema "0.3.3"
  :description "Clojure library for JSON Schema validation and generation - Draft-07 compatible"
  :url "https://github.com/luposlip/json-schema"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :repositories {"jitpack" {:url "https://jitpack.io"}}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [cheshire "5.10.1"]
                 [com.github.everit-org.json-schema/org.everit.json.schema "1.14.0"]]
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init-ns json-schema.core}
  :profiles {:dev {:resource-paths ["test/resources"]}})
