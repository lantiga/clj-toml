(ns clj-toml.core-test
  (:use clojure.test
        clj-toml.core
        [clj-time.core :only [date-time]]))

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
            {"data" [["gamma" "delta"] [1 2]]}}))))
