(ns clj-toml.core
  ^{:doc "TOML parser written in Clojure"
    :author "Luca Antiga"}
  (:use [blancas.kern.core]
        [clj-time.core :only [date-time]]
        [clj-toml.toml-style]))

(def datetime-lit
  (<?> (>>= (<:> (lexeme
                  (<*> (<+> (times 4 digit)) (sym* \-)
                       (<+> (times 2 digit)) (sym* \-)
                       (<+> (times 2 digit)) (sym* \T)
                       (<+> (times 2 digit)) (sym* \:)
                       (<+> (times 2 digit)) (sym* \:)
                       (<+> (times 2 digit)) (sym* \Z))))
            (fn [[y _ mo _ d _ h _ m _ s]]
              (return (apply date-time (map read-string [y mo d h m s])))))
       "datetime literal"))

(def primitive
  (<|> datetime-lit dec-lit float-lit string-lit bool-lit))

(declare array)

;; TODO: check that array is homogeneous, otherwise fail
(def array
  (brackets (comma-sep (<|> primitive array))))

(def array
  (brackets (comma-sep (<|> primitive array))))

(def pair
  (bind [f (<?> identifier "key = value")
         _ (sym \=)
         v (<|> primitive array)]
        (return [f v])))

(def group
  (bind [hrd (brackets (field "]\n"))
         rec (many pair)]
        (return [hrd (apply hash-map (apply concat rec))])))

(def root-group
  (bind [rec (many pair)]
        (return (apply hash-map (apply concat rec)))))

(def groups
  (bind [rec (many group)]
        (return
         (reduce
          (fn [out [k v]]
            (assoc-in out (clojure.string/split k #"\.") v)) {} rec))))

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
