# clj-toml

clj-toml is [TOML](https://github.com/mojombo/toml) for Clojure. TOML is Tom's Obvious, Minimal Language. 

> TOML is like INI, only better (Tom Preston-Werner)

clj-toml uses [Kern](https://github.com/blancas/kern) for parsing.

This library supports TOML version [4a6ed3944183e2a0307ad6022b7daf53fb9e7eb0](https://github.com/mojombo/toml/tree/4a6ed3944183e2a0307ad6022b7daf53fb9e7eb0).

## Usage

Leiningen:

```clojure
[clj-toml "0.1.0"]
```

```clojure
(use 'clj-toml.core)

(parse-string "
title = \"TOML\"
[Foo]
bar=[1,2,3]")
;; {"title" "TOML" "foo" {"bar" [1 2 3]}}
```

## TODO

The parser does not yet raise an error if

* an array is not homogeneous
* duplicate keys exist

## License

Copyright © 2013 Luca Antiga.

Distributed under the Eclipse Public License, the same as Clojure.
