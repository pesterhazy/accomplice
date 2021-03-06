(def project 'accomplice)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0-alpha12"]
                            [org.clojure/core.async "0.2.391"]
                            [http-kit "2.1.18"]
                            [funcool/cuerdas "1.0.1"]
                            [fogus/ring-edn "0.3.0"]
                            [io.aviso/pretty "0.1.30"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 repl {:init-ns 'accomplice.core}
 aot {:namespace   #{'accomplice.core}}
 pom {:project     project
      :version     version
      :description "FIXME: write description"
      :url         "http://example/FIXME"
      :scm         {:url "https://github.com/yourname/accomplice"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'accomplice.core
      :file        (str "accomplice-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require '[accomplice.core :as app])
  (apply (resolve 'app/-main) args))

(deftask serve
  []
  (future (require '[accomplice.core :as app])
          ((resolve 'app/serve))))

(require '[adzerk.boot-test :refer [test]])
