(ns clj-toml.core
  ^{:doc "TOML for Clojure"
    :author "Luca Antiga"}
  (:use [instaparse.core :as insta]
        [clojure.instant :only [read-instant-timestamp]]
        [clojure.string :only [replace-first]]
        [java-time :only [local-time]])
  (:require [clojure.string :as s]
            [clojure.zip :as z]))

(def toml-grammar
  "
  ;; Overall Structure

  toml = expression *( <newline> expression )

  <expression> =  <ws> [ <comment> ]
  <expression> =/ <ws> keyval <ws> [ <comment> ]
  <expression> =/ <ws> table <ws> [ <comment> ]

  ;; Whitespace

  ws = *wschar

  wschar =  %x20  ; Space
  wschar =/ %x09  ; Horizontal tab

  ;; Newline

  newline =  %x0A     ; LF
  newline =/ %x0D.0A  ; CRLF

  ;; Comment

  comment-start-symbol = %x23 ; #
  non-eol =  %x09
  non-eol =/ %x20-10FFFF

  comment = comment-start-symbol *non-eol

  ;; Key-Value pairs

  keyval = key <keyval-sep> val

  key = unquoted-key / quoted-key
  <unquoted-key> = 1*( ALPHA / DIGIT / %x2D / %x5F ) ; A-Z / a-z / 0-9 / - / _
  <quoted-key> = basic-string / literal-string

  keyval-sep = ws %x3D ws ; =

  <val> = string / boolean / array / inline-table / date-time / float / integer

  ;; Table

  <table> = table-name expression
  <table-name> = std-table / array-table

  ;; Standard Table

  std-table = <std-table-open> key *( <table-key-sep> key ) <std-table-close>

  <std-table-open>  = %x5B ws     ; [ Left square bracket
  <std-table-close> = ws %x5D     ; ] Right square bracket
  <table-key-sep>   = ws %x2E ws  ; . Period

  ;; String

  <string> = ml-basic-string / basic-string / ml-literal-string / literal-string

  ;; Basic String

  basic-string = <quotation-mark> *basic-char <quotation-mark>

  quotation-mark = %x22            ; \"

  <basic-char> = basic-unescaped / escaped
  <basic-unescaped> = %x20-21 / %x23-5B / %x5D-7E / %x80-10FFFF

  escaped = escape escape-seq-char

  <escape> = %x5C                    ; \\
  <escape-seq-char> =   %x22         ; \"    quotation mark  U+0022
  <escape-seq-char> =/  %x5C         ; \\    reverse solidus U+005C
  <escape-seq-char> =/  %x2F         ; /     solidus         U+002F
  <escape-seq-char> =/  %x62         ; b     backspace       U+0008
  <escape-seq-char> =/  %x66         ; f     form feed       U+000C
  <escape-seq-char> =/  %x6E         ; n     line feed       U+000A
  <escape-seq-char> =/  %x72         ; r     carriage return U+000D
  <escape-seq-char> =/  %x74         ; t     tab             U+0009
  <escape-seq-char> =/  %x75 4HEXDIG ; uXXXX                U+XXXX
  <escape-seq-char> =/  %x55 8HEXDIG ; UXXXXXXXX            U+XXXXXXXX

  ;; Multiline literals

  <ml-newline> = ( %x0A /              ; LF
                   %x0D.0A )           ; CRLF

  ;; Multiline Basic String

  ml-basic-string = <ml-basic-string-delim> ml-basic-body <ml-basic-string-delim>

  ml-basic-string-delim = 3quotation-mark

  <ml-basic-body> = *( ml-basic-char / ml-newline / ( escape ws ml-newline ) )
  <ml-basic-char> = ml-basic-unescaped / escaped
  <ml-basic-unescaped> = %x20-5B / %x5D-7E / %x80-10FFFF

  ;; Literal String

  literal-string = <apostrophe> *literal-char <apostrophe>

  apostrophe = %x27 ; ' apostrophe

  <literal-char> = %x09 / %x20-26 / %x28-10FFFF

  ;; Multiline Literal String

  ml-literal-string = <ml-literal-string-delim> ml-literal-body <ml-literal-string-delim>

  ml-literal-string-delim = 3apostrophe

  <ml-literal-body> = *( ml-literal-char / ml-newline )
  <ml-literal-char> = %x09 / %x20-10FFFF

  ;; Integer

  integer = dec-int / hex-int / oct-int / bin-int

  <minus> = %x2D                       ; -
  <plus> = %x2B                        ; +

  underscore = %x5F                  ; _
  <digit1-9> = %x31-39               ; 1-9
  <digit0-7> = %x30-37               ; 0-7
  <digit0-1> = %x30-31               ; 0-1

  <hex-prefix> = %x30.78             ; 0x
  <oct-prefix> = %x30.6f             ; 0o
  <bin-prefix> = %x30.62             ; 0b

  dec-int = [ minus / plus ] unsigned-dec-int
  <unsigned-dec-int> = DIGIT / digit1-9 1*( DIGIT / <underscore> DIGIT )

  hex-int = hex-prefix HEXDIG *( HEXDIG / <underscore> HEXDIG )
  oct-int = oct-prefix digit0-7 *( digit0-7 / <underscore> digit0-7 )
  bin-int = bin-prefix digit0-1 *( digit0-1 / <underscore> digit0-1 )

  ;; Float

  float = float-int-part ( exp / frac [ exp ] )
  float =/ special-float

  <float-int-part> = [ minus / plus ] unsigned-dec-int
  <frac> = decimal-point zero-prefixable-int
  <decimal-point> = %x2E               ; .
  <zero-prefixable-int> = DIGIT *( DIGIT / <underscore> DIGIT )

  <exp> = \"e\" float-int-part

  <special-float> = [ minus / plus ] ( inf / nan )
  <inf> = %x69.6e.66  ; inf
  <nan> = %x6e.61.6e  ; nan

  ;; Boolean

  boolean = true / false

  true    = <%x74.72.75.65>     ; true
  false   = <%x66.61.6C.73.65>  ; false

  ;; Date and Time (as defined in RFC 3339)

  date-time      = offset-date-time / local-date-time / local-date / local-time

  <date-fullyear>  = 4DIGIT
  <date-month>     = 2DIGIT  ; 01-12
  <date-mday>      = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31 based on month/year
  <time-delim>     = \"T\" / %x20 ; T, t or space
  <time-hour>      = 2DIGIT  ; 00-23
  <time-minute>    = 2DIGIT  ; 00-59
  <time-second>    = 2DIGIT  ; 00-58, 00-59, 00-60 based on leap second rules
  <time-secfrac>   = \".\" 1*DIGIT
  <time-numoffset> = ( \"+\" / \"-\" ) time-hour \":\" time-minute
  <time-offset>    = \"Z\" / time-numoffset

  <partial-time>   = time-hour \":\" time-minute \":\" time-second [ time-secfrac ]
  <full-date>      = date-fullyear \"-\" date-month \"-\" date-mday
  <full-time>      = partial-time time-offset

  ;; Offset Date-Time

  offset-date-time = full-date time-delim full-time

  ;; Local Date-Time

  local-date-time = full-date time-delim partial-time

  ;; Local Date

  local-date = full-date

  ;; Local Time

  local-time = partial-time

  ;; Array

  array = <array-open> [ array-values ] <ws-comment-newline> <array-close>

  array-open  = %x5B ; [
  array-close = %x5D ; ]

  <array-values> = <ws-comment-newline> val <ws> <array-sep> array-values
  <array-values> =/ <ws-comment-newline> val <ws> [ <array-sep> ]

  array-sep = %x2C  ; , Comma

  ws-comment-newline = *( wschar / [ comment ] newline )

  ;; Inline Table

  inline-table = <inline-table-open> inline-table-keyvals <inline-table-close>

  inline-table-open  = %x7B ws     ; {
  inline-table-close = ws %x7D     ; }
  inline-table-sep   = ws %x2C ws  ; , Comma

  <inline-table-keyvals> = [ inline-table-keyvals-non-empty ]
  <inline-table-keyvals-non-empty> = key <keyval-sep> val [ <inline-table-sep> inline-table-keyvals-non-empty ]

  ;; Array Table

  array-table = <array-table-open> key *( <table-key-sep> key ) <array-table-close>

  array-table-open  = %x5B.5B ws  ; [[ Double left square bracket
  array-table-close = ws %x5D.5D  ; ]] Double right square bracket

  ;; Built-in ABNF terms, reproduced here for clarity

  <ALPHA> = %x41-5A / %x61-7A ; A-Z / a-z
  <DIGIT> = %x30-39 ; 0-9
  <HEXDIG> = DIGIT / \"A\" / \"B\" / \"C\" / \"D\" / \"E\" / \"F\"
  ")

;; TODO:
;; repeating table names is invalid
;; non-homogenous array types

(def toml-parser
  (insta/parser toml-grammar :input-format :abnf))

(defn- convert-boolean
  "Converts value in form [keyword] into Clojure boolean type."
  [x]
  (let [value (first x)]
    (= value :true)))

(defn- convert-array
  "Converts value from sequence to array."
  [& xs]
  (vec xs))

(defn- convert-float
  "Converts value in form [keyword] into Clojure float type."
  [x]
  (condp = x
    "-inf" Double/NEGATIVE_INFINITY
    "+inf" Double/POSITIVE_INFINITY
    "inf" Double/POSITIVE_INFINITY
    "-nan" Double/NaN
    "+nan" Double/NaN
    "nan" Double/NaN
    (read-string x)))

(def toml-transform
  (partial insta/transform
           {:keyval            (fn [k v]
                                 (if (and (sequential? v) (= :inline-table (first v)))
                                   [:inline-table k v]
                                   [:keyval k v]))
            :float             (comp convert-float str)
            :integer           identity
            :dec-int           (comp read-string str)
            :hex-int           (comp read-string #(str "16r" %) #(subs % 2) str)
            :oct-int           (comp read-string #(str "8r" %) #(subs % 2) str)
            :bin-int           (comp read-string #(str "2r" %) #(subs % 2) str)
            :boolean           convert-boolean
            :offset-date-time  (comp read-instant-timestamp #(replace-first % #" " "T") str)
            :local-date-time   (comp read-instant-timestamp #(replace-first % #" " "T") str)
            :local-date        (comp read-instant-timestamp #(replace-first % #" " "T") str)
            :local-time        (comp local-time str)
            :date-time         identity
            :escaped           (comp read-string #(str "\"" % "\"") str)
            :ml-basic-string   str
            :basic-string      str
            :ml-literal-string str
            :literal-string    str
            :key               str
            :array             convert-array
            :std-table         (fn [& xs] [:std-table (into [] xs)])
            :toml              (fn [& xs] xs)}))

;; Segment end predicates

(defn- segment-separator?
  "Returns if x separates input into new segment."
  [x]
  (let [[t _ _] x]
    (or (= t :std-table) (= t :array-table))))

;; Value update methods

(defn- update-by-keyval
  "Updates m with new value v bound to key k."
  [m k v]
  (into m {k v}))

(defn- sequence-path
  "Creates path item for sequential item."
  [xs ks k]
  [(conj (conj ks k) (dec (count xs))) (last xs)])

(defn- consume-path
  "Walks data given the path as far as possible with returning
  the consumed path and data on it."
  [data path]
  (reduce
   (fn [x y]
     (let [[ck nk] y
           ks      (first x)
           values  (second x)]
       (cond
         (nil? ck)                     (reduced x)
         (not (map? values))           (reduced x)
         (not (contains? values ck))   (reduced x)
         (sequential? (get values ck)) (sequence-path (get values ck) ks ck)
         :else                         [(conj ks ck) (get values ck)])))
   [[] data]
   (partition 2 1 (conj (vec path) nil))))

(defn- path-trim-left
  "Splits xs by ys with returning prefix of path defined by
  ys found in xs. Data d accompany result."
  [xs ys d]
  (loop [xs1   xs
         ys1   ys
         taken []]
    (cond
      (empty? xs1)                   [taken ys1 d]
      (number? (first xs1))          (recur (rest xs1) ys1 (conj taken (first xs1)))
      (empty? ys1)                   [taken ys1 d]
      (not= (first xs1) (first ys1)) [taken ys1 d]
      :else                          (recur (rest xs1) (rest ys1) (conj taken (first xs1))))))

(defn- map-swap!
  "Returns y, intended for usage in update-in for maps."
  [x y]
  y)

(defn- analyze-table
  "Creates combination of present path, rest path and data."
  [m ks]
  (let [[path data] (consume-path m ks)]
    (path-trim-left path ks data)))

;; Standard table update

(defn- standard-table-update
  "Updates supposedly table value in data on path with
  mv value."
  [data path mv]
  (conj
   (if (map? data) data {})
   (if (empty? path)
     mv
     (update-in {} path map-swap! mv))))

(defn- array-table-update
  "Updates path in data with array of tables value represented
  by mv."
  [data path mv e?]
  (let [real-data (if (and e? (empty? path))
                    {}
                    (cond
                      (map? data) data
                      (sequential? data) data
                      :else []))]
    (conj
     real-data
     (if (empty? path)
       mv
       (update-in {} path map-swap! [mv])))))

(defn- input-table-update
  "Updates associative structure m with mv on path."
  [m path mv]
  (if (empty? path)
    (into m mv)
    (update-in m path map-swap! mv)))

;; Standard table update

(defn- update-standard-table
  "Updates associative structure m with map value mv on path ks."
  [m ks mv]
  (let [[present path _] (analyze-table m ks)
        data             (get-in m present)]
    (input-table-update m present (standard-table-update data path mv))))

;; Array of tables update

(defn- update-array-table
  "Updates associative structure m with array of tables mv on path ks."
  [m ks mv]
  (let [[present path data] (analyze-table m ks)
        existing?           (number? (last present))
        real-present        (if (and existing? (empty? path))
                              (conj (vec (butlast present)) (inc (last present)))
                              present)]
    (input-table-update m real-present (array-table-update data path mv existing?))))

;; Inline array update

(defn- prepare-inline-array
  "Converts output of Instaparse for inline-table into internal table-like input."
  [coll]
  (clojure.walk/prewalk
   (fn [x]
     (if (and (sequential? x) (= (first x) :inline-table))
       (map
        (fn [x]
          (if-not (and (sequential? (second x)) (= :inline-table (first (second x))))
            (assoc {:type :node} :value x)
            (assoc {:type :table} :key (first x) :value (second x))))
        (partition 2 (rest x)))
       x))
   coll))

(defn- concat-inline-input
  "Concats inline table-like input with inserting ends of tables."
  [coll x]
  (concat (when (and (not= :table-end (:type x)) (not= :node (:type x)))
            (concat (:value x) [{:type :table-end}]))
          (rest coll)))

(defn- concat-inline-output
  "Concats inline table-like output and removes ends of tables."
  [coll x ctx]
  (concat coll (condp = (:type x)
                 :table-end nil
                 :table     [(assoc x :key (conj ctx (:key x)))]
                 [x])))

(defn- update-inline-context
  "Manages context for internal table-like input."
  [coll x]
  (condp = (:type x)
    :table     (conj coll (:key x))
    :table-end (pop coll)
    coll))

(defn- flatten-inline-array
  "Flattens nested structure of inline-table into sequence."
  [coll]
  (loop [input  coll
         output []
         ctx    []]
    (if (empty? input)
      output
      (let [x (first input)]
        (recur
         (concat-inline-input input x)
         (concat-inline-output output x ctx)
         (update-inline-context ctx x))))))

(defn- convert-inline-array
  "Converts inline-table Instaparse output into Instaparse standard table output."
  [coll]
  (->> coll
       prepare-inline-array
       flatten-inline-array
       (map (fn [x]
              (condp = (:type x)
                :node  (into [:keyval] (:value x))
                :table (into [:std-table] [(:key x)]))))))

;; Input processing

(declare segment-merge)

(defmulti ^:private process
  "Processes current segment in input and merges it into values based on type."
  (fn [type values input] type))

(defmethod ^:private process :keyval
  [_ values input]
  (let [[_ k v] (first input)]
    [(update-by-keyval values k v) (rest input)]))

(defmethod ^:private process :array
  [_ values input]
  (let [[_ k v] (first input)]
    [(update-by-keyval values k v) (rest input)]))

(defmethod ^:private process :inline-table
  [_ values input]
  [(merge values
          (segment-merge {} (convert-inline-array (first input))))
   (rest input)])

(defmethod ^:private process :std-table
  [_ values input]
  (let [[_ & ks] (first input)
        [this other] (split-with (complement segment-separator?) (rest input))]
    [(update-standard-table values (first ks) (segment-merge {} this))
     other]))

(defmethod ^:private process :array-table
  [_ values input]
  (let [[_ & ks] (first input)
        [this other] (split-with (complement segment-separator?) (rest input))]
    [(update-array-table values ks (segment-merge {} this))
     other]))

(defmethod ^:private process :default
  [_ values input]
  nil)

;; Merging by segments

(defn- segment-merge
  "Merges segment into values."
  [values input]
  (if (empty? input)
    values
    (let [[vs in] (process (ffirst input) values input)]
      (recur vs in))))

(defn parse-string
  "Parses TOML structure from string into Clojure associative structure."
  [^String string]
  (->>
   string
   toml-parser
   toml-transform
   (segment-merge {})))

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
