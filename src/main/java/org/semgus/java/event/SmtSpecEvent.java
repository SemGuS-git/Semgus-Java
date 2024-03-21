package org.semgus.java.event;

import org.semgus.java.object.AttributeValue;
import org.semgus.java.object.Sort;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            Sort returnType,
            List<Sort> argumentTypes
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
     * @param annotations The annotations (usually about input/output variables) on the function body. (optional)
     */
    record DefineFunctionEvent(
            String name,
            Sort returnType,
            List<TypedVar> arguments,
            SmtTerm body,
            Map<String, AttributeValue> annotations
    ) implements SmtSpecEvent {

        public DefineFunctionEvent(String name, Sort returnType, List<TypedVar> arguments,
                            SmtTerm body) {
            this(name, returnType, arguments, body, new HashMap<>());
        }

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
        public record Constructor(String name, List<Sort> argumentTypes) {
            // NO-OP
        }

    }

}
