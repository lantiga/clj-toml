(ns clj-toml.core-test
  (:use clojure.test
        [clojure.instant :only [read-instant-timestamp]]
        clj-toml.core))

(deftest comment-test
  (testing "Comments"
    (is (= (parse-string "#just a comment line
                          foo = \"bar\" # and one more comment")
           {"foo" "bar"}))))

(deftest numbers-test
  (testing "Numbers"
    (is (= (parse-string "integer = 3_0
                          negative_integer = -3_0
                          float = 3_0.0
                          float_exp = 1e1_0
                          negative_float = -3.0")
           {"integer" 30
            "negative_integer" -30
            "float" 30.0
            "float_exp" 1e10
            "negative_float" -3.0}))))

(deftest datetime-test
  (testing "Datetime"
    (is (= (parse-string "mydob = 1975-10-03T16:20:00Z # and a comment, just because")
           {"mydob" (read-instant-timestamp "1975-10-03T16:20:00Z")}))
    (is (= (parse-string "mydob = 1975-10-03T16:20:00.999999Z # and a comment, just because")
           {"mydob" (read-instant-timestamp "1975-10-03T16:20:00.999999Z")}))
    (is (= (parse-string "mydob = 1975-10-03T16:20:00-07:00 # and a comment, just because")
           {"mydob" (read-instant-timestamp "1975-10-03T16:20:00-07:00")}))
    (is (= (parse-string "mydob = 1975-10-03T16:20:00.999999-07:00 # and a comment, just because")
           {"mydob" (read-instant-timestamp "1975-10-03T16:20:00.999999-07:00")}))))

(deftest bool-test
  (testing "Booleans"
    (is (= (parse-string "truthy = true
                          falsy = false")
           {"truthy" true
            "falsy" false}))))

(deftest array-test
  (testing "Arrays"
    (is (= (parse-string "inline = [1, 2, 3]
                          multiline = [4, 
                                       5, 
                                       -6]
                          nested = [[7, 8, -9], 
                                    [\"seven\",
                                     \"eight\",
                                     \"negative nine\"]]")
           {"inline" [1 2 3]
            "multiline" [4 5 -6]
            "nested" [[7 8 -9]["seven" "eight" "negative nine"]]}))))

(deftest lonely-keygroup
  (testing "Lonely keygroups"
    (is (= (parse-string "[Agroup]
                          [Bgroup]
                          [Bgroup.nested]
                          [Cgroup.nested]")
           {"Agroup" {}
            "Bgroup" {"nested" {}}
            "Cgroup" {"nested" {}}}))))

(deftest standard-keygroup
  (testing "Standard keygroups"
    (is (= (parse-string "[Agroup]
                          first = \"first\"
                          second = true
                          third = 3
                          fourth = 4.0
                          fifth = [5, -6 ,7]")
           {"Agroup" {"first" "first"
                      "second" true
                      "third" 3
                      "fourth" 4.0
                      "fifth" [5 -6 7]}}))))

(deftest nested-keygroup
  (testing "Nested keygroups"
    (is (= (parse-string "[Agroup]
                          first = \"first\"
                          second = true

                          [Agroup.nested]
                          third = 3
                          fourth = 4.0

                          [Bgroup.nested]
                          fifth = [5, -6 ,7]")
           {"Agroup" {"first" "first"
                      "second" true
                      "nested"
                      {"third" 3
                       "fourth" 4.0}}
            "Bgroup" {"nested"
                      {"fifth" [5 -6 7]}}}))))

(deftest inline-table-test
  (testing "Inline table"
    (is (= (parse-string "[table.inline]
                          name = { first = \"Tom\", last = \"Preston-Werner\" }
                         ")
           {"table" {"inline" {"name" {"first" "Tom" 
                                       "last" "Preston-Werner"}}}}))))

(deftest example-test
  (testing "TOML example"
    (is (= (parse-string (slurp "resources/example.toml"))
           {"title" "TOML Example"
            "owner"
            {"name" "Tom Preston-Werner"
             "organization" "GitHub"
             "bio" "GitHub Cofounder & CEO\nLikes tater tots and beer."
             "dob" (read-instant-timestamp "1979-05-27T07:32:00Z")}
            "database"
            {"enabled" true,
             "connection_max" 5000,
             "server" "192.168.1.1",
             "ports" [8001 8001 8002]}
            "servers"
            {"alpha"
             {"ip" "10.0.0.1"
              "dc" "eqdc10"}
             "beta"
             {"ip" "10.0.0.2"
              "dc" "eqdc10"
              "country" "中国"}}
            "clients"
            {"data" [["gamma" "delta"] [1 2]]
             "hosts" ["alpha" "omega"]}
            "products"
            [{"name" "Hammer"
              "sku" 738594937}
             {"name" "Nail"
              "sku" 284758393
              "color" "gray"}]
            }))))

(deftest hard-example-test
  (testing "TOML hard example"
    (is (= (parse-string (slurp "resources/hard_example.toml"))
           {"the"
            {"hard"
             {"another_test_string" " Same thing, but with a string #",
              "test_array2"
              ["Test #11 ]proved that" "Experiment #9 was a success"],
              "test_array" ["] " " # "],
              "bit#"
              {"what?" "You don't think some user won't do that?",
               "multi_line_array" ["]"]},
              "harder_test_string"
              " And when \"'s are in the string, along with # \""},
             "test_string" "You'll hate me after this - #"}}))))

#_(deftest example-v0.4.0-test
  (testing "TOML 0.4.0 example"
    (is (= (parse-string (slurp "resources/example-v0.4.0.toml"))
           {"table"
            {"key" "value"
             "subtable" {"key" "another value"}
             "inline" {"first" "Tom" "last" "Preston-Werner"}}
            "x" {"y" {"z" {"w" {}}}}
            "string"
            {"basic" 
             {"basic" "I'm a string. \"You can quote me\". Name\tJos\u00E9\nLocation\tSF."}
             "multiline"
             {"key1" "One\nTwo"
              "key2" "One\nTwo"
              "key3" "One\nTwo"
              "continued"
              {"key1" "The quick brown fox jumps over the lazy dog."
               "key2" "The quick brown fox jumps over the lazy dog."
               "key3" "The quick brown fox jumps over the lazy dog."}}
             "literal"
             {"winpath" "C:\\Users\\nodejs\\templates"
              "winpath2" "\\\\ServerX\\admin$\\system32\\"
              "quoted" "Tom \"Dubs\" Preston-Werner"
              "regex" "<\\i\\c*\\s*>"
              "multiline"
              {"regex2" "I [dw]on't need \\d{2} apples"
               "lines" "The first newline is\ntrimmed in raw strings.\n   All other whitespace\n   is preserved.\n"}}}
            "integer"
            {"key1" 99 
             "key2" 42
             "key3" 0
             "key4" -17
             "underscores"
             {"key1" 1000
              "key2" 5349221
              "key3" 12345}}
            "float"
            {"fractional"
             {"key1" 1.0
              "key2" 3.1415
              "key3" -0.01}
             "exponent"
             {"key1" 5e+22
              "key2" 1e6
              "key3" -2E-2}
             "both"
             {"key" 6.626e-34}
             "underscores"
             {"key1" 9224617.445991228313
              "key2" 1e1000}}
            "boolean"
            {"True" true
             "False" false}
            "datetime"
            {"key1" "1979-05-27T07:32:00Z"
             "key2" "1979-05-27T00:32:00-07:00"
             "key3" "1979-05-27T00:32:00.999999-07:00"}
            "array"
            {"key1" [1 2 3]
             "key2" ["red" "yellow" "green"]
             "key3" [[1 2] [3 4 5]]
             "key4" [[1 2] ["a" "b" "c"]]
             "key5" [1 2 3]
             "key6" [1 2]}
            "products"
            [{"name" "Hammer" "sku" 738594937}
             {}
             {"name" "Neil" "sku" 284758393 "color" "gray"}]
            "fruit"
            [{"name" "apple" 
              "physical" {"color" "red" "shape" "round"} 
              "variety" [{"name" "red delicious"}
                         {"name" "granny smith"}]}
             {"name" "banana" 
              "variety" [{"name" "plantain"}]}]}))))
