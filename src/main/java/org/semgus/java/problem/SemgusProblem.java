package org.semgus.java.problem;

import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;

import java.util.List;
import java.util.Map;

public record SemgusProblem(Map<String, SemgusNonTerminal> nonTerminals, List<SmtTerm> constraints) {

    public String dump() {
        StringBuilder sb = new StringBuilder();
        for (SemgusNonTerminal nonTerminal : nonTerminals.values()) {
            sb.append(nonTerminal.name()).append(" →\n");
            for (SemgusProduction production : nonTerminal.productions().values()) {
                sb.append("  ").append(production.operator());
                if (!production.childNonTerminals().isEmpty()) {
                    for (SemgusNonTerminal childNonTerminal : production.childNonTerminals()) {
                        sb.append(" ").append(childNonTerminal.name());
                    }
                }
                sb.append("\n");
                for (SemanticRule rule : production.semanticRules()) {
                    sb.append("    ").append(rule.head()).append(" ⇐ ");
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
