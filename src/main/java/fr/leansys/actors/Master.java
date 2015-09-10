package fr.leansys.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.RoundRobinPool;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import fr.leansys.messages.*;
import fr.leansys.model.ActorContext;
import org.bson.Document;

/**
 * Main actor (query data then dispatch processing)
 * <p/>
 * Created by @jrenno
 */
public class Master extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final ActorContext actorContext;
    private final ActorRef workerRouter;

    public Master(final int nrOfWorkers, ActorContext actorContext) {
        this.actorContext = actorContext;
        this.workerRouter = getContext().actorOf(new RoundRobinPool(nrOfWorkers).props(Props.create(Worker.class, actorContext)), "workerRouter");
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof Count) {

            log.info("Received Count message");
            actorContext.getCollection().count((count, t) -> {
                log.info("Total records {}", count);
                actorContext.getTotal().set(count);
            });

        } else if (message instanceof SelectAll) {

            log.info("Received SelectAll message");

            // Query All
            Block<Document> processDocumentBlock = document -> {
                actorContext.getRead().incrementAndGet();
                // Dispath write process
                workerRouter.tell(new Work(document), getSelf());
            };

            SingleResultCallback<Void> callbackWhenFinished = (result, t) -> {
                log.debug("End of read process");
                String resultMessage = String.format("Operation Finished, %s records read in %d ms", actorContext.getRead(),
                        (System.currentTimeMillis() - ((SelectAll) message).getStart()));
                getSelf().tell(new Result(resultMessage), getSelf());
            };

            // Start query
            actorContext.getCollection().find().forEach(processDocumentBlock, callbackWhenFinished);

        } else if (message instanceof Stats) {

            log.info("Received Stats message");
            getSender().tell(actorContext.getStats(), getSelf());

        } else if (message instanceof Result) {

            Result result = (Result) message;
            log.info("Terminated : {}", result);

        } else {
            unhandled(message);
        }
    }
}