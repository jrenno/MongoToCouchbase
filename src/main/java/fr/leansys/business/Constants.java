package fr.leansys.business;

/**
 * Constants (hold key entries for properties.xml file)
 *
 * @author jacky.renno@leansys.fr
 */
public interface Constants {
    // General
    String HTTP_PORT = "http.port";

    // Mongo
    String MONGO_URI = "mongodb.uri";
    String MONGO_DATABASE = "mongodb.database";
    String MONGO_USER = "mongodb.user";
    String MONGO_PASSWORD = "mongodb.password";

    // Couchbase
    String COUCHBASE_URI = "couchbase.uri";
    String COUCHBASE_USER = "couchbase.user";
    String COUCHBASE_PASSWORD = "couchbase.password";

    // Tunning
    int COUCHBASE_CONNECT_TIMEOUT = 30000;          // seconds
    int COUCHBASE_CLUSTER_MEMORY_QUOTA = 2 * 1024;  // MB
    int COUCHBASE_KEEPALIVE_INTERVAL = 3600 * 1000; // milliseconds
    int WORKER_INSTANCES = 5;                       // numbers of parallel workers used for writing data
}

