package org.semgus.java.event;

import org.semgus.java.object.AttributeValue;

/**
 * A SemGuS parser event of the "meta" type.
 */
public sealed interface MetaSpecEvent extends SpecEvent {

    /**
     * A "set-info" event specifying some problem metadata.
     *
     * @param keyword The metadata key.
     * @param value   The metadata value.
     */
    record SetInfoEvent(String keyword, AttributeValue value) implements MetaSpecEvent {
        // NO-OP
    }

    /**
     * A "end-of-stream" event indicating that there are no further events.
     */
    record StreamEndEvent() implements MetaSpecEvent {
        // NO-OP
    }

}
