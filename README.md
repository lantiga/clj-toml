# clj-toml

clj-toml is [TOML](https://github.com/mojombo/toml) for Clojure. TOML is Tom's Obvious, Minimal Language. 

> TOML is like INI, only better (Tom Preston-Werner)

  clj-toml uses [Kern](https://github.com/blancas/kern) for parsing. Kern does all the heavy lifting, we're just sitting pretty.

  clj-toml comes with a decent [collection of tests](https://github.com/lantiga/clj-toml/blob/master/test/clj_toml/core_test.clj). It successfully parses the TOML [hard example](https://github.com/mojombo/toml/blob/master/tests/hard_example.toml). Easy peasy.

  Supported TOML version: [v0.1.0](https://github.com/mojombo/toml/blob/master/versions/toml-v0.1.0.md)

## Usage

  Leiningen:

  ```clojure
  [clj-toml "0.3.1"]
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

  The parser is pretty solid (thanks to Kern) and complete. 

  In a way it implements a superset of TOML, since it successfully parses

  * non-homogeneous arrays
  * TOML with duplicate keys
  * multiline strings

  As the TOML specification stabilizes, we'll raise errors according to specification.

## License

  Copyright © 2013 Luca Antiga.

  Distributed under the Eclipse Public License, the same as Clojure.
