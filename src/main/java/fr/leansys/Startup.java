package fr.leansys;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import fr.leansys.actors.DeadLetterActor;
import fr.leansys.actors.Master;
import fr.leansys.business.FactoryModule;
import fr.leansys.messages.Count;
import fr.leansys.messages.SelectAll;
import fr.leansys.messages.Stats;
import fr.leansys.model.ActorContext;
import org.bson.Document;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import static fr.leansys.business.Constants.*;

/**
 * Import JSON document from MongoDB to Couchbase
 * Version : Java8, Akka and NoSQL Async drivers
 * <p/>
 * Note : expose internal processing state (in JSON format) on HTTP server (ie. http://localhost:8080)
 *
 * @author jacky.renno@leansys.fr
 */
class Startup {
    //~ Static fields/initializers =============================================
    // Initialisation of log
    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    // Application info
    private static final String PROGNAME = "MongoToCouchbase";
    private static final String VERSION = "1.0-ßeta1";

    // Internal global context holder
    private static final ActorContext actorContext = new ActorContext();

    //~ Arribtutes =============================================================

    @Inject
    @Named(HTTP_PORT)
    private int port;

    @Inject
    @Named(MONGO_URI)
    private String mongoUri;

    @Inject
    @Named(MONGO_USER)
    private String mongoUser;

    @Inject
    @Named(MONGO_PASSWORD)
    private String mongoPassword;

    @Inject
    @Named(MONGO_DATABASE)
    private String mongoDatabase;

    @Inject
    @Named(COUCHBASE_URI)
    private String couchbaseUri;

    @Inject
    @Named(COUCHBASE_USER)
    private String couchbaseUser;

    @Inject
    @Named(COUCHBASE_PASSWORD)
    private String couchbasePassword;

    //~ Methods ================================================================
    public static void main(String args[]) {
        System.out.println(String.format("%s version %s", PROGNAME, VERSION));

        if (args.length != 1) {
            System.out.println(String.format("Usage: %s <collection-name>", PROGNAME));
            System.exit(0);
        }

        // Create Guice IOC Factory
        Injector injector = Guice.createInjector(new FactoryModule());

        injector.getInstance(Startup.class).process(args[0]);
    }

    private void process(String collectionName) {

        MongoClient mongoClient = null;
        Cluster cluster = null;

        try {
            // Create MongoDB Connection
            mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase(mongoDatabase);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Set the Couchbase environment configuration
            CouchbaseEnvironment ENV = DefaultCouchbaseEnvironment.builder()
                    .connectTimeout(COUCHBASE_CONNECT_TIMEOUT)
                    .keepAliveInterval(COUCHBASE_KEEPALIVE_INTERVAL)
                    .build();

            // Create Couchbase Connection
            cluster = CouchbaseCluster.create(ENV, Arrays.asList(couchbaseUri));
            // Connect the Cluster Manager
            ClusterManager clusterManager = cluster.clusterManager(couchbaseUser, couchbasePassword);

            if (clusterManager.hasBucket(collectionName)) {
                // Clear data
                log.info("Flushing bucket {}...", collectionName);
                cluster.openBucket(collectionName).bucketManager().flush();
            } else {
                // Create new bucket
                log.info("Creating bucket {}...", collectionName);
                clusterManager.insertBucket(DefaultBucketSettings.builder().
                        name(collectionName).
                        type(BucketType.COUCHBASE).
                        quota(COUCHBASE_CLUSTER_MEMORY_QUOTA).
                        enableFlush(true).
                        build());
            }
            actorContext.setBucket(cluster.openBucket(collectionName));

            actorContext.setCollection(collection);

            // Create the ‘bulkImport’ actor system
            final ActorSystem system = ActorSystem.create("bulkImport");
            // Create the ‘QueryActor’ actor
            final ActorRef master = system.actorOf(Props.create(Master.class, WORKER_INSTANCES, actorContext), "master");

            // Create Dead Letters Messages listener
            final ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class));
            system.eventStream().subscribe(actor, DeadLetter.class);

            // Get records count
            master.tell(new Count(), ActorRef.noSender());

            // Create JSON producer
            final ObjectWriter writer = new ObjectMapper().writer();

            // Create internal HTTP Server process
            Container view = (request, response) -> {
                // Query stats data
                Future<Object> future = Patterns.ask(master, new Stats(), 1000);
                // Wait for async response
                future.onComplete(new OnComplete<Object>() {
                    public void onComplete(Throwable t, Object result) {
                        @SuppressWarnings("unchecked")
                        ImmutableMap<String, Object> stats = (ImmutableMap<String, Object>) result;
                        // Write JSON
                        try {
                            log.debug("Sending stats {}...", stats);
                            // Write JSON
                            writer.writeValue(response.getPrintStream(), stats);
                        } catch (IOException ex) {
                            log.error("Server error {}", Throwables.getRootCause(ex).getMessage());
                        } finally {
                            try {
                                response.close();
                            } catch (IOException ex) {
                                log.error("Server error {}", Throwables.getRootCause(ex).getMessage());
                            }
                        }
                    }
                }, system.dispatcher());
            };

            // Start HTTP server
            ContainerSocketProcessor processor = new ContainerSocketProcessor(view);
            new SocketConnection(processor).connect(new InetSocketAddress(port));

            // Get all records
            master.tell(new SelectAll(), ActorRef.noSender());

            // Start waiting
            system.awaitTermination();
        } catch (Exception ex) {
            log.error("Error occured: {}", Throwables.getStackTraceAsString(ex));
        } finally {
            log.info("Shutting down");
            if (mongoClient != null) mongoClient.close();
            if (cluster != null) cluster.disconnect();
        }
    }
}
