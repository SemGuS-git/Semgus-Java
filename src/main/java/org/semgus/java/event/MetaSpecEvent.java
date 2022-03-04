package org.semgus.java.event;

import org.semgus.java.object.AttributeValue;

public interface MetaSpecEvent extends SpecEvent {

    record SetInfoEvent(String keyword, AttributeValue value) implements MetaSpecEvent {
        // NO-OP
    }

    record StreamEndEvent() implements MetaSpecEvent {
        // NO-OP
    }

}
