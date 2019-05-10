(ns json-schema.core-test
  (:require [clojure.test :refer :all]
            [json-schema.core :as json]))

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
        (let [data {:id 1}] (is (= data (json/validate schema data))))))))


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

