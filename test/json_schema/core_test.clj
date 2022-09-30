(ns json-schema.core-test
  (:require [clojure.test :refer :all]
            [json-schema.core :as json]
            [clojure.java.io :as io])
  (:import [org.everit.json.schema ValidationException]
           clojure.lang.ExceptionInfo))

(deftest validate-json
  (testing "Basic JSON Schema and input"
    (let [json-schema-str "{\"$schema\": \"http://json-schema.org/draft-07/schema#\",
                          \"id\": \"https://luposlip.com/some-schema.json\",
                          \"type\": \"object\",
                          \"properties\": {\"id\" : {\"type\": \"integer\"}},
                          \"required\": [\"id\"]}"
          schema (json/prepare-schema json-schema-str)]

      (testing "Numbers are not valid input"
        (is (thrown-with-msg? Exception #"Unsupported Schema input"
                              (json/validate 1 2)))
        (is (thrown-with-msg? Exception #"Unsupported Schema input"
                              (json/validate 1 "2")))
        (is (thrown-with-msg? Exception #"Unsupported Schema input"
                              (json/validate "1" 2))))

      (testing "Schema has to be a map"
        (is (thrown-with-msg? Exception #"Unsupported Schema input"
                              (json/validate "1" "2"))))

      (testing "JSON has to be a map or an array"
        (is (thrown-with-msg? Exception #"Unsupported JSON input"
                              (json/validate schema "1"))))

      (testing "ID has to be a number"
        (is (thrown? RuntimeException (json/validate schema "{\"id\" : \"1\"}"))))

      (testing "Valid input as JSON string"
        (let [data "{\"id\": 1}"] (is (= data (json/validate schema data)))))

      (testing "Valid input as EDN"
        (let [data {:id 1}] (is (= data (json/validate schema data)))))

      (testing "Causing ValidationException included when validation fails"
        (let [schema {:$schema "http://json-schema.org/draft-07/schema#"
                      :id "https://luposlip.com/some-schema.json"
                      :type "object"
                      :properties {:id {:type "string"}}
                      :required [:id]}
              ex (try
                   (json/validate schema "{}" :include-original-exception)
                   (catch Throwable ex
                     ex))]
          (is (instance? ValidationException (.getCause ^Exception ex)))))

      (testing "Causing ExceptionInfo when validation fails"
        (let [schema {:$schema "http://json-schema.org/draft-07/schema#"
                      :id "https://luposlip.com/some-schema.json"
                      :type "object"
                      :properties {:id {:type "string"}}
                      :required [:id]}
              ex (try
                   (json/validate schema "{}")
                   (catch Throwable ex
                     ex))]
          (is (instance? ExceptionInfo ex)))))))


(deftest validate-exclusive-minimum
  (testing "Validate exclusiveMinimum (which is part of Draft-07"
    (let [json-schema-edn {:$schema "http://json-schema.org/draft-07/schema#"
                           :id "https://luposlip.com/some-other-schema.json"
                           :type "object"
                           :properties {:id {:type "number"
                                             :exclusiveMinimum 0}}
                           :required [:id]}
          schema (json/prepare-schema json-schema-edn)
          json-edn-valid {:id 0.001}
          json-edn-invalid {:id 0}]

      (testing "valid input VALIDATES"
        (is (= json-edn-valid (json/validate schema json-edn-valid))))

      (testing "valid input does NOT validate"
        (is (thrown?
             clojure.lang.ExceptionInfo
             (json/validate schema json-edn-invalid)))))))

(deftest definitions-and-pointers
  (testing "JSON Schema validation with definitions and JSON Pointers"
    (let [schema-str "{\"$schema\": \"http://json-schema.org/draft-07/schema#\",
         \"definitions\": {
                         \"address\": {
                                     \"type\": \"object\",
                                     \"properties\": {\"street_address\": { \"type\": \"string\" },
                                                    \"city\":           { \"type\": \"string\" },
                                                    \"state\":          { \"type\": \"string\" }},
                                     \"required\": [\"street_address\", \"city\", \"state\"]}},
         \"type\": \"object\",
         \"properties\": {\"billing_address\": { \"$ref\": \"#/definitions/address\" },
                          \"shipping_address\": { \"$ref\": \"#/definitions/address\" }}}"
          json "{\"shipping_address\": {
              \"street_address\": \"1600 Pennsylvania Avenue NW\",
              \"city\": \"Washington\",
              \"state\": \"DC\"},
              \"billing_address\": {
              \"street_address\": \"1st Street SE\",
              \"city\": \"Washington\",
              \"state\": \"DC\"}}"]
      (is (= json (json/validate schema-str json))))))

