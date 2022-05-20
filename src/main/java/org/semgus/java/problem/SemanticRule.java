package org.semgus.java.problem;

import org.semgus.java.object.AnnotatedVar;
import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.List;
import java.util.Map;

/**
 * A semantic rule associated with a production in a SemGuS problem's grammar specification. This takes the form of a
 * constrained horn clause.
 *
 * @param childTermVars The variables associated with child terms of the production.
 * @param head          The conclusion of the semantic rule.
 * @param bodyRelations The premise relations of the semantic rule.
 * @param constraint    The semantic constraint of the semantic rule's premise, given as an SMT formula.
 * @param variables     The collection of all variables referenced in the semantic rule.
 */
public record SemanticRule(List<TypedVar> childTermVars, RelationApp head, List<RelationApp> bodyRelations,
                           SmtTerm constraint, Map<String, AnnotatedVar> variables) {
    // NO-OP
}
