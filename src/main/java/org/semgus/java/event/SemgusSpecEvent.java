package org.semgus.java.event;

import org.semgus.java.object.AnnotatedVar;
import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SemgusSpecEvent extends SpecEvent {

    record CheckSynthEvent() implements SemgusSpecEvent {
        // NO-OP
    }

    record DeclareTermTypeEvent(String name) implements SemgusSpecEvent {
        // NO-OP
    }

    record DefineTermTypeEvent(String name, List<Constructor> constructors) implements SemgusSpecEvent {

        public static record Constructor(String name, List<String> children) {
            // NO-OP
        }

    }

    record HornClauseEvent(
            Constructor constructor,
            RelationApp head,
            List<RelationApp> bodyRelations,
            SmtTerm constraint,
            Set<AnnotatedVar> variables
    ) implements SemgusSpecEvent {

        public static record Constructor(String name, List<TypedVar> arguments, String returnType) {
            // NO-OP
        }

    }

    record ConstraintEvent(SmtTerm constraint) implements SemgusSpecEvent {
        // NO-OP
    }

    record SynthFunEvent(String name, Map<String, NonTerminal> grammar, String termType) implements SemgusSpecEvent {

        public static record NonTerminal(String termType, Map<String, Production> productions) {
            // NO-OP
        }

        public static record Production(String operator, List<String> occurrences) {
            // NO-OP
        }

    }

}
