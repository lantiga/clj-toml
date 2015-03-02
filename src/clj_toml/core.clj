(ns clj-toml.core
  ^{:doc "TOML for Clojure"
    :author "Luca Antiga"}
  (:use [blancas.kern.core]
        [blancas.kern.i18n]
        [clojure.instant :only [read-instant-timestamp]]
        [clj-toml.toml-style])
  (:require [clojure.string :as s]))

(def- sign (optional (one-of* "+-")))

(def- int-suffix (<|> (<< (sym* \N) (not-followed-by letter))
		      (not-followed-by (<|> letter (sym* \.)))))

(def- float-suffix (<< (optional (sym* \M)) (not-followed-by letter)))

;; TODO: 
;; multiline strings
;; array of tables

(defn- rmvz_
  "Removes leading zeroes and underscores from a string."
  [cs]
  (let [s (clojure.string/join (drop-while #(= % \0) cs))
        s (clojure.string/replace s #"_" "")]
    (if (empty? s) "0" s)))

(def digit_ (<|> digit (sym* \_)))

(def double-brackets
  (fn [p] (between (times 2 (sym \[)) (times 2 (sym \])) (lexeme p))))

(def dec-lit_
  (<?> (>>= (<:> (lexeme (<+> sign (many1 digit_) int-suffix)))
            (fn [x] (return (read-string (rmvz_ x)))))
       (i18n :dec-lit)))

(def float-lit_ 
  (<?> (>>= (<:> (lexeme
                   (<+> sign (many1 digit_)
                        (option ".0" (<*> (sym* \.) (many1 digit_)))
                        (optional (<*> (one-of* "eE") sign (many1 digit_)))
                        float-suffix)))
            (fn [x] (>> (return (read-string (rmvz_ x))) clear-empty)))
       (i18n :float-lit)))

(def timestamp-lit
  (<?> (>>= (<:> (lexeme
                  (<+> (times 4 digit) (sym* \-) 
                       (times 2 digit) (sym* \-) 
                       (times 2 digit) (sym* \T) 
                       (times 2 digit) (sym* \:) 
                       (times 2 digit) (sym* \:) 
                       (times 2 digit) 
                       (optional (<+> (sym* \.) (many1 digit))) 
                       (<|> (sym* \Z)
                            (<+> (sym* \-) (times 2 digit) 
                                 (sym* \:) (times 2 digit))))))
            #(return (read-instant-timestamp %)))
       "timestamp literal"))

(def primitive
  (<|> timestamp-lit dec-lit_ float-lit_ string-lit bool-lit))

;; TODO: check that array is homogeneous, otherwise fail
(def array
  (brackets (sep-end-by comma (<|> primitive (fwd array)))))

(def inline-table
  (<$> #(into {} %) 
       (braces (sep-by1 comma (<*> (<< (<|> identifier string-lit) (sym \=)) primitive)))))

(def pair
  (bind [f (<?> (<|> identifier string-lit) "key")
         _ (sym \=)
         v (<|> primitive array inline-table)]
        (return [f v])))

(defn- split-group-name [x]
  (prn "SPLIT" x)
  (map #(s/replace % #"\"(.*)\"" "$1") (s/split x #"\.")))

(def table-array
  (bind [hrd (<$> split-group-name (double-brackets (field "]]\n"))) 
         rec (many pair)]
        (return [hrd (apply hash-map (apply concat rec))])))

(def group
  (bind [;; TODO: replace with (brackets (sep-by1 dot (<|> string-lit identifier)))
         hrd (<$> split-group-name (brackets (field "]\n"))) 
         rec (<$> (fn [x] (prn x) x) (many pair)) 
         #_(many pair)]
        (return [hrd (apply hash-map (apply concat rec))])))

(def root-group
  (bind [rec (many pair)]
        (return (apply hash-map (apply concat rec)))))

(def groups
  (bind [rec (many group #_(<|> table-array group))]
        (return
          (reduce
            (fn [out [k v]]
              (prn "ASSOC" k v)
              (assoc-in out k v))
            {} 
            rec))))

(def toml
  (bind [rec (<*> root-group groups)]
        (return (apply merge rec))))

(defn parse-string
  "Returns the Clojure object corresponding to the given TOML string."
  [^String string]
  (when string
    (:value (parse (>> trim toml) string))))

(comment
  (parse-string (slurp "resources/example.toml")))
