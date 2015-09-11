package fr.leansys.messages;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

/**
 * Select all message
 * <p/>
 * Created by @jrenno
 */
@ToString
public class SelectAll {
    @Getter
    private long start = 0;

    public SelectAll() {
        this.start = System.currentTimeMillis();
    }
}
