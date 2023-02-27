# mock-server

A library allowing simple assertions about whether and how an HTTP endpoint was
called.

When testing a service, you may want to make assertions about how external
dependencies are called, or that your system behaves as you expect when an
another system responds poorly.

For example, you may be testing an email confirmation flow, which makes HTTP
requests to send emails to users. You'd like to call your API, and assert that
it calls the external API. But you'd also like to see what happens when that
external API:

- takes 5 minutes to respond?

- responds with a 500?

- fails, then succeeds?

etc.

## Usage

deps.edn:

```
[net.clojars.jds02006/mock_server {:mvn/version "0.1.0-SNAPSHOT"}]
```

leiningen:

```
[net.clojars.jds02006/mock_server "0.1.0-SNAPSHOT"]
```

Usage is pretty simple. First require the namespace:

``` clojure
(require '[johnswanson.mock-server :as mock :refer [with-mock-server with-requests with-handler]])

```

You can use `with-mock-server` to launch the mock server. (You can also launch
it manually with `start-server!`, in which case you'll need to manually shut it
down with `stop-server!`.)

```clojure

;; launch the mock HTTP server. By default, it just returns a 200 status and no body
(with-mock-server server
  (is (= 200 (:status (http/get (mock/url server))))))
```

If you want to inspect the requests your application has made to the server, you
can use `with-requests` to collect requests into a vector:

``` clojure
(with-mock-server server
  (with-requests requests
    (http/get (mock/url server))
    (is (= :get (:request-method (first @requests))))))
```

Finally, if you want to change the behavior of the HTTP server (e.g. return 500
errors, or induce timeouts with a long sleep), you can do that as well! Just use
`with-handler`:

``` clojure
(with-mock-server server
  (with-handler (fn [_req]
                  (Thread/sleep 10000)
                  {:status 404})
    ;; this will take 10 seconds!
    (is (= 404 (:status (http/get (mock/url server)))))))
```

You've probably figured it out by now, but `mock/url` is a function returning
the URL for the mock server - probably something like "http://localhost:61022".

## Building

Run the project's CI pipeline and build a JAR:

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.jds02006/mock_server on clojars.org by default.

## License

Copyright © 2023 John Swanson

Distributed under the Eclipse Public License version 1.0.
