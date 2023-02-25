# johnswanson/mock_server

A library allowing simple assertions about whether and how an HTTP endpoint was called.

## Usage

When testing a service, you may want to make assertions about how external
dependencies are called. For example, you may be testing an email confirmation
flow, which makes HTTP requests to send emails to users.

This is a wrapper around the Java library WireMock, with a more declarative
interface.

The basic flow:

``` clojure
;; launch the mock HTTP server
(with-mock-server
  ;; set up the expectations that must be met. In this case, we must receive an
  ;; HTTP request to the url "/foo". Any method and content is allowed.
  ;;
  ;; If no matching HTTP request is made, OR if non-matching HTTP requests are
  ;; made, throw an exception
  (with-expect {:request {:url "/foo"}} 
    ;; do something that makes an HTTP request to `(mock/url "/foo")`
    (http/get (mock/url "/foo"))))
```

You can specify the method:

``` clojure
(with-expect {:request {:url "/foo"
                        :method "GET"}}
  ;; ...)
```

You can specify that repeated requests must be made:
``` clojure
(with-expect {:request {:url "/foo"
                        :method "GET"}
              :response {:status 200
                         :count 2}}
  ;; ...)
```

You can also specify `responses` instead of `response`, and pass a collection of responses.
``` clojure
(with-expect {:request {:url "/foo"
                        :method "GET"}
              :responses [{:status 200} {:status 500}]}
  ;; make 2 requests, the second will fail with a 500 error
  ;; ...)
```

You can go wild and combine the two options... though maybe you shouldn't?

``` clojure
(with-expect {:request {:url "/foo"
                        :method "GET"}
              :responses [{:status 200 :count 3} {:status 500}]}
  ;; make 4 requests total, the last one will return a 500 error
  ;; ...)
```

### More complex matching

You can be more specific about what kinds of requests should be expected. You can pass:

#### Headers

You can pass headers like so:

``` clojure
:headers {"header-name" {:matches "header-value"}}
```

Any request that matches will get the response you specified. Otherwise the
expectation will fail.

#### Body patterns

You can match against the raw body of the HTTP request:

``` clojure
:body-patterns [{:equal-to "raw body"}]
```

Or the JSON decoded body (use strings for keys, rather than keywords - otherwise
`mock-server` may change the casing!):

``` clojure
:body-patterns [{:equal-to-json {"some_key" "value"}}]
```

You can also expect based on the raw bytes or XML-decoded body!

Instead of `equal-to` you can use `contains`:

``` clojure
:body-patterns [{:contains "some text"}]
```

For more details on request matching, see the documentation for WireMock.

Run the project's CI pipeline and build a JAR (this will fail until you edit the
tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.johnswanson/mock_server on clojars.org by default.

## License

Copyright © 2023 Jds

_EPLv1.0 is just the default for projects generated by `deps-new`: you are not_
_required to open source this project, nor are you required to use EPLv1.0!_
_Feel free to remove or change the `LICENSE` file and remove or update this_
_section of the `README.md` file!_

Distributed under the Eclipse Public License version 1.0.