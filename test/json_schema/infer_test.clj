(ns json-schema.infer-test
  (:require [clojure.test :refer :all]
            [json-schema.core :as v]
            [json-schema.infer :as t])
  (:import clojure.lang.ExceptionInfo))

(deftest infer-map
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"thing" {:type #{:integer}}}
          :required #{"thing"}}
         (t/infer-strict {:title "ent-1"} {:thing 1})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"thing" {:type #{:object}
                                :additionalProperties false
                                :properties {"quantity" {:type #{:integer}}}
                                :required #{"quantity"}}}
          :required #{"thing"}}
         (t/infer-strict {:title "ent-1"} {:thing {:quantity 1}})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"thing" {:type #{:object}
                                :additionalProperties false
                                :properties {"quantity" {:type #{:number}}}
                                :required #{"quantity"}}}
          :required #{"thing"}}
         (t/infer-strict {:title "ent-1"} {:thing {:quantity 1.1}})))
  
  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"thing" {:type #{:object}
                                :additionalProperties false
                                :properties {"quantity" {:type #{:string}}}
                                :required #{"quantity"}}}
          :required #{"thing"}}
         (t/infer-strict {:title "ent-1"} {:thing {:quantity "11.111,11"}})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"things" {:type #{:array}
                                 :items {:type #{:object}
                                         :additionalProperties false
                                         :properties {"quantity" {:type #{:integer}}}
                                         :required #{"quantity"}}}}
          :required #{"things"}}
         (t/infer-strict {:title "ent-1"} {:things [{:quantity 1} {:quantity 2}]})))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#"
          :title "ent-1" 
          :type #{:object}
          :additionalProperties false
          :properties {"thing" {:type #{:object}
                                :additionalProperties false
                                :properties {"quantities" {:type #{:array}
                                                           :items {:type #{:number}}}}
                                :required #{"quantities"}}}
          :required #{"thing"}}
         (t/infer-strict {:title "ent-1"} {:thing {:quantities [1.3 2.2 3.1]}}))))

(deftest infer-array

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type #{:array}
          :items {:type #{:integer}}}
         (t/infer-strict {:title "ent-1"} [1])))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type #{:array}
          :items {:type #{:number}}}
         (t/infer-strict {:title "ent-1"} [1.2])))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",
          :title "ent-1"
          :type #{:array}
          :items {:type #{:string}}}
         (t/infer-strict {:title "ent-1"} ["hej"])))

  (is (= {:$schema "http://json-schema.org/draft-07/schema#",	  
          :title "ent-1",
          :type #{:array},
          :items
          {:type #{:object},
           :additionalProperties false,
           :properties {"quantity" {:type #{:integer}}},
           :required #{"quantity"}}}
         (t/infer-strict {:title "ent-1"} [{:quantity 1}]))))

(deftest infer-strict->json
  (is (= "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"ent-1\",\"type\":[\"object\"],\"additionalProperties\":false,\"properties\":{\"thing\":{\"type\":[\"object\"],\"additionalProperties\":false,\"properties\":{\"quantities\":{\"type\":[\"array\"],\"items\":{\"type\":[\"number\"]}}},\"required\":[\"quantities\"]}},\"required\":[\"thing\"]}"
         (t/infer->json {:title "ent-1"} {:thing {:quantities [1.3 2.2 3.1]}}))))

(deftest infer-and-validate
  (let [data {:things [{:quantity 1} {:quantity 2}]
              :yearly {2020 1
                       2021 2
                       2022 2.5
                       2023 3
                       2024 (/ 1 3)}}
        schema (t/infer-strict {:title "ent-1"} data)]
    (is (= data (v/validate schema data)))))

(deftest infer-strict-optional
  (let [data {:things [{:quantity 1} {:quantity 2}]
              :yearly {2020 1
                       2021 2
                       2022 2.5
                       2023 3
                       2024 (/ 1 3)}
              :meta {:this "is"
                     :optional? "yesyes"}}
        schema (t/infer-strict {:title "ent-1"
                                :optional #{:meta}}
                               data)]
    (is (= data (v/validate schema data)))
    (is (= (dissoc data :meta) (v/validate schema (dissoc data :meta))))
    (is (thrown? ExceptionInfo (v/validate schema (assoc data :meta2 {:this "is" :not "allowed"}))))))

(deftest infer-multiple

  (testing "implicit root key optionality by samples"
    (let [data [{:things [{:quantity 1} {:quantity 2}]
                 :meta {:this "is"
                        :optional? "yesyes"}}
                {:things [{:quantity 1} {:quantity 2}]}]
          schema (apply (partial t/infer {:title "ent-1"}) data)]
      (is (= (first data) (v/validate schema (first data))))
      (is (= (second data) (v/validate schema (second data))))
      (is (thrown? ExceptionInfo (v/validate schema (assoc (second data) :foo [{:bar "baz"}]))))))

  (testing "implicit child key optionality by samples"
    (let [data [{:things [{:quantity 1} {:quantity 2}]
                 :meta {:this "is"
                        :optional? "yesyes"}}
                {:things [{:quantity 1}
                          {:quantity 2 :limit 3}]}]
          schema (apply (partial t/infer {:title "ent-1"}) data)]
      (is (= (first data) (v/validate schema (first data))))
      (is (= (second data) (v/validate schema (second data))))
      (is (thrown? ExceptionInfo
                   (v/validate schema (assoc (second data) :foo {:bar "baz"}))))
      (is (thrown? ExceptionInfo
                   (v/validate schema {:things [{:quantity 1}
                                                {:quantity 2 :limits 3}]})))))

  (testing "explicit optionality in child structure"
    (let [data [{:things [{:quantity 1} {:quantity 2}]
                 :meta {:this "is"
                        :optional? "yesyes"}}
                {:things [{:quantity 1}
                          {:quantity 2 :limit 3}]}]
          schema (apply (partial t/infer {:title "ent-1"
                                          :optional #{:quantity}}) data)]
      (is (= (first data) (v/validate schema (first data))))
      (is (= (second data) (v/validate schema (second data))))
      (is (= {:things [{:quantity 1} {:limit 3}]}
             (v/validate schema {:things [{:quantity 1} {:limit 3}]})))
      (is (thrown? ExceptionInfo
                   (v/validate schema
                               (assoc (second data)
                                      :quantity 0)))))))

(deftest implicit-optionality-by-nullability
  (let [data [{:foo {:bar "baz"}}
              {:foo nil}]
        schema (apply (partial t/infer {:title "ent-1"
                                        :nullable true})
                      data)]
    (is (= (first data) (v/validate schema (first data))))
    (is (= (second data) (v/validate schema (second data))))
    (is (thrown? ExceptionInfo
                 (v/validate schema
                             (assoc (second data) ;; not allowed to change type
                                    :foo [{:bar "baz"}]))))))

(deftest implicit-optionality-many-siblings
  (let [d [{:cat "Something",
            :code "1",
            :headline "Something"}
           {:cat "More something",
            :code "2",
            :headline "Yes my boys"}
           {:cause "Just cause",
            :msg-1 "Message 1",
            :cat "Caterpillar",
            :code "3",
            :rule "nifty rule",
            :headline "None for now",
            :msg-2 "Message 2"}]
        schema (apply (partial t/infer {:title "ent-1" :nullable true}) d)]
    (is (= {:required #{"cat" "code" "headline"},
            :additionalProperties false,
            :properties
            {"cat" {:type #{:string}},
             "code" {:type #{:string}},
             "headline" {:type #{:string}},
             "rule" {:type #{:string :null}},
             "msg-1" {:type #{:string :null}}
             "msg-2" {:type #{:string :null}}
             "cause" {:type #{:string :null}}},
            :title "ent-1",
            :type #{:object},
            :$schema "http://json-schema.org/draft-07/schema#"}
           schema))
    (is (= (map (partial v/validate schema) d)
           d))))

(deftest infer-additional-properties
  (let [data {:things [{:quantity 1} {:quantity 2}]
              :yearly {2020 1
                       2021 2
                       2022 2.5
                       2023 3
                       2024 (/ 1 3)}}
        schema (t/infer-strict {:title "ent-1"} data)
        more-schema (t/infer-strict {:title "ent-1" :additional-props true} data)
        more-data (assoc data :more-data {:hey "you!"})]
    (is (thrown? ExceptionInfo (v/validate schema more-data)))
    (is (= more-data (v/validate more-schema more-data)))))
