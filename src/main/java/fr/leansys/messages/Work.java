package fr.leansys.messages;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit of work message
 * <p/>
 * Created by @jrenno
 */
@ToString
@AllArgsConstructor
public class Work {
    private static final Logger log = LoggerFactory.getLogger(Work.class);

    @Getter
    private final Document document;

    // Convert BSON document to JSON
    public JsonDocument getJsonDocument() {
        log.debug(document.toJson());
        JsonObject content = JsonObject.from(document);
        return JsonDocument.create(document.getString("_id"), content);
    }
}
