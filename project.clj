(defproject luposlip/json-schema "0.4.1"
  :description "Clojure library for JSON Schema validation and generation - Draft-07 compatible"
  :url "https://github.com/luposlip/json-schema"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.11.0"]
                 [org.json/json "20230227"]
                 [com.github.erosb/everit-json-schema "1.14.1" :exclusions [org.json/json]]]
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init-ns json-schema.core}
  :profiles {:dev {:resource-paths ["test/resources"]}})
