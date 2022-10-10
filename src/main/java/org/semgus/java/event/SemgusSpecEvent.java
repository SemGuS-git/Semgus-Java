package org.semgus.java.event;

import org.semgus.java.object.AnnotatedVar;
import org.semgus.java.object.RelationApp;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.List;
import java.util.Map;

/**
 * A SemGuS parser event of the "semgus" type.
 */
public sealed interface SemgusSpecEvent extends SpecEvent {

    /**
     * A "check-synth" event indicating that the synthesis problem specification is done.
     */
    record CheckSynthEvent() implements SemgusSpecEvent {
        // NO-OP
    }

    /**
     * A "declare-term-type" event reserving a name for a term type.
     *
     * @param name The name of the term type.
     */
    record DeclareTermTypeEvent(String name) implements SemgusSpecEvent {
        // NO-OP
    }

    /**
     * A "define-term-type" event specifying syntactic constructors for a previously-declared term type.
     *
     * @param name         The name of the term type.
     * @param constructors The constructors to define for the term type.
     */
    record DefineTermTypeEvent(String name, List<Constructor> constructors) implements SemgusSpecEvent {

        /**
         * A constructor being defined for a term type.
         *
         * @param name     The name of the constructor.
         * @param children The names of child term types for this constructor.
         */
        public record Constructor(String name, List<String> children) {
            // NO-OP
        }

    }

    /**
     * A "chc" event defining a semantic rule in the form of a constrained horn clause.
     *
     * @param constructor   The syntactic constructor this semantic rule applies to.
     * @param head          The conclusion of the semantic rule.
     * @param bodyRelations The premise relations of the semantic rule.
     * @param constraint    The semantic constraint of the premise.
     * @param variables     A collection of all the variables referenced in the semantic rule.
     */
    record HornClauseEvent(
            Constructor constructor,
            RelationApp head,
            List<RelationApp> bodyRelations,
            SmtTerm constraint,
            Map<String, AnnotatedVar> variables
    ) implements SemgusSpecEvent {

        /**
         * A specification of a syntactic constructor.
         *
         * @param name       The name of the constructor.
         * @param arguments  The arguments to the constructor.
         * @param returnType The name of the term type constructed by the constructor.
         */
        public record Constructor(String name, List<TypedVar> arguments, String returnType) {
            // NO-OP
        }

    }

    /**
     * A "constraint" event defining a constraint on the synthesis target function.
     *
     * @param constraint The constraint as an SMT formula.
     */
    record ConstraintEvent(SmtTerm constraint) implements SemgusSpecEvent {
        // NO-OP
    }

    /**
     * A "synth-fun" event defining a synthesis target function and a target DSL in terms of previously-declared term
     * types and semantic rules.
     *
     * @param name     The name of the target function.
     * @param grammar  The regular tree grammar for the target DSL.
     * @param termType The term type of the target function.
     */
    record SynthFunEvent(String name, Map<String, NonTerminal> grammar, String termType) implements SemgusSpecEvent {

        /**
         * A specification of a non-terminal in a regular tree grammar.
         *
         * @param termType    The term type corresponding to this non-terminal.
         * @param productions A set of productions, indexed by production name.
         */
        public record NonTerminal(String termType, Map<String, Production> productions) {
            // NO-OP
        }

        /**
         * A specification of a production in a regular tree grammar.
         *
         * @param operator    The name/symbol for this production.
         * @param occurrences The names of non-terminals for child terms.
         */
        public record Production(String operator, List<String> occurrences) {
            // NO-OP
        }

    }

}
