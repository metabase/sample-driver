# Metabase Sample Driver

This is an example of a 3rd party driver, similar to the [sudoku-driver](https://github.com/metabase/sudoku-driver), with the following differences:

* it connects to a real (Postgres) database and executes queries there
* it has the test extensions defines, so that the full Metabase core test suite can run against it

**Note**: although this is a functionally identical clone of the Postgres driver we ship with the product, it should not be used in production.  It will not be maintained regularly or supported for production usage.

## Usage

Build the same way as the suduko-driver

`clojure -X:dev:build`

## Testing

### Set Up
Running the Metabase test suite using your driver requires a bit of one-time setup.  You will need to create (or edit, if you have an existing one), your personal `deps.edn` file at `~/.clojure/deps.edn`, to add a new alias for your custom driver.  Note that if you have this `:aliases` map already, you will just need to create a new entry here.

```
{:aliases {
  :user/sample-driver {:extra-paths ["/path/to/sample-driver/test"]
                       :extra-deps {metabase/sample-driver {:local/root "/path/to/sample-driver"}}}
}}
```

### Interactive Testing

To run Metabase tests, go to your [Metabase core](https://github.com/metabase/metabase) repository, then run the following:

```
MB_SAMPLE_DRIVER_TEST_PLUGIN_MANIFEST_PATH=/path/to/sample-driver/resources/metabase-plugin.yaml clojure -M:dev:ee:ee-dev:drivers:drivers-dev:user:trace:deps-alpha:user/sample-driver:nrepl >/tmp/metabase/repl.out 2>&1
```

If you want to use remote Java debugging to diagnose test issues, you can add the following options right after `clojure` above.  These will enble the remote debugger (on port `15319`, although you can change it), and also disable local variable clearing, so you can more easily walk up the call stack when suspended at a breakpoint.

`-J-Dclojure.compiler.disable-locals-clearing=true -J-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=15319`

Once the nREPL starts up, you can run the following:

```
;; load the interactive testing namespace
(require '[dev.driver.testing.interactive :as interactive])

;; test your driver interactively
(interactive/interactively-test-driver :sample-driver)
```

At this point, you should see a series of prompts on the screen to enter all the driver's connection properties.  Enter a value for each one, hitting enter after each value.  At the end, you will see some output like the following:

```
Storing to store db-details to `user/details`.
Run `(user/test-connect!) to test a connection using these details`.
Use `(user/run-tests!)` to execute tests against the connection details you entered.  Pass an optional argument to only run certain tests.  Ex:
  `(run-tests! 'metabase.query-processor-test.string-extracts-test)`
```

Now, you can simply run `details` to see the map of connection details you just entered (which corresponds to the values you would type in the UI Admin section for this database).

To test that a connection can be successfully established, just run `(test-connect!)` and you should see `Successfully connected` (or an error message if it fails).  You can also run a small test suite using:

```
(run-tests! 'metabase.query-processor-test.string-extracts-test)
```

To run the full test suite, simply leave out the argument, but note this is involved and will take a while.  In either case, a report will be printed at the end.

