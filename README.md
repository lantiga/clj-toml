# clj-toml

clj-toml is [TOML](https://github.com/mojombo/toml) for Clojure. TOML is Tom's Obvious, Minimal Language. 

> TOML is like INI, only better (Tom Preston-Werner)

  clj-toml uses [Instaparse](https://github.com/Engelberg/instaparse) for parsing. Instaparse does all the heavy lifting, we're just sitting pretty.

  clj-toml comes with a decent [collection of tests](https://github.com/lantiga/clj-toml/blob/master/test/clj_toml/core_test.clj). It successfully parses the TOML [hard example](https://github.com/mojombo/toml/blob/master/tests/hard_example.toml). Easy peasy.

  Supported TOML version: [v0.4.0](https://github.com/toml-lang/toml/blob/master/versions/en/toml-v0.4.0.md)

## Usage

  Work is underway towards a TOML 0.4.0-compliant release. Use the 0.3.2 release (TOML 0.1.0-compliant) in the meantime:

  Leiningen:

  ```clojure
  [clj-toml "0.3.2"]
  ```

  Test:

  ```clojure
  lein test
  ```

  Use:

  ```clojure
  (use 'clj-toml.core)

  (parse-string "
   title = \"TOML\"
   [Foo]
   bar=[1,2,3]")
  ;; {"title" "TOML" "foo" {"bar" [1 2 3]}}
  ```

## TODO

  The parser is pretty solid (thanks to Instaparse) and complete. 

  In a way it implements a superset of TOML, since it successfully parses

  * non-homogeneous arrays
  * TOML with duplicate keys

  As the TOML specification stabilizes, we'll raise errors according to specification.

## License

  Copyright © 2013-2015 Luca Antiga.

  Distributed under the Eclipse Public License, the same as Clojure.
