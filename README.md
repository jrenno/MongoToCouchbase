# MongoToCouchbase
MongoToCouchbase import tool (based on Akka Actors with full async read-write access)

Bulk import for couchbase from MongoDB Collection (for testing purpose only)

The tool create (or flush if already exists) target bucket, and insert all records

A simple HTTP server is up to provide JSON status while processing data (default port is 8080)


Set configuration
-----------------

- Set database properties in resources/properties.xml
- Tune settings in Constants.java


Compile
-------

mvn clean compile assembly:single


Usage
-----

java -jar target/MongoToCouchbase-1.0-SNAPSHOT-jar-with-dependencies.jar <collection-name>

