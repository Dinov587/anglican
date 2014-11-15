(ns embang.simp)

;;; Clojure simplification
;;
;; The clojure subset to which anglican is translated
;; is simplified so that there are only applications,
;; functions with a single expression in the body,
;; and cond.

(declare simplify)

(defn simplify-elist
  "simplifies expression list"
  [elist]
  (let [[e & elist] elist]
    (if elist
      `((~'fn [~'_]
          ~(simplify-elist elist))
        ~(simplify e))
      (simplify e))))

(defn simplify-fn
  "simplifies function by
  eliminating expression list in the body"
  [args]
  (if (symbol? (first args)) ; named fn
    (let [[name parms & body] args]
      `(~'fn ~name ~parms ~(simplify-elist body)))
    (let [[parms & body] args]
      `(~'fn ~parms ~(simplify-elist body)))))

(defn simplify-let
  "replaces let with application of fn"
  [[bindings & body]]
  (let [bindings (partition 2 bindings)]
    `(~(simplify `(~'fn ~(vec (map first bindings)) ~@body))
      ~@(map (comp simplify second) bindings))))

(defn simplify-if
  "replaces if with cond"
  [[cnd thn els]]
  (simplify 
   `(~'cond ~cnd ~thn
            ~@(when els `(:else ~els)))))

(defn simplify-cond
  "simplifies expressions in cond recursively"
  [args]
  `(~'cond ~@(map simplify args)))

(defn simplify-do
  "eliminates do"
  [args]
  (simplify-elist args))

(defn simplify
  "simplifies clojure representation of an anglican program:
  `if' is replaced with `cond',
  `let' is replaced with application of `fn',
  sequences are represented by nested `fn'"
  [code]
  (if (seq? code)
    (let [[kwd & args] code]
      (case kwd
        quote code
        fn    (simplify-fn args)
        let   (simplify-let args)
        if    (simplify-if args)
        cond  (simplify-cond args)
        do    (simplify-do args)
        (map simplify code)))
    code))