(deftest array-data
  (testing "Validating EDN data where root is a vector (ref. issue #3)"
    (let [json-schema-edn {:$schema "http://json-schema.org/draft-07/schema#"
                           :id "https://luposlip.com/some-other-schema.json"
                           :type "array"}
          schema (json/prepare-schema json-schema-edn)
          json-str-valid "[{\"some\":\"data\"}]"
          json-edn-valid [{:some "data"}]
          json-edn-invalid {:some "data"}]

      (testing "valid JSON string input VALIDATES"
        (is (= json-str-valid (json/validate schema json-str-valid))))

      (testing "valid EDN input VALIDATES"
        (is (= json-edn-valid (json/validate schema json-edn-valid))))

      (testing "invalid EDN input fails"
        (is (thrown-with-msg?
             Exception
             #"JSON Validation error"
             (json/validate schema json-edn-invalid)))))))

(deftest classpath-ref-resolution
  (testing "JSON Schema validation with classpath ref resolution"
    (let [valid-input {:address {:street_address "Penguin Avenue 15"
                                 :city "Vilnius"}}
          invalid-input {:address "Penguin Avenue 15, Vilnius"}]

      (testing "Absolute classpath refs"
        (let [schema-input (-> "ref/absolute/person.json" io/resource slurp)
              schema (json/prepare-schema schema-input {:classpath-aware? true})]

          (testing "valid EDN input for absolute classpath ref VALIDATES"
            (is (= valid-input (json/validate schema valid-input))))

          (testing "invalid EDN input for absolute classpath ref FAILS"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema invalid-input))))))

      (testing "Relative classpath refs"
        (let [schema-input (-> "ref/relative/person.json" io/resource slurp)
              schema (json/prepare-schema schema-input {:classpath-aware? true
                                                        :default-resolution-scope "classpath://ref/relative/"})]

          (testing "valid EDN input for relative classpath ref VALIDATES"
            (is (= valid-input (json/validate schema valid-input))))

          (testing "valid EDN input for relative classpath ref FAILS"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema invalid-input)))))))))

(deftest validate-list
  (testing "Validate EDN list as JSON array"
    (let [schema (json/prepare-schema {:$schema "http://json-schema.org/draft-04/schema"
                                       :type "array"})]

      (testing "Valid input as EDN vector"
        (is (= ["a" "b"] (json/validate schema ["a" "b"]))))

      (testing "Valid input as EDN list"
        (is (= '("a" "b") (json/validate schema '("a" "b"))))))))

(deftest validate-uuid
  (testing "Validate EDN where data is uuid"
    (let [uuid-validator (fn [value]
                           (when-not (uuid? (parse-uuid value))
                             (format "[%s] is not a valid UUID value" value)))
          json-str-valid "{\"id\" : \"7c0fcade-fdcc-4d48-86a7-da55ebb82f04\"}"
          json-str-invalid "{\"id\" : \"Tc0fcade-fdcc-4d48-86a7-da55ebb82f04\"}"
          json-str-empty "{\"id\" : \"\"}"]

      (testing "Class path unaware"
        (let [schema (json/prepare-schema {:$schema "http://json-schema.org/draft-04/schema"
                                           :id         "https://luposlip.com/some-other-schema.json"
                                           :type       "object"
                                           :properties {:id {:type    "string"
                                                             :format  "uuid"}}}
                                          {:format-validators {"uuid" uuid-validator}})]
          (testing "Valid uuid"
            (is (= json-str-valid (json/validate schema json-str-valid))))

          (testing "Invalid uuid"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema json-str-invalid))))

          (testing "Empty instead of uuid"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema json-str-empty))))))

      (testing "Class path aware"
        (let [schema (json/prepare-schema {:$schema "http://json-schema.org/draft-04/schema"
                                           :id         "https://luposlip.com/some-other-schema.json"
                                           :type       "object"
                                           :properties {:id {:type    "string"
                                                             :format  "uuid"}}}
                                          {:classpath-aware? true
                                           :default-resolution-scope "classpath://ref/relative/"
                                           :format-validators {"uuid" uuid-validator}})]
          (testing "Valid uuid"
            (is (= json-str-valid (json/validate schema json-str-valid))))

          (testing "Invalid uuid"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema json-str-invalid))))

          (testing "Empty instead of uuid"
            (is (thrown-with-msg?
                  Exception
                  #"JSON Validation error"
                  (json/validate schema json-str-empty)))))))))
