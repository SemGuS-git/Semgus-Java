package org.semgus.java.problem;

import java.util.List;

/**
 * A production of a non-terminal in a SemGuS problem's grammar specification.
 *
 * @param operator          The name/symbol for the production.
 * @param childNonTerminals The names of non-terminals for child terms.
 * @param semanticRules     The semantic rules for the production.
 */
public record SemgusProduction(String operator, List<SemgusNonTerminal> childNonTerminals,
                               List<SemanticRule> semanticRules) {
    // NO-OP
}
