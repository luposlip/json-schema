(defproject luposlip/json-schema "0.1.0"
  :description "Clojure library for validating via JSON Schema - Draft-07 compatible"
  :url "https://github.com/luposlip"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories {"jitpack.io" {:url "https://jitpack.io"}}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.1"]
                 [com.github.everit-org.json-schema/org.everit.json.schema "1.10.0"]]
  :repl-options {:init-ns json-schema.core})
