(ns json-schema.infer-test
  (:require [clojure.test :refer :all]
            [json-schema.infer :as t]))

(deftest infer-map
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :integer}}
          :required ["thing"]}
         (t/infer-strict {:thing 1} {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :integer}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {:thing {:quantity 1}} {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :number}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {:thing {:quantity 1.1}} {:schema-name "ent-1"})))
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantity" {:type :string}}
                                :required ["quantity"]}}
          :required ["thing"]}
         (t/infer-strict {:thing {:quantity "11.111,11"}} {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"things" {:type :array
                                 :items {:type :object
                                         :additionalProperties false
                                         :properties {"quantity" {:type :integer}}
                                         :required ["quantity"]}}}
          :required ["things"]}
         (t/infer-strict {:things [{:quantity 1} {:quantity 2}]} {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type :object
          :additionalProperties false
          :properties {"thing" {:type :object
                                :additionalProperties false
                                :properties {"quantities" {:type :array
                                                           :items {:type :number}}}
                                :required ["quantities"]}}
          :required ["thing"]}
         (t/infer-strict {:thing {:quantities [1.3 2.2 3.1]}} {:schema-name "ent-1"}))))

(deftest infer-array

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :integer}}
         (t/infer-strict [1] {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :number}}
         (t/infer-strict  [1.2] {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type :array
          :items {:type :string}}
         (t/infer-strict ["hej"] {:schema-name "ent-1"})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",	  
          :title "ent-1",
          :type :array,
          :items
          {:type :object,
           :additionalProperties false,
           :properties {"quantity" {:type :integer}},
           :required ["quantity"]}}
         (t/infer-strict [{:quantity 1}] {:schema-name "ent-1"}))))

(deftest infer-strict->json
  (is (= "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"ent-1\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"thing\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"quantities\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}},\"required\":[\"quantities\"]}},\"required\":[\"thing\"]}"
         (t/infer-strict->json {:thing {:quantities [1.3 2.2 3.1]}} {:schema-name "ent-1"}))))
