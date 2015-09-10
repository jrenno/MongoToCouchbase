package fr.leansys.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Result message
 * <p/>
 * Created by @jrenno
 */
@ToString
@AllArgsConstructor
public class Result {
    @Getter
    protected String Message;
}
