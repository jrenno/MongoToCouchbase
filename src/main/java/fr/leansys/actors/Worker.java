package fr.leansys.actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.base.Throwables;
import fr.leansys.messages.Work;
import fr.leansys.model.ActorContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Write records asynchronously
 * <p/>
 * Created by @jrenno
 */
@AllArgsConstructor
public class Worker extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Getter
    private ActorContext actorContext = null;

    public void onReceive(Object message) {
        if (message instanceof Work) {

            Work work = (Work) message;
            // asynchronous write
            actorContext.getBucket().async().insert(work.getJsonDocument()).
                    doOnError(throwable -> log.error("Write error : {}", Throwables.getRootCause(throwable).getClass().getCanonicalName())).
                    subscribe((response) -> actorContext.getWrite().incrementAndGet());

        } else {
            unhandled(message);
        }
    }

}
