package org.semgus.java.problem;

import java.util.Map;

/**
 * A non-terminal in a SemGuS problem's grammar specification.
 *
 * @param name        The name of the non-terminal.
 * @param productions The set of productions, indexed by their names.
 */
public record SemgusNonTerminal(String name, Map<String, SemgusProduction> productions) {
    // NO-OP
}
