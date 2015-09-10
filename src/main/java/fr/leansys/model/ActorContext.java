package fr.leansys.model;

import com.couchbase.client.java.Bucket;
import com.google.common.collect.ImmutableMap;
import com.mongodb.async.client.MongoCollection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bson.Document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Context holder (used by Actors)
 * <p/>
 * Created by @jrenno
 */
@ToString
public class ActorContext implements Serializable {
    @Getter
    private final AtomicLong write = new AtomicLong(0);
    @Getter
    private final AtomicLong read = new AtomicLong(0);
    @Getter
    private final AtomicLong total = new AtomicLong(0);

    @Getter
    @Setter
    private MongoCollection<Document> collection;
    @Getter
    @Setter
    private Bucket bucket;

    private final Map<String, Object> stats = new HashMap<>();

    public ImmutableMap<String, Object> getStats() {
        stats.put("collection", collection.getNamespace().getCollectionName());
        stats.put("total", total.longValue());
        stats.put("read", read.longValue());
        stats.put("write", write.longValue());
        return ImmutableMap.copyOf(stats);
    }

}
