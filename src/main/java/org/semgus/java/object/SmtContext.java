package org.semgus.java.object;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A context for SMT containing datatype and function definitions.
 *
 * @param datatypes The table of datatype definitions.
 * @param functions The table of function definitions.
 */
public record SmtContext(Map<String, Datatype> datatypes, Map<String, SmtContext.Function> functions) {

    @Override
    public String toString() {
        if (datatypes.isEmpty()) {
            if (functions.isEmpty()) {
                return "{}";
            } else {
                return "{;" + String.join(", ", functions.keySet()) + "}";
            }
        } else if (functions.isEmpty()) {
            return "{" + String.join(", ", datatypes.keySet()) + ";}";
        } else {
            return "{" + String.join(", ", datatypes.keySet()) + "; "
                    + String.join(", ", functions.keySet()) + "}";
        }
    }

    /**
     * A definition of an (inductive) datatype.
     *
     * @param name         The name of the datatype.
     * @param constructors The set of constructors for the datatype.
     */
    public record Datatype(String name, Map<String, Constructor> constructors) {

        @Override
        public String toString() {
            return String.format(
                    "%s { %s }",
                    name,
                    constructors.values().stream()
                            .map(Constructor::toString)
                            .collect(Collectors.joining(" | ")));
        }

        /**
         * A constructor for a datatype.
         *
         * @param name          The name of the constructor.
         * @param argumentTypes The types of the constructor's arguments.
         */
        public record Constructor(String name, List<Identifier> argumentTypes) {

            @Override
            public String toString() {
                if (argumentTypes.isEmpty()) {
                    return name;
                }

                StringBuilder sb = new StringBuilder("(").append(name);
                for (Identifier argumentType : argumentTypes) {
                    sb.append(" ").append(argumentType);
                }
                return sb.append(")").toString();
            }

        }

    }

    /**
     * A definition of a function.
     *
     * @param name      The name of the function.
     * @param arguments The arguments to the function.
     * @param body      The body of the function.
     */
    public record Function(String name, List<TypedVar> arguments, SmtTerm body) {

        @Override
        public String toString() {
            return String.format(
                    "(lambda (%s) %s)",
                    arguments.stream().map(TypedVar::toString).collect(Collectors.joining(" ")),
                    body);
        }

    }

}
