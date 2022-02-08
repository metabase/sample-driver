(ns metabase.test.data.sample-driver
  "Test extensions for the sample driver.  Includes logic for creating/destroying test datasets, building
  the connection specs from environment variables, etc."
  (:require [metabase.test.data.interface :as tx]
            [metabase.test.data.sql :as sql.tx]
            [metabase.test.data.sql-jdbc :as sql-jdbc.tx]
            [metabase.test.data.sql-jdbc.load-data :as load-data]
            [metabase.test.data.sql.ddl :as ddl]))

(sql-jdbc.tx/add-test-extensions! :sample-driver)

(defmethod tx/has-questionable-timezone-support? :sample-driver [_] true) ; TODO - What?

(defmethod tx/sorts-nil-first? :sample-driver [_ _] false)

(defmethod sql.tx/pk-sql-type :sample-driver [_] "SERIAL")

(defmethod tx/aggregate-column-info :sample-driver
  ([driver ag-type]
   ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type))

  ([driver ag-type field]
   (merge
    ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type field)
    (when (= ag-type :sum)
      {:base_type :type/BigInteger}))))

(doseq [[base-type db-type] {:type/BigInteger     "BIGINT"
                             :type/Boolean        "BOOL"
                             :type/Date           "DATE"
                             :type/DateTime       "TIMESTAMP"
                             :type/DateTimeWithTZ "TIMESTAMP WITH TIME ZONE"
                             :type/Decimal        "DECIMAL"
                             :type/Float          "FLOAT"
                             :type/Integer        "INTEGER"
                             :type/IPAddress      "INET"
                             :type/Text           "TEXT"
                             :type/Time           "TIME"
                             :type/TimeWithTZ     "TIME WITH TIME ZONE"
                             :type/UUID           "UUID"}]
  (defmethod sql.tx/field-base-type->sql-type [:sample-driver base-type] [_ _] db-type))

(defmethod tx/dbdef->connection-details :sample-driver
  [_ context {:keys [database-name]}]
  (merge
   {:host     (tx/db-test-env-var-or-throw :sample-driver :host "localhost")
    :port     (tx/db-test-env-var-or-throw :sample-driver :port 5432)
    :timezone :America/Los_Angeles}
   (when-let [user (tx/db-test-env-var :sample-driver :user)]
     {:user user})
   (when-let [password (tx/db-test-env-var :sample-driver :password)]
     {:password password})
   (when (= context :db)
     {:db database-name})))

(defn- kill-connections-to-db-sql
  "Return a SQL `SELECT` statement that will kill all connections to a database with DATABASE-NAME."
  ^String [database-name]
  (format (str "DO $$ BEGIN\n"
               "  PERFORM pg_terminate_backend(pg_stat_activity.pid)\n"
               "  FROM pg_stat_activity\n"
               "  WHERE pid <> pg_backend_pid()\n"
               "    AND pg_stat_activity.datname = '%s';\n"
               "END $$;\n")
          (name database-name)))

(defmethod ddl/drop-db-ddl-statements :sample-driver
  [driver {:keys [database-name], :as dbdef} & options]
  (when-not (string? database-name)
    (throw (ex-info (format "Expected String database name; got ^%s %s"
                            (some-> database-name class .getCanonicalName) (pr-str database-name))
                    {:driver driver, :dbdef dbdef})))
  ;; add an additional statement to the front to kill open connections to the DB before dropping
  (cons
   (kill-connections-to-db-sql database-name)
   (apply (get-method ddl/drop-db-ddl-statements :sql-jdbc/test-extensions) :sample-driver dbdef options)))

(defmethod load-data/load-data! :sample-driver [& args]
  (apply load-data/load-data-all-at-once! args))

(defmethod sql.tx/standalone-column-comment-sql :sample-driver [& args]
  (apply sql.tx/standard-standalone-column-comment-sql args))

(defmethod sql.tx/standalone-table-comment-sql :sample-driver [& args]
  (apply sql.tx/standard-standalone-table-comment-sql args))

