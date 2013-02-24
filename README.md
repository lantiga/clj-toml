# clj-toml

clj-toml is a [TOML](https://github.com/mojombo/toml) parser written in Clojure. TOML is Tom's Obvious, Minimal Language. 

> TOML is like INI, only better (Tom Preston-Werner)

clj-toml uses [Kern](https://github.com/blancas/kern) for parsing.

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

The parser does not yet raise an error if an array is not homogeneous.

## License

Copyright © 2013 Luca Antiga.

Distributed under the Eclipse Public License, the same as Clojure.
