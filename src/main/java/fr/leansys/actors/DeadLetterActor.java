package fr.leansys.actors;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Listen undelivered messages (Dead Letters)
 * <p/>
 * Created by @jrenno
 */
public class DeadLetterActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public void onReceive(Object message) {
        if (message instanceof DeadLetter) {
            log.error("Dead Letter received : {}", message);
        }
    }
}