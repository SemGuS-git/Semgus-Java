package org.semgus.java.event;

import org.semgus.java.object.Identifier;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A SemGuS parser event of the "smt" type.
 */
public sealed interface SmtSpecEvent extends SpecEvent {

    /**
     * A "declare-function" event declaring the signature of a function.
     *
     * @param name          The name of the function.
     * @param returnType    The return type of the function.
     * @param argumentTypes The types of the arguments to the function.
     */
    record DeclareFunctionEvent(
            String name,
            Identifier returnType,
            List<Identifier> argumentTypes
    ) implements SmtSpecEvent {
        // NO-OP
    }

    /**
     * A "define-function" event giving a definition for a previously-declared function.
     *
     * @param name       The name of the function.
     * @param returnType The return type of the function.
     * @param arguments  The arguments to the function.
     * @param body       The body of the function.
     */
    record DefineFunctionEvent(
            String name,
            Identifier returnType,
            List<TypedVar> arguments,
            SmtTerm body
    ) implements SmtSpecEvent {

        /**
         * Constructs a {@link org.semgus.java.object.SmtTerm.Lambda} lambda abstraction from the function definition.
         *
         * @return The new lambda abstraction SMT term.
         */
        public SmtTerm toLambda() {
            return new SmtTerm.Lambda(arguments.stream().map(TypedVar::name).collect(Collectors.toList()), body);
        }

    }

    /**
     * A "declare-datatype" event declaring the signature of a datatype.
     *
     * @param name The name of the datatype.
     */
    record DeclareDatatypeEvent(String name) implements SmtSpecEvent {
        // NO-OP
    }

    /**
     * A "define-datatype" event giving a definition for a previously-declared datatype.
     *
     * @param name         The name of the datatype.
     * @param constructors The constructors of the datatype.
     */
    record DefineDatatypeEvent(String name, List<Constructor> constructors) implements SmtSpecEvent {

        /**
         * A constructor for a datatype.
         *
         * @param name          The name of the constructor.
         * @param argumentTypes The types of the arguments to the constructor.
         */
        public record Constructor(String name, List<Identifier> argumentTypes) {
            // NO-OP
        }

    }

}
