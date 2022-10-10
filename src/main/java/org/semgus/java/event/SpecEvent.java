package org.semgus.java.event;

/**
 * Parent interface for all SemGuS parser events.
 */
public sealed interface SpecEvent permits MetaSpecEvent, SmtSpecEvent, SemgusSpecEvent {
    // NO-OP
}
