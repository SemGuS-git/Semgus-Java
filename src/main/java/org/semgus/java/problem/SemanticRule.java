package org.semgus.java.problem;

import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.List;

public record SemanticRule(List<TypedVar> childTermVars, RelationApp head, List<RelationApp> bodyRelations,
                           SmtTerm constraint) {
    // NO-OP
}
