package org.semgus.java.problem;

import java.util.Map;

public record SemgusNonTerminal(String name, Map<String, SemgusProduction> productions) {
    // NO-OP
}
