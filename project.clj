(defproject luposlip/json-schema "0.4.4"
  :description "Clojure library for JSON Schema validation and generation - Draft-07 compatible"
  :url "https://github.com/luposlip/json-schema"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [cheshire "5.13.0"]
                 [com.github.erosb/everit-json-schema "1.14.4"]]
  :global-vars {*warn-on-reflection* true}
  :repl-options {:init-ns json-schema.core}
  :profiles {:dev {:resource-paths ["test/resources"]}})
