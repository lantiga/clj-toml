(ns clj-toml.core-test
  (:use clojure.test
        clj-toml.core
        [clj-time.core :only [date-time]]))

(deftest comment-test
  (testing "Comments"
    (is (= (parse-string "#just a comment line
                          foo = \"bar\" # and one more comment")
           {"foo" "bar"}))))

(deftest numbers-test
  (testing "Numbers"
    (is (= (parse-string "integer = 3
                          negative_integer = -3
                          float = 3.0
                          negative_float = -3.0")
           {"integer" 3
            "negative_integer" -3
            "float" 3.0
            "negative_float" -3.0}))))

(deftest datetime-test
  (testing "Datetime"
    (is (= (parse-string "mydob = 1975-10-03T16:20:00Z # and a comment, just because")
           {"mydob" (date-time 1975 10 03 16 20 00)}))))

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

(deftest example-test
  (testing "TOML example"
    (is (= (parse-string (slurp "resources/example.toml"))
           {"title" "TOML Example"
            "owner"
            {"name" "Tom Preston-Werner"
             "organization" "GitHub"
             "bio" "GitHub Cofounder & CEO\nLikes tater tots and beer."
             "dob" (date-time 1979 5 27 7 32 0)}
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
              "dc" "eqdc10"}}
            "clients"
            {"data" [["gamma" "delta"] [1 2]]
             "hosts" ["alpha" "omega"]}}))))

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
