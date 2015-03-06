(ns clj-toml.core
  ^{:doc "TOML for Clojure"
    :author "Luca Antiga"}
  (:use [instaparse.core :as insta]
        [clojure.instant :only [read-instant-timestamp]])
  (:require [clojure.string :as s]))


(def toml-grammar
  "
  <TOML> = <n*> root-table? <n*> tables* <n*>

  root-table = root-table-content
  <root-table-content> = <s*> pair <s*> | <s*> pair <s*> <n+> root-table-content

  <tables> = (table | table-array) | (table | table-array) <n+> tables

  table = table-name <n*> table-content
  table-name = <s*> <'['> <s*> dotted-name <s*> <']'> <s*>
  table-content = table-content-rec
  <table-content-rec> = <s*> pair <s*> | <s*> pair <s*> <n+> table-content-rec

  table-array = table-array-name <n*> table-array-content
  table-array-name = <s*> <'[['> <s*> dotted-name <s*> <']]'> <s*>
  table-array-content = table-array-content-rec
  <table-array-content-rec> = <s*> pair <s*> | <s*> pair <s*> <n+> table-array-content-rec

  <dotted-name> = key | key <s*> <'.'> <s*> dotted-name

  <pair> = key <s*> <'='> <s*> value

  <key> = identifier | string

  <value> = inline-table | array | primitive

  inline-table = <'{'> <s*> inline-table-content <s*> <'}'>
  <inline-table-content> = pair <s*> <','?> | pair <s*> <','> <s*> inline-table-content
 
  array = <'['> <s*> <n*> array-content <n*> <s*> <']'>
  <array-content> = <s*> primitive <s*> <n*> <','?> <s*> <n*> | <s*> primitive <s*> <n*> <','> <s*> <n*> array-content
 
  <primitive> = timestamp | integer | float | boolean | string

  timestamp = #'(\\d{4}-\\d{2}-\\d{2}[tT]\\d{2}:\\d{2}:\\d{2}(\\.\\d*)?)([zZ]|([+\\-])(\\d{2}):?(\\d{2}))'

  boolean = 'true' | 'false'

  integer = int
  float = (sign? frac) | (int frac) | (int exp) | (int frac exp) | (float 'M')
  <int> = (sign? digit_) | (sign? #'[1-9]' digits_) | (int 'M')
  <sign> = '+' | '-'
  <frac> = '.' digits_
  <exp> = ex digits_
  <digits_> = digit_ | (digit_ digits_)
  <digit_> = digit | <'_'>
  <digits> = digit | (digit digits)
  <digit> = #'[0-9]'
  <ex> = 'e' | 'e+' | 'e-' | 'E' | 'E+' | 'E-'

  string = <'\"'> string-content <'\"'>
  <string-content> = #'[^\"\n]*'

  identifier = #'[a-zA-Z0-9_-]+' 

  <n> = <s*> <comment?> '\n'
  <comment> = #'#[^\n]*'
  <s> = ' '
  ")

;; TODO:
;; multiline strings
;; force arrays to be homogenous
;; repeating table names is invalid

(defn- to-map [xs]
  (->> xs (partition 2) (map vec) (into {})))

(defn- to-entry [entry-type ks xs]
  {:entry-type entry-type
   :keys (vec ks)
   :value (to-map xs)})

(defn- merge-entries [res {entry-type :entry-type ks :keys entry-value :value}]
  (if (= entry-type :root-table) 
    (merge res entry-value)
    (let [ks (reduce 
               (fn [out k]
                 (let [v (get-in res out)]
                   (if (vector? v) 
                     (concat out [(dec (count v)) k])
                     (conj out k)))) 
               [] ks)]
      (if (get-in res ks) 
        (condp = entry-type
          :table (update-in res ks merge entry-value)
          :table-array (update-in res ks conj entry-value))
        (condp = entry-type
          :table (assoc-in res ks entry-value)
          :table-array (assoc-in res ks [entry-value]))))))
 
(def toml-parser
  (insta/parser toml-grammar))

(def toml-transform
  (partial insta/transform
           {:float (comp read-string str) 
            :integer (comp read-string str)
            :boolean read-string
            :timestamp read-instant-timestamp
            :string str
            :identifier str
            :array (fn [& xs] (into [] xs))
            :inline-table (fn [& xs] (to-map xs))
            :root-table (fn [& xs] (to-entry :root-table nil xs))
            :table (fn 
                     ([[_ & ks]] (to-entry :table ks []))
                     ([[_ & ks] [_ & xs]] (to-entry :table ks xs)))
            :table-array (fn 
                           ([[_ & ks]] (to-entry :table-array ks []))
                           ([[_ & ks] [_ & xs]] (to-entry :table-array ks xs)))}))

(defn parse-string
  "Returns the Clojure object corresponding to the given TOML string."
  [^String string]
  (->> (str string "\n") 
    toml-parser
    toml-transform
    (reduce merge-entries {})))


(comment

  (parse-string "[[foo.bar.\"#baz\"]]
                a = {foo = \"bar\", baz = 123}") 

  (parse-string "[[fruit]]
                name = \"apple\"

                [[fruit.variety]]
                name = \"red delicious\"

                [[fruit.variety]]
                name = \"granny smith\"

                [[fruit]]
                name = \"banana\"") 

  (parse-string "# ciccio
                a = 456 
                # asd
                b = 123 #lakmsd

                [ foo . bar.\"#baz\"] # lkmasld
                a = 456
                b = 123")
  
  (parse-string "b = 123
                a = [1, 2 , #asd
                3 , 
                5 , ]

                [foo.bar.\"#baz2\"]
                a = {foo = \"bar\", baz = 123}

                [foo.bar.\"#baz\"]
                a = {foo = \"bar\", baz = 123}"))

 
