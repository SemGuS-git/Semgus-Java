package org.semgus.java.problem;

import java.util.List;

public record SemgusProduction(String operator, List<SemgusNonTerminal> childNonTerminals,
                               List<SemanticRule> semanticRules) {
    // NO-OP
}
