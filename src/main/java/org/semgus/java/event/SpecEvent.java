package org.semgus.java.event;

/**
 * Parent interface for all SemGuS parser events.
 */
public sealed interface SpecEvent permits MetaSpecEvent, SemgusSpecEvent {
    // NO-OP
}
