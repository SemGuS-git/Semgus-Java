package org.semgus.java.problem;

import org.semgus.java.object.AttributeValue;
import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtContext;
import org.semgus.java.object.SmtTerm;

import java.util.List;
import java.util.Map;

/**
 * A representation of a full SemGuS synthesis problem.
 *
 * @param targetName        The name of the target function.
 * @param targetNonTerminal The name of the target function's non-terminal.
 * @param nonTerminals      The grammar of the target DSL.
 * @param constraints       The constraints specifying the target function.
 * @param metadata          Metadata for the synthesis problem.
 * @param smtContext        The SMT context of the synthesis problem.
 */
public record SemgusProblem(String targetName, SemgusNonTerminal targetNonTerminal,
                            Map<String, SemgusNonTerminal> nonTerminals, List<SmtTerm> constraints,
                            Map<String, AttributeValue> metadata,
                            SmtContext smtContext) {

    /**
     * Produces a human-readable string representation (over multiple lines) of this synthesis problem.
     *
     * @return Human-readable stringification of this synthesis problem.
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();

        // print the grammar
        for (SemgusNonTerminal nonTerminal : nonTerminals.values()) {
            // print the non-terminal name
            sb.append(nonTerminal.name()).append(" →\n");
            // print each production
            for (SemgusProduction production : nonTerminal.productions().values()) {
                // print the production's symbol and child term non-terminals
                sb.append("  ").append(production.operator());
                if (!production.childNonTerminals().isEmpty()) {
                    for (SemgusNonTerminal childNonTerminal : production.childNonTerminals()) {
                        sb.append(" ").append(childNonTerminal.name());
                    }
                }
                sb.append("\n");
                // print the production's semantic rules
                for (SemanticRule rule : production.semanticRules()) {
                    sb.append("    ").append(rule.head()).append(" :- ");
                    boolean first = true;
                    for (RelationApp rel : rule.bodyRelations()) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ∧ ");
                        }
                        sb.append(rel);
                    }
                    if (!first) {
                        sb.append(" ∧ ");
                    }
                    sb.append(rule.constraint()).append("\n");
                }
            }
        }
        sb.append("\n");

        // print the constraints
        boolean first = true;
        for (SmtTerm constraint : constraints) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }
            sb.append(constraint);
        }
        return sb.toString();
    }

}
